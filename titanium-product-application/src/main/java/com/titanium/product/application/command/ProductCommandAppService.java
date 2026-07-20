package com.titanium.product.application.command;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.exception.CommandValidationException;
import com.titanium.product.command.AuditProductCommand;
import com.titanium.product.command.ConfigureLifeProductCommand;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.command.InvalidateProductCommand;
import com.titanium.product.command.RejectProductAuditCommand;
import com.titanium.product.command.ReviseProductCommand;
import com.titanium.product.command.SubmitProductForAuditCommand;
import com.titanium.product.command.UpdateAttachProductCommand;
import com.titanium.product.command.UpdateProductClauseRelCommand;
import com.titanium.product.command.UpdateSalesChannelCommand;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.query.service.ProductTemplateQueryService;
import com.titanium.product.valueobject.LifeProductSpec;

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

    /**
     * 创建产品：先校验引用的产品模板存在且处于 ACTIVE，再派发创建命令。
     * <p>
     * 模板存在性/状态属跨聚合读校验，由 application 编排完成（聚合根不查其它聚合读模型）。
     * </p>
     *
     * @param command 创建产品命令
     * @return 产品ID
     */
    public String createProduct(CreateProductCommand command) {
        validateTemplateActive(command.templateId(), command.tenantId());
        return commandGateway.sendAndWait(command);
    }

    /**
     * 校验产品模板存在且为 ACTIVE 状态。
     *
     * @param templateId 产品模板ID
     * @param tenantId 租户ID
     */
    private void validateTemplateActive(String templateId, String tenantId) {
        ProductTemplateQueryResult template = productTemplateQueryService.getTemplateById(templateId, tenantId);
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
     * 审核通过产品：派发审核命令。
     *
     * @param command 审核产品命令
     */
    public void auditProduct(AuditProductCommand command) {
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
     * @param command 修订命令
     */
    public void reviseProduct(ReviseProductCommand command) {
        commandGateway.sendAndWait(command);
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
     * 更新产品条款关系。
     *
     * @param command 更新命令
     */
    public void updateProductClauseRel(UpdateProductClauseRelCommand command) {
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
}
