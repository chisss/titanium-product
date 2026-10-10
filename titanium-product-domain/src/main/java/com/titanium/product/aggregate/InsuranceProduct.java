package com.titanium.product.aggregate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import com.titanium.common.domain.BaseAggregate;
import com.titanium.metadata.enums.insurance.InsuranceMandateType;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.CommandValidationException;
import com.titanium.metadata.exception.DomainException;
import com.titanium.product.command.AuditProductCommand;
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
import com.titanium.product.event.CoverageSumInsuredConfigUpdatedEvent;
import com.titanium.product.event.ProductAttachUpdatedEvent;
import com.titanium.product.event.ProductAuditRejectedEvent;
import com.titanium.product.event.ProductAuditedEvent;
import com.titanium.product.event.ProductClauseRelUpdatedEvent;
import com.titanium.product.event.ProductCreatedEvent;
import com.titanium.product.event.ProductInvalidatedEvent;
import com.titanium.product.event.ProductRevisedEvent;
import com.titanium.product.event.ProductSalesChannelUpdatedEvent;
import com.titanium.product.event.ProductSubmittedForAuditEvent;
import com.titanium.product.exception.ProductAuditException;
import com.titanium.product.exception.ProductStatusPreconditionException;
import com.titanium.product.service.ProductDomainService;
import com.titanium.product.valueobject.AuditInfo;
import com.titanium.product.valueobject.config.CoveragePeriodConfig;
import com.titanium.product.valueobject.config.CoverageSumInsuredConfig;
import com.titanium.product.valueobject.config.DocumentConfig;
import com.titanium.product.valueobject.config.InsureCondition;
import com.titanium.product.valueobject.config.IssuanceProcessConfig;
import com.titanium.product.valueobject.config.PaymentConfig;
import com.titanium.product.valueobject.config.PolicyFormConfig;
import com.titanium.product.valueobject.config.PolicyStructureConfig;
import com.titanium.product.valueobject.config.SalesChannelConfig;
import com.titanium.product.valueobject.config.UnderwritingConfig;
import com.titanium.product.valueobject.pricing.pricing.ActuarialBasis;
import com.titanium.product.valueobject.pricing.pricing.PricingBasicRule;
import com.titanium.product.valueobject.rate.RateTableRef;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 保险产品聚合根 核心聚合根，封装产品的基础信息、形态、险种、绑定条款、定价基础规则、 出单流程配置、保单形态配置、核保配置等核心业务配置
 */
@Getter
@SuperBuilder(toBuilder = true)
@Aggregate
public class InsuranceProduct extends BaseAggregate {

    // ====== 基础标识 ======
    /** 产品编号（聚合根ID） */
    @AggregateIdentifier
    private String                      productId;
    /** 所属产品模板ID（单向引用 ProductTemplate，一模板对多产品） */
    private String                      templateId;
    /** 产品代码（简短唯一标识） */
    private String                      productCode;
    /** 产品名称 */
    private String                      productName;
    /** 产品描述 */
    private String                      productDesc;
    /** 创建人（登录用户显示名，随创建命令由前端传入） */
    private String                      createdBy;

    // ====== 产品分类与形态 ======
    /** 产品形态（团险/个险） */
    private ProductEnum.ProductForm     form;
    /** 险种类型 */
    private InsuranceProductType        insuranceType;
    /**
     * 险别（法定强制/自愿投保，G11/g11-04 新增）。
     * <p>
     * 与险种<b>正交</b>的维度（见 {@link InsuranceMandateType} javadoc）：车险「交强 + 商业同单」即
     * 同一 {@code AUTO} 险种下的两个险别。{@code null} = 未声明险别，消费方（{@code PolicyProduct#isCompulsory()}）
     * 按「非强制」处理——存量产品与全部非车险产品均不声明，不得据此推断为自愿。
     * </p>
     */
    private InsuranceMandateType        insuranceMandateType;
    /** 产品类别（MAIN-主险/RIDER-附加险） */
    private ProductEnum.ProductCategory category;

    // ====== 版本与状态 ======
    /** 产品版本（如 V1.0/V2.0） */
    private String                      version;
    /** 产品状态（DRAFT/AUDITING/EFFECTIVE/INVALID） */
    private ProductEnum.ProductStatus   status;
    /** 原始产品ID（修订溯源） */
    private String                      originalProductId;

