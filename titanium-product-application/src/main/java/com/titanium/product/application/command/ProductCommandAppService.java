package com.titanium.product.application.command;


import java.util.List;
import java.util.Set;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.common.multitenant.TenantContext;
import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.CommandValidationException;
import com.titanium.metadata.exception.DomainException;
import com.titanium.product.application.orchestration.product.ProductPublishValidator;
import com.titanium.product.application.query.ProductQueryAppService;
import com.titanium.product.command.AuditProductCommand;
import com.titanium.product.command.ConfigureLifeProductCommand;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.command.InvalidateProductCommand;
import com.titanium.product.command.RejectProductAuditCommand;
import com.titanium.product.command.ReviseProductCommand;
import com.titanium.product.command.SubmitProductForAuditCommand;
import com.titanium.product.command.UpdateAttachProductCommand;
import com.titanium.product.command.UpdateCoverageSumInsuredConfigCommand;
import com.titanium.product.command.UpdateProductClauseRelCommand;
import com.titanium.product.command.UpdateSalesChannelCommand;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.port.clause.ClauseBindingCheck;
import com.titanium.product.port.clause.ClauseExistencePort;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.query.service.ProductTemplateQueryService;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.valueobject.config.CoverageSumInsuredConfig;

import lombok.RequiredArgsConstructor;