    // ====== 时间管理 ======
    /** 产品生效时间 */
    private LocalDateTime               effectiveTime;
    /** 下架时间 */
    private LocalDateTime               invalidTime;
    /** 销售开始时间 */
    private LocalDateTime               saleStartTime;
    /** 销售截止时间 */
    private LocalDateTime               saleEndTime;

    // ====== 核心配置（值对象） ======
    /** 投保条件 */
    private InsureCondition             insureCondition;
    /** 保障期间配置 */
    private CoveragePeriodConfig        coveragePeriod;
    /** 缴费方式配置 */
    private PaymentConfig               paymentConfig;
    /** 定价基础规则 */
    private PricingBasicRule            pricingBasicRule;

    // ====== 责任级保额档次 ======
    /**
     * 责任级保额档次配置（按 coverageCode 唯一，见 {@link CoverageSumInsuredConfig#assertDistinctCoverageCodes}）。
     * <p>
     * 空清单合法——「暂不为任何责任配置保额档次」是允许的中间状态。持久化为「一条责任一行」的配置集合，
     * 不是产品级的一个大列表。
     * </p>
     */
    private List<CoverageSumInsuredConfig> coverageSumInsuredConfigs;

    // ====== 关联 ======
    /** 条款关联列表 */
    private List<ProductClauseRel>      clauseRels;
    /** 销售渠道配置 */
    private List<SalesChannelConfig>    salesChannels;
    /** 可搭配的附加险产品ID */
    private List<String>                attachProductIds;

    // ====== 出单流程编排（核心新增） ======
    /** 出单流程配置 */
    private IssuanceProcessConfig       issuanceProcessConfig;
    /** 保单形态配置 */
    private PolicyFormConfig            policyFormConfig;
    /** 核保配置 */
    private UnderwritingConfig          underwritingConfig;
    /** 文档配置（所需投保材料清单 + 生成文档模板清单，纯产品配置不跨文档域） */
    private DocumentConfig              documentConfig;
    /** 保单结构配置（标的类型/标的字段Schema/多标的/参与方角色，宠物险等非人险标的产品的标的结构定义） */
    private PolicyStructureConfig       policyStructureConfig;

    // ====== 定价模式（保费计算数据源与方法，billing 出单按此分派） ======
    /** 定价模式（费率表查询/精算公式） */
    private PricingMode                 pricingMode;
    /** 费率表引用（pricingMode=RATE_TABLE 时使用） */
    private RateTableRef                rateTableRef;
    /** 精算基础参数（pricingMode=ACTUARIAL_FORMULA 时使用） */
    private ActuarialBasis              actuarialBasis;

    // ====== 审核 ======
    /** 审核信息 */
    private AuditInfo                   auditInfo;

    // ====== 规则引擎预留 ======
    /** 定价规则集ID（接入规则引擎后使用） */
    private String                      pricingRuleSetId;
    /** 投保条件规则集ID（接入规则引擎后使用） */
    private String                      insureConditionRuleSetId;
    /** 核保规则集ID（接入规则引擎后使用） */
    private String                      underwritingRuleSetId;

    // ====== 租户 ======
    // 租户ID（tenantId）由基类 BaseAggregate 提供

    /** 无参构造器（Axon反射必备） */
    public InsuranceProduct() {
    }

    // ==================== 命令处理器 ====================

    /**
     * 创建产品
     */
    @CommandHandler
    public InsuranceProduct(CreateProductCommand command, ProductDomainService productDomainService) {
        validateCreateCommand(command, productDomainService);

        this.productId = command.productId();
        this.templateId = command.templateId();
        this.productCode = command.productCode();
        this.productName = command.productName();
        this.productDesc = command.productDesc();
        this.form = command.form();
        this.insuranceType = command.insuranceType();
        // 险别（G11/g11-04）：与险种正交的可选维度，未传即 null（「未声明」，非「自愿」）
        this.insuranceMandateType = command.insuranceMandateType();
        this.category = command.category() != null ? command.category() : ProductEnum.ProductCategory.MAIN;
        this.version = "V1.0";
        this.status = ProductEnum.ProductStatus.DRAFT;
        this.saleStartTime = command.saleStartTime();
        this.saleEndTime = command.saleEndTime();
        this.insureCondition = command.insureCondition();
        this.coveragePeriod = command.coveragePeriod();
        this.paymentConfig = command.paymentConfig();
        this.pricingBasicRule = command.pricingBasicRule();
        // 条款版本映射为可选元数据（前端可不传），缺省视为无显式版本，避免空指针
        Map<String, String> clauseVersionMap = command.clauseVersionMap() != null
                ? command.clauseVersionMap() : Map.of();
        this.clauseRels = command
                .clauseIds().stream().map(clauseId -> new ProductClauseRel(clauseId,
                        clauseVersionMap.get(clauseId), clauseId.equals(command.mainClauseId())))
                .collect(Collectors.toList());
        this.salesChannels = command.salesChannels();
        this.attachProductIds = command.attachProductIds();
        this.issuanceProcessConfig = command.issuanceProcessConfig();
        this.policyFormConfig = command.policyFormConfig();
        this.underwritingConfig = command.underwritingConfig();
        this.pricingMode = command.pricingMode();
        this.rateTableRef = command.rateTableRef();
        this.actuarialBasis = command.actuarialBasis();
        this.documentConfig = command.documentConfig();
        this.policyStructureConfig = command.policyStructureConfig();
        this.tenantId = command.tenantId();
        this.createdBy = command.createdBy();

        AggregateLifecycle.apply(
                new ProductCreatedEvent(productId, templateId, productCode, productName, productDesc, form,
                        insuranceType, category,
                        version, ProductEnum.ProductStatus.DRAFT, LocalDateTime.now(), saleStartTime, saleEndTime,
                        insureCondition, coveragePeriod, paymentConfig, pricingBasicRule, clauseRels, salesChannels,
                        attachProductIds, issuanceProcessConfig, policyFormConfig, underwritingConfig, tenantId,
                        pricingMode, rateTableRef, actuarialBasis, documentConfig, createdBy, policyStructureConfig,
                        insuranceMandateType));
    }

    /**
     * 修订新版本聚合的工厂构造器（非命令处理器）
     * <p>
     * 由原 EFFECTIVE 产品在 {@link #handle(ReviseProductCommand)} 中经 {@link AggregateLifecycle#createNew}
     * 调用，为 {@code newProductId} 建立独立事件流。此处 apply {@link ProductRevisedEvent} 作为新聚合的首个事件，
     * 状态初始化由 {@link #on(ProductRevisedEvent)} 承接。
     * </p>
     */
    public InsuranceProduct(ProductRevisedEvent event) {
        AggregateLifecycle.apply(event);
    }

    /**
     * 提交产品审核（DRAFT → AUDITING）
     */
    @CommandHandler
    public void handle(SubmitProductForAuditCommand command) {
        if (!ProductEnum.ProductStatus.DRAFT.equals(this.status)) {
            throw new ProductStatusPreconditionException(this.productId, statusName(), "提交审核");
        }
        this.status = ProductEnum.ProductStatus.AUDITING;
        AggregateLifecycle.apply(new ProductSubmittedForAuditEvent(productId, command.submitterId(),
                command.submitterName(), LocalDateTime.now()));
    }

    /**
     * 审核产品通过（AUDITING → EFFECTIVE）
     */
    @CommandHandler
    public void handle(AuditProductCommand command) {
        if (!ProductEnum.ProductStatus.AUDITING.equals(this.status)) {
            throw new ProductStatusPreconditionException(this.productId, statusName(), "审核");
        }
        if (ProductEnum.AuditResult.PASS.equals(command.auditResult())) {
            this.status = ProductEnum.ProductStatus.EFFECTIVE;
            this.effectiveTime = LocalDateTime.now();
            this.auditInfo = new AuditInfo(command.auditorId(), command.auditorName(), command.auditOpinion(),
                    LocalDateTime.now(), ProductEnum.AuditResult.PASS);
            AggregateLifecycle.apply(new ProductAuditedEvent(productId, ProductEnum.ProductStatus.EFFECTIVE,
                    effectiveTime, auditInfo, this.tenantId));
        } else {
            // 审核驳回走RejectProductAuditCommand
            throw new ProductAuditException(this.productId, "审核不通过请使用驳回命令");
        }
    }