/**
 * 产品命令应用服务（写用例入口门面）
 *
 * <p>入参统一为 domain 领域命令：Request/DTO → Command 的翻译由 web 层
 * {@code ProductWebMapper} 承担，本层只做事务边界与 {@code commandGateway.sendAndWait} 派发，
 * 不依赖 api 契约细节、不写业务规则（规则内聚聚合根）。（项目规约 3.4.8/六边形隔离）</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProductCommandAppService {

    private final CommandGateway               commandGateway;
    private final ProductTemplateQueryService  productTemplateQueryService;
    private final ProductQueryAppService       productQueryAppService;
    private final ClauseExistencePort          clauseExistencePort;
    private final ProductPublishValidator      productPublishValidator;

    /** 平台级公共模板租户（default 租户模板对全租户可见，如宠物医疗险种子） */
    private static final String PUBLIC_TEMPLATE_TENANT = "default";

    /**
     * 创建产品：先校验引用的产品模板存在且处于 ACTIVE、绑定的条款在条款域真实存在且可绑定，再派发创建命令。
     * <p>
     * 模板存在性/状态属跨聚合读校验、条款可绑定性属跨服务校验，二者均由 application 编排完成
     * （聚合根不查其它聚合读模型、领域服务不得依赖 Port）。
     * </p>
     *
     * @param command 创建产品命令
     * @return 产品ID
     */
    public String createProduct(CreateProductCommand command) {
        validateTemplateActive(command.templateId(), command.tenantId());
        validateClauseBindable(command.clauseBinding().clauseIds(), command.tenantId(),
                CreateProductCommand.class.getSimpleName(), "clauseIds");
        return commandGateway.sendAndWait(command);
    }

    /**
     * 校验绑定的条款在条款域真实存在、且状态允许绑定（须 ACTIVE）。
     *
     * <p>🔴 这是条款绑定**写入侧唯一的真伪与状态判据**：该字段指向条款域聚合根，产品域无从自证。
     * 此前只校验「{@code clauseIds} 非空 + 主条款在列表内」，**任何字符串都能落库**——R7-01 的条款
     * <b>编码</b>被当成<b>主键</b>写进绑定，导致产品详情整页报「资源不存在」、出单取不到条款与责任快照。</p>
     *
     * <p>判据不是形态（UUID 正则会把雪花 ID 形态的合法条款全挡掉），而是向条款域实地求证；
     * 条款服务不可用时由 {@code ClauseExistencePort} 实现原样上抛，<b>不会</b>被表达成「条款不存在」。</p>
     *
     * <p>🔴 <b>两种拒绝必须说两种话</b>：不存在 ⇒ 查输入（可能把编码当主键传了）；存在但未生效
     * ⇒ 查时序（该先让条款生效）。合并成一句话会让运营照「不存在」的提示去新建同编码条款，
     * 撞上编码唯一约束。</p>
     *
     * <p>创建与更新两条路径<b>共用本方法</b>（不变量 3.4.11「同一个判断不得在两处各写一份」）——
     * R7-02 的负向对照曾因该判据分散在三处而漏注一处，得出「测试没牙」的错误结论。</p>
     *
     * @param clauseIds   绑定的条款主键列表
     * @param tenantId    租户ID
     * @param commandName 发起命令的类名（用于错误定位）
     * @param fieldName   命令中承载条款引用的字段名（用于错误定位）
     */
    private void validateClauseBindable(List<String> clauseIds, String tenantId,
                                        String commandName, String fieldName) {
        ClauseBindingCheck check = clauseExistencePort.checkBindable(clauseIds, tenantId);
        if (!check.missingClauseIds().isEmpty()) {
            throw new CommandValidationException(ProductErrorCode.CLAUSE_BINDING_FAILED,
                    commandName, fieldName,
                    "以下条款不存在，无法绑定: " + String.join(", ", check.missingClauseIds())
                            + "（请确认传入的是条款主键；条款编码不能直接绑定）");
        }
        if (!check.nonActiveClauseIds().isEmpty()) {
            throw new CommandValidationException(ProductErrorCode.CLAUSE_BINDING_FAILED,
                    commandName, fieldName,
                    "以下条款当前未生效，无法绑定: " + String.join(", ", check.nonActiveClauseIds())
                            + "（绑定要求条款处于 ACTIVE；请先在条款域完成审核激活）");
        }
    }

    /**
     * 提取条款关联列表中的条款主键。
     *
     * <p>三条写入路径（创建 / 整体替换 / 修订）共用本方法——此前这段映射在替换路径内联写了一份，
     * 修订路径再抄一份就会重现「同一个判断在两处各写一份 ⇒ 必漂移」（§3.4.11）。</p>
     *
     * <p>入参为 {@code null} 时返回空列表：本条只负责「取出引用」，不表达「继承」语义——继承由聚合裁决，
     * 且跨域校验对空列表本就不做任何判定（端口对空入参短路返回可绑定）。</p>
     *
     * @param clauseRels 条款关联列表，可为 null
     * @return 条款主键列表，永不为 null
     */
    private static List<String> clauseIdsOf(List<ProductClauseRel> clauseRels) {
        return clauseRels == null
                ? List.of()
                : clauseRels.stream().map(ProductClauseRel::clauseId).toList();
    }

    /**
     * 校验产品模板存在且为 ACTIVE 状态。
     * <p>
     * 平台级公共模板兜底：本租户未建模板时允许引用 {@code default} 租户的公共模板
     * （如宠物医疗险种子 {@code seed_template_pet_medical}），本租户模板优先。
     * </p>
     *
     * @param templateId 产品模板ID
     * @param tenantId 租户ID
     */
    private void validateTemplateActive(String templateId, String tenantId) {
        ProductTemplateQueryResult template = productTemplateQueryService.getTemplateById(templateId, tenantId);
        if (template == null && !PUBLIC_TEMPLATE_TENANT.equals(tenantId)) {
            template = productTemplateQueryService.getTemplateById(templateId, PUBLIC_TEMPLATE_TENANT);
        }
        if (template == null) {
            throw new CommandValidationException(CreateProductCommand.class.getSimpleName(), "templateId",
                    "引用的产品模板不存在: " + templateId);
        }
        if (!CommonStatus.ACTIVE.equals(template.getStatus())) {
            throw new CommandValidationException(CreateProductCommand.class.getSimpleName(), "templateId",
                    "产品模板未生效(需 ACTIVE): " + templateId);
        }
    }

    /**
     * 提交产品审核。
     *
     * @param productId 产品ID
     */
    public void submitForAudit(String productId) {
        commandGateway.sendAndWait(new SubmitProductForAuditCommand(productId, "system", "system"));
    }

    /**
     * 审核通过产品：**先做上架完整性校验**，再派发审核命令。
     *
     * <p>🔴 校验落在本编排点而非聚合命令处理器：校验需跨聚合仓储与跨域端口，聚合不得依赖 Port
     * （项目规约 §3.4.4 + ArchUnit）。聚合内原有的 {@code AUDITING} 状态前置检查<b>保留不动</b>，
     * 二者职责不同——状态检查管「能不能审」，本校验管「审过了卖不卖得出去」。</p>
     *
     * <p>⚠️ <b>已知并发窗口</b>：校验在 {@code sendAndWait} <b>之前</b>执行，与聚合内状态检查之间存在
     * 一瞬窗口——并发上架/并发改动定价组件时，可能出现「校验通过后、命令到达前」状态已变。
     * 本批次<b>接受该窗口</b>：产品配置是低频人工操作，且聚合内的状态前置检查仍会兜住非法状态迁移。
     * 若将来产品上架改为高频自动化流程，需改为「校验 + 命令」同事务或落聚合内不变量。</p>
     *
     * @param command 审核产品命令
     */
    public void auditProduct(AuditProductCommand command) {
        productPublishValidator.validate(TenantContext.getTenantId(), command.productId());
        commandGateway.sendAndWait(command);
    }

    /**
     * 驳回产品审核：派发驳回命令。
     *
     * @param command 驳回产品审核命令
     */
    public void rejectAudit(RejectProductAuditCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 修订产品。
     *
     * <p>🔴 修订是 UI 上**唯一可达**的条款改绑定入口（`POST /web/products/{id}/revise`），此前零门禁：
     * 命令直发聚合，而聚合只校验状态 EFFECTIVE，事件溯源构造器再把 {@code newClauseRels} 原样赋值——
     * 不存在的条款、未生效条款、重复条款、零主条款全部可以写进新版本。创建与整体替换两条路径
     * 至少还有一道跨域校验，修订这条最常走的反而没有。</p>
     *
     * <p>🔴 校验范围与聚合的继承语义严格对齐：**只校验调用方本次提供的列表**。
     * {@code newClauseRels} 为 null 表示继承原版本绑定（既有数据，不是本次写入）——若对继承值重跑
     * ACTIVE 校验，存量绑 DRAFT 条款的产品（实测 9 个）将永远无法修订。</p>
     *
     * @param command 修订命令
     * @return 新版本产品ID（修订以独立聚合创建新版本 DRAFT，供调用方跳转/回显）
     */
    public String reviseProduct(ReviseProductCommand command) {
        if (command.newClauseRels() != null) {
            validateClauseBindable(clauseIdsOf(command.newClauseRels()), command.tenantId(),
                    ReviseProductCommand.class.getSimpleName(), "newClauseRels");
        }
        commandGateway.sendAndWait(command);
        return command.newProductId();
    }

    /**
     * 下架产品。
     *
     * @param productId 产品ID
     */
    public void invalidateProduct(String productId) {
        commandGateway.sendAndWait(new InvalidateProductCommand(productId));
    }

    /**
     * 更新产品条款关系（整体替换）。
     *
     * <p>🔴 与创建路径<b>共用同一道条款可绑定性校验</b>：本路径此前只做聚合内的纯领域不变量
     * （非空 / 恰一条主条款 / 不重复），**任何字符串都能整体替换掉现有绑定**——包括条款编码、
     * 包括已归档或从未生效的条款。它比创建路径更危险：创建至少还有一道跨域校验把关，
     * 而整体替换会**一次性抹掉**原有全部合法绑定。</p>
     *
     * @param command 更新命令
     */
    public void updateProductClauseRel(UpdateProductClauseRelCommand command) {
        validateClauseBindable(clauseIdsOf(command.newClauseRels()), command.tenantId(),
                UpdateProductClauseRelCommand.class.getSimpleName(), "newClauseRels");
        commandGateway.sendAndWait(command);
    }

    /**
     * 更新销售渠道。
     *
     * @param command 更新命令
     */
    public void updateSalesChannel(UpdateSalesChannelCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 更新附加险。
     *
     * @param command 更新命令
     */
    public void updateAttachProduct(UpdateAttachProductCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 配置寿险产品规格：以产品ID定位其关联的产品模板，再对模板派发寿险规格配置命令。
     * <p>
     * 寿险规格（投保年龄/保额范围/缴费期/保障期）是产品模板（{@code ProductTemplate}）聚合的专属配置，
     * 命令以 {@code templateId} 为聚合标识。产品→模板的定位属跨聚合读校验，由 application 编排完成
     * （聚合根不查其它聚合读模型）；模板不存在时抛校验异常。
     * </p>
     *
     * @param productId 产品ID
     * @param lifeProductSpec 寿险产品规格
     * @param tenantId 租户ID
     */
    public void configureLifeProduct(String productId, LifeProductSpec lifeProductSpec, String tenantId) {
        ProductTemplateQueryResult template = productTemplateQueryService.getTemplateByProductId(productId, tenantId);
        if (template == null) {
            throw new CommandValidationException(ConfigureLifeProductCommand.class.getSimpleName(), "productId",
                    "产品关联的产品模板不存在: " + productId);
        }
        commandGateway.sendAndWait(new ConfigureLifeProductCommand(template.getTemplateId(), lifeProductSpec, tenantId));
    }

    /**
     * 更新责任级保额档次配置：先校验责任编码归属，再派发整体替换命令。
     * <p>
     * 与条款绑定同构的编排——{@code DRAFT} 前置与七条不变量在聚合根/值对象内裁决，
     * application 只做它做不到的那一件事：<b>跨域取责任清单</b>（产品域只有「产品-条款关联」，
     * 不含条款主数据，见 {@code ClauseExistencePort#listCoverageCodes}）。
     * </p>
     * <p>
     * 🔴 <b>归属校验的必要性</b>：{@code coverageCode} 是裸字符串，聚合根无从判定其真伪。
     * 不校验就能给一个产品配上它根本没有的责任档次——产品侧写成功、投保侧永远匹配不到，
     * 属「配了没用」的静默失效。判据是本产品<b>已绑定条款的责任集合</b>。
     * </p>
     * <p>
     * ⚠️ <b>已知边界：绑定关系读自读模型，最终一致</b>。刚改完条款绑定立刻配置档次，
     * 可能因投影未收敛而误判「责任不属于该产品」。此处刻意不做重试：重试会把
     * 「确实配错了」也拖成长延迟；运营重试一次即可，且报文已列出<b>本次读到的责任集合</b>，
     * 可直接判断是配错还是读到了旧值。
     * </p>
     *
     * @param command 更新命令
     */
    public void updateCoverageSumInsuredConfig(UpdateCoverageSumInsuredConfigCommand command) {
        validateCoverageOwnership(command);
        commandGateway.sendAndWait(command);
    }

    /**
     * 校验档次配置的责任编码是否都属于该产品已绑定条款的责任集合。
     * <p>
     * 清单为 {@code null} 时不在此拦截——{@code null} 与「空清单（清空）」的区分由聚合根以更精确的
     * 文案拒绝，两处各判一次会漂移。空清单是合法「清空」，无需跨域取证。
     * </p>
     */
    private void validateCoverageOwnership(UpdateCoverageSumInsuredConfigCommand command) {
        List<CoverageSumInsuredConfig> configs = command.coverageSumInsuredConfigs();
        if (configs == null || configs.isEmpty()) {
            return;
        }
        List<String> clauseIds = productQueryAppService.queryProductClauses(command.productId()).stream()
                .map(ProductClauseQueryResult::getClauseId)
                .toList();
        Set<String> ownedCoverageCodes = clauseExistencePort.listCoverageCodes(clauseIds, command.tenantId());
        List<String> unknown = configs.stream()
                .map(CoverageSumInsuredConfig::coverageCode)
                .filter(code -> !ownedCoverageCodes.contains(code))
                .toList();
        if (!unknown.isEmpty()) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    "以下责任编码不属于该产品已绑定条款的责任集合，无法配置保额档次: "
                            + String.join(", ", unknown)
                            + "（本次读到的该产品责任集合=" + ownedCoverageCodes + "）");
        }
    }
}