    /**
     * 驳回产品审核（AUDITING → DRAFT）
     */
    @CommandHandler
    public void handle(RejectProductAuditCommand command) {
        if (!ProductEnum.ProductStatus.AUDITING.equals(this.status)) {
            throw new ProductStatusPreconditionException(this.productId, statusName(), "驳回审核");
        }
        this.status = ProductEnum.ProductStatus.DRAFT;
        this.auditInfo = new AuditInfo(command.auditorId(), command.auditorName(), command.rejectReason(),
                LocalDateTime.now(), ProductEnum.AuditResult.REJECT);
        AggregateLifecycle.apply(new ProductAuditRejectedEvent(productId, ProductEnum.ProductStatus.DRAFT, auditInfo,
                LocalDateTime.now()));
    }

    /**
     * 产品修订（EFFECTIVE → 创建新版本DRAFT）
     */
    @CommandHandler
    public void handle(ReviseProductCommand command) {
        if (!ProductEnum.ProductStatus.EFFECTIVE.equals(this.status)) {
            throw new ProductStatusPreconditionException(this.productId, statusName(), "修订");
        }
        String newVersion = generateNewVersion(this.version);
        // 修订生成新版本：templateId/productCode/attachProductIds/tenantId 继承原产品（修订主要变更定价/条款，模板与附加险搭配延续）；
        // 销售渠道/费率表引用/精算基础读模型未对外暴露，命令未携带时继承原值，防止修订意外清空
        List<SalesChannelConfig> newSalesChannels = command.newSalesChannels() != null
                ? command.newSalesChannels() : this.salesChannels;
        RateTableRef newRateTableRef = command.newRateTableRef() != null
                ? command.newRateTableRef() : this.rateTableRef;
        ActuarialBasis newActuarialBasis = command.newActuarialBasis() != null
                ? command.newActuarialBasis() : this.actuarialBasis;
        // 条款绑定同规则：命令未携带时继承原版本绑定。绑定非空是硬不变量，「清空」从来不是合法意图，
        // 故此处绝不能像定价/核保那样把 null 直接落库（那会让新版本静默丢掉全部条款）
        List<ProductClauseRel> newClauseRels = command.newClauseRels() != null
                ? command.newClauseRels() : this.clauseRels;
        validateClauseRels(ReviseProductCommand.class.getSimpleName(), newClauseRels);
        // 保额档次：责任级配置，随版本整体承继。新版本落在 DRAFT，运营再经
        // UpdateCoverageSumInsuredConfigCommand 按需调整；修订命令不承载该配置，
        // 以免「改保额档次」这条有专用入口的路径被复制成第二份实现。
        List<CoverageSumInsuredConfig> newCoverageSumInsuredConfigs = this.coverageSumInsuredConfigs != null
                ? this.coverageSumInsuredConfigs : List.of();
        ProductRevisedEvent revisedEvent = new ProductRevisedEvent(command.newProductId(), this.templateId,
                this.productId, newVersion, this.productCode, command.newProductName(), command.newProductDesc(),
                command.newForm(), command.newInsuranceType(), command.newCategory(), command.newInsureCondition(),
                command.newCoveragePeriod(), command.newPaymentConfig(), newClauseRels,
                command.newPricingBasicRule(), newSalesChannels, command.newIssuanceProcessConfig(),
                command.newPolicyFormConfig(), command.newUnderwritingConfig(), this.attachProductIds,
                command.newPricingMode(), newRateTableRef, newActuarialBasis, this.tenantId,
                newCoverageSumInsuredConfigs,
                // 险别继承原版本（与 templateId/productCode/attachProductIds 同族）：险别是产品身份的一部分，
                // 修订改定价与条款、不改变「法定强制/自愿投保」的定性；要换险别应另建产品
                this.insuranceMandateType);
        // 修订不改写当前 EFFECTIVE 版本，而以全新的 newProductId 创建独立聚合，使新版本拥有自己的事件流并可被命令寻址
        try {
            AggregateLifecycle.createNew(InsuranceProduct.class, () -> new InsuranceProduct(revisedEvent));
        } catch (Exception e) {
            throw new ProductStatusPreconditionException(this.productId, statusName(), "修订：创建新版本聚合异常");
        }
    }

    /**
     * 产品下架（EFFECTIVE → INVALID）
     */
    @CommandHandler
    public void handle(InvalidateProductCommand command) {
        if (!ProductEnum.ProductStatus.EFFECTIVE.equals(this.status)) {
            throw new ProductStatusPreconditionException(this.productId, statusName(), "下架");
        }
        this.status = ProductEnum.ProductStatus.INVALID;
        this.invalidTime = LocalDateTime.now();
        AggregateLifecycle.apply(new ProductInvalidatedEvent(productId, ProductEnum.ProductStatus.INVALID,
                invalidTime, this.tenantId));
    }

    /**
     * 更新产品条款关联（仅DRAFT状态）
     */
    @CommandHandler
    public void handle(UpdateProductClauseRelCommand command) {
        if (!ProductEnum.ProductStatus.DRAFT.equals(this.status)) {
            throw new ProductStatusPreconditionException(this.productId, statusName(), "更新条款关联");
        }
        validateClauseRels(UpdateProductClauseRelCommand.class.getSimpleName(), command.newClauseRels());
        this.clauseRels = command.newClauseRels();
        AggregateLifecycle.apply(new ProductClauseRelUpdatedEvent(productId, clauseRels));
    }

    /**
     * 更新责任级保额档次配置（仅 DRAFT 状态，整体替换语义）
     * <p>
     * 🔴 <b>命令清单为 {@code null} 一律拒绝，不用「null 即清空」的宽松解释</b>。与条款绑定不同，
     * 这里「一条都不配」是<b>合法</b>状态，于是 null 与空清单若都当清空处理，调用方漏传字段就会
     * <b>静默抹掉全部保额档次</b>——正是 §7.14 记载的那类事故（修订一次静默清空全部条款绑定）。
     * 故显式要求调用方用空清单表达「清空」，让「漏传」与「有意清空」在协议层就分得开。
     * </p>
     * <p>
     * 不变量①（同产品下 coverageCode 不可重复）在本处理器裁决：它是「一组配置之间」的性质，
     * 单个 {@link CoverageSumInsuredConfig} 自身判不了。②～⑦ 已在值对象紧凑构造器内裁决，此处不重复。
     * </p>
     *
     * @param command 更新命令
     */
    @CommandHandler
    public void handle(UpdateCoverageSumInsuredConfigCommand command) {
        if (!ProductEnum.ProductStatus.DRAFT.equals(this.status)) {
            throw new ProductStatusPreconditionException(this.productId, statusName(), "更新保额档次配置");
        }
        if (command.coverageSumInsuredConfigs() == null) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    "命令 " + UpdateCoverageSumInsuredConfigCommand.class.getSimpleName()
                            + " 字段 coverageSumInsuredConfigs 校验失败: 不得为 null，清空请传空清单");
        }
        CoverageSumInsuredConfig.assertDistinctCoverageCodes(command.coverageSumInsuredConfigs());
        this.coverageSumInsuredConfigs = command.coverageSumInsuredConfigs();
        AggregateLifecycle.apply(new CoverageSumInsuredConfigUpdatedEvent(productId,
                command.coverageSumInsuredConfigs(), this.tenantId));
    }

    /**
     * 校验待替换的条款关联列表满足最小不变量。
     * <p>
     * 🔴 本处理器此前**零校验**——任何列表都能直接整体替换现有绑定。补上三条纯领域不变量：
     * 非空、恰有一条主条款、条款不重复（判据与创建路径同源：relId 由「条款ID + 版本」派生，
     * 重复条目会产生同 relId 的两条关联）。条款的**真伪与状态**（是否存在、是否已生效）是跨域问题，
     * 不在聚合内判定——见 application 层 {@code ClauseExistencePort} 校验。
     * </p>
     *
     * @param commandName 发起命令的类名（错误定位用）
     * @param newRels     待替换的条款关联列表
     */
    private void validateClauseRels(String commandName, List<ProductClauseRel> newRels) {
        assertClauseRelsNotEmpty(commandName, "newClauseRels", newRels);
        long mainCount = newRels.stream().filter(rel -> Boolean.TRUE.equals(rel.isMainClause())).count();
        if (mainCount != 1) {
            throw new CommandValidationException(ProductErrorCode.CLAUSE_BINDING_FAILED,
                    commandName, "newClauseRels", "必须且只能指定一条主条款，当前指定 " + mainCount + " 条");
        }
        assertNoDuplicateClauseIds(commandName, "newClauseRels",
                newRels.stream().map(ProductClauseRel::clauseId).toList());
    }

    /**
     * 条款关联不得为空。
     *
     * <p>🔴 创建（{@code clauseIds}）与整体替换（{@code newClauseRels}）两条路径**共用本实现**——
     * 此前该判据在两处各写一份，R7-02 的负向对照正是因此漏注了一处实现，得出「测试没牙」的错误结论。</p>
     *
     * @param commandName 发起命令的类名（错误定位用）
     * @param fieldName   命令中承载条款引用的字段名（错误定位用）
     * @param clauseIds   待判定的条款引用列表
     */
    private static void assertClauseRelsNotEmpty(String commandName, String fieldName, List<?> clauseIds) {
        if (clauseIds == null || clauseIds.isEmpty()) {
            throw new CommandValidationException(commandName, fieldName, "产品必须绑定至少一条条款");
        }
    }

    /**
     * 条款不得重复绑定。
     *
     * <p>🔴 relId 由「条款ID + "_" + 版本」派生，同一个 clauseId 出现两次会产生**两条 relId 相同的
     * 关联**（读模型 {@code t_product_clause_rel_view} 按 relId 建键时互相覆盖），且两条都满足
     * {@code clauseId.equals(mainClauseId)}，产品会出现「两个主条款」。这是纯领域不变量、不依赖外部信息，
     * 故内聚在聚合内判定（条款**真伪与状态**是跨域问题，另由 application 层经
     * {@code ClauseExistencePort} 校验）。</p>
     *
     * <p>🔴 创建与整体替换两条路径**共用本实现**（同上）。</p>
     *
     * @param commandName 发起命令的类名（错误定位用）
     * @param fieldName   命令中承载条款引用的字段名（错误定位用）
     * @param clauseIds   待判定的条款引用列表
     */
    private static void assertNoDuplicateClauseIds(String commandName, String fieldName, List<String> clauseIds) {
        if (clauseIds.stream().distinct().count() != clauseIds.size()) {
            throw new CommandValidationException(ProductErrorCode.CLAUSE_BINDING_FAILED,
                    commandName, fieldName, "条款不得重复绑定");
        }
    }

    /**
     * 更新销售渠道配置（仅DRAFT状态）
     */
    @CommandHandler
    public void handle(UpdateSalesChannelCommand command) {
        if (!ProductEnum.ProductStatus.DRAFT.equals(this.status)) {
            throw new ProductStatusPreconditionException(this.productId, statusName(), "更新销售渠道");
        }
        this.salesChannels = command.salesChannels();
        AggregateLifecycle.apply(new ProductSalesChannelUpdatedEvent(productId, salesChannels));
    }

    /**
     * 更新附加险关联（仅DRAFT/EFFECTIVE状态）
     */
    @CommandHandler
    public void handle(UpdateAttachProductCommand command) {
        if (!ProductEnum.ProductStatus.DRAFT.equals(this.status)
                && !ProductEnum.ProductStatus.EFFECTIVE.equals(this.status)) {
            throw new ProductStatusPreconditionException(this.productId, statusName(), "更新附加险关联");
        }
        AggregateLifecycle.apply(new ProductAttachUpdatedEvent(productId, command.attachProductIds()));
    }

    // ==================== 事件溯源处理器 ====================

    @EventSourcingHandler
    public void on(ProductCreatedEvent event) {
        this.productId = event.productId();
        this.templateId = event.templateId();
        this.productCode = event.productCode();
        this.productName = event.productName();
        this.productDesc = event.productDesc();
        this.form = event.form();
        this.insuranceType = event.insuranceType();
        // 尾部分量（G11/g11-04 追加）：存量事件载荷无该字段，按属性名绑定读到 null = 「未声明险别」
        this.insuranceMandateType = event.insuranceMandateType();
        this.category = event.category();
        this.version = event.version();
        this.status = event.status();
        this.saleStartTime = event.saleStartTime();
        this.saleEndTime = event.saleEndTime();
        this.insureCondition = event.insureCondition();
        this.coveragePeriod = event.coveragePeriod();
        this.paymentConfig = event.paymentConfig();
        this.pricingBasicRule = event.pricingBasicRule();
        this.clauseRels = event.clauseRels();
        // 保额档次不由创建命令承载：新产品先建空白档，再由 UpdateCoverageSumInsuredConfigCommand 按责任录入。
        // 显式赋空而非留 null，使「未配置」与「已配置为空」在聚合内同形，免去后续各处判空。
        this.coverageSumInsuredConfigs = List.of();
        this.salesChannels = event.salesChannels();
        this.attachProductIds = event.attachProductIds();
        this.issuanceProcessConfig = event.issuanceProcessConfig();
        this.policyFormConfig = event.policyFormConfig();
        this.underwritingConfig = event.underwritingConfig();
        this.pricingMode = event.pricingMode();
        this.rateTableRef = event.rateTableRef();
        this.actuarialBasis = event.actuarialBasis();
        this.documentConfig = event.documentConfig();
        this.policyStructureConfig = event.policyStructureConfig();
        this.tenantId = event.tenantId();
        this.createdBy = event.createdBy();
        this.createTime = event.createdAt();
        this.updateTime = event.createdAt();
    }

    @EventSourcingHandler
    public void on(ProductSubmittedForAuditEvent event) {
        this.status = ProductEnum.ProductStatus.AUDITING;
    }

    @EventSourcingHandler
    public void on(ProductAuditedEvent event) {
        this.status = event.status();
        this.effectiveTime = event.effectiveTime();
        this.auditInfo = event.auditInfo();
    }

    @EventSourcingHandler
    public void on(ProductAuditRejectedEvent event) {
        this.status = event.status();
        this.auditInfo = event.auditInfo();
    }

    /**
     * 修订事件溯源处理器：初始化「新版本」聚合的状态。
     * <p>
     * 该事件是经 {@link AggregateLifecycle#createNew} 创建的新聚合的首个事件，运行在新聚合
     * （{@code newProductId}）自己的事件流上，故此处对 {@code productId} 的赋值是<b>初始化</b>而非改写既有标识。
     * 新版本落为 DRAFT 状态，{@code productCode}/{@code tenantId} 由事件携带（不再从读模型继承），
     * 并通过 {@code originalProductId} 溯源到原产品。
     * </p>
     */
    @EventSourcingHandler
    public void on(ProductRevisedEvent event) {
        this.productId = event.newProductId();
        this.templateId = event.templateId();
        this.originalProductId = event.originalProductId();
        this.productCode = event.productCode();
        this.productName = event.newProductName();
        this.productDesc = event.newProductDesc();
        this.version = event.newVersion();
        this.form = event.newForm();
        this.insuranceType = event.newInsuranceType();
        // 尾部分量（G11/g11-04 追加）：同上，存量载荷读到 null；修订路径的取值由 handle(ReviseProductCommand) 传原值继承
        this.insuranceMandateType = event.insuranceMandateType();
        this.category = event.newCategory();
        this.insureCondition = event.newInsureCondition();
        this.coveragePeriod = event.newCoveragePeriod();
        this.paymentConfig = event.newPaymentConfig();
        this.clauseRels = event.newClauseRels();
        // 尾部分量：存量事件载荷无该字段，授权序列化按属性名绑定会读到 null ⇒ 兜底为空清单
        this.coverageSumInsuredConfigs = event.newCoverageSumInsuredConfigs() != null
                ? event.newCoverageSumInsuredConfigs() : List.of();
        this.pricingBasicRule = event.newPricingBasicRule();
        this.salesChannels = event.newSalesChannels();
        this.attachProductIds = event.attachProductIds();
        this.issuanceProcessConfig = event.newIssuanceProcessConfig();
        this.policyFormConfig = event.newPolicyFormConfig();
        this.underwritingConfig = event.newUnderwritingConfig();
        this.pricingMode = event.newPricingMode();
        this.rateTableRef = event.newRateTableRef();
        this.actuarialBasis = event.newActuarialBasis();
        this.status = ProductEnum.ProductStatus.DRAFT;
        this.tenantId = event.tenantId();
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @EventSourcingHandler
    public void on(ProductInvalidatedEvent event) {
        this.status = event.status();
        this.invalidTime = event.invalidTime();
    }

    @EventSourcingHandler
    public void on(ProductClauseRelUpdatedEvent event) {
        this.clauseRels = event.clauseRels();
    }

    @EventSourcingHandler
    public void on(CoverageSumInsuredConfigUpdatedEvent event) {
        this.coverageSumInsuredConfigs = event.coverageSumInsuredConfigs();
    }

    @EventSourcingHandler
    public void on(ProductSalesChannelUpdatedEvent event) {
        this.salesChannels = event.salesChannels();
    }

    @EventSourcingHandler
    public void on(ProductAttachUpdatedEvent event) {
        this.attachProductIds = event.attachProductIds();
    }

    // ==================== 私有方法 ====================

    private void validateCreateCommand(CreateProductCommand command, ProductDomainService productDomainService) {
        String commandName = CreateProductCommand.class.getSimpleName();
        if (command.productId() == null || command.productName() == null) {
            throw new CommandValidationException(commandName, "productId/productName", "产品编号、名称不能为空");
        }
        if (command.productCode() == null || command.productCode().isBlank()) {
            throw new CommandValidationException(commandName, "productCode", "产品代码不能为空");
        }
        if (command.templateId() == null || command.templateId().isBlank()) {
            throw new CommandValidationException(commandName, "templateId", "产品必须引用一个产品模板");
        }
        assertClauseRelsNotEmpty(commandName, "clauseIds", command.clauseIds());
        assertNoDuplicateClauseIds(commandName, "clauseIds", command.clauseIds());
        // 主条款必须在绑定条款列表中（跨条款关联的纯领域规则，委托领域服务判定）
        if (!productDomainService.validateMainClause(command.clauseIds(), command.mainClauseId())) {
            throw new CommandValidationException(commandName, "mainClauseId",
                    "必须指定一条主条款，且主条款须包含在绑定条款列表中");
        }
        // 出单流程配置与出单模式的步数一致性（配置存在时校验；缺省视为暂不配置，留待提审前补全）
        if (command.issuanceProcessConfig() != null
                && !productDomainService.validateIssuanceConfig(command.issuanceProcessConfig())) {
            throw new CommandValidationException(commandName, "issuanceProcessConfig",
                    "出单流程配置与出单模式不一致：步骤数不满足所选出单模式要求");
        }
        if (command.insureCondition() == null) {
            throw new CommandValidationException(commandName, "insureCondition", "投保条件不能为空");
        }
        // G7 收口：产品级 factors 系数链无消费方（两套定价体系未打通），创建时配置将误导运营以为生效；
        // 动态因子（定价计划 dynamicFactorRefs）才是真实消费路径，此处拦截并引导。
        if (command.pricingBasicRule() != null && command.pricingBasicRule().factors() != null
                && !command.pricingBasicRule().factors().isEmpty()) {
            throw new CommandValidationException(
                    ProductErrorCode.PRICING_FACTORS_DEPRECATED, commandName, "pricingBasicRule.factors",
                    "产品级 factors 系数不参与试算，请改为在定价计划中配置动态因子");
        }
        if (command.tenantId() == null || command.tenantId().isBlank()) {
            throw new CommandValidationException(commandName, "tenantId", "租户ID不能为空");
        }
    }

    /** 返回当前状态名称（状态为空时返回NULL占位，用于异常上下文） */
    private String statusName() {
        return this.status == null ? "NULL" : this.status.name();
    }

    private String generateNewVersion(String currentVersion) {
        int dotIndex = currentVersion.indexOf('.');
        if (dotIndex > 0) {
            int majorVersion = Integer.parseInt(currentVersion.substring(1, dotIndex));
            return "V" + (majorVersion + 1) + ".0";
        }
        return "V2.0";
    }
}
