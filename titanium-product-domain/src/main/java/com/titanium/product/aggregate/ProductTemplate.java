package com.titanium.product.aggregate;

import java.time.LocalDateTime;
import java.util.List;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import com.titanium.common.domain.BaseAggregate;
import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.metadata.exception.CommandValidationException;
import com.titanium.product.command.ActivateProductTemplateCommand;
import com.titanium.product.command.ConfigureLifeProductCommand;
import com.titanium.product.command.CreateProductTemplateCommand;
import com.titanium.product.command.DeactivateProductTemplateCommand;
import com.titanium.product.command.UpdateProductTemplateCommand;
import com.titanium.product.event.LifeProductConfiguredEvent;
import com.titanium.product.event.ProductTemplateActivatedEvent;
import com.titanium.product.event.ProductTemplateCreatedEvent;
import com.titanium.product.event.ProductTemplateDeactivatedEvent;
import com.titanium.product.event.ProductTemplateUpdatedEvent;
import com.titanium.product.exception.ProductStatusPreconditionException;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.valueobject.PolicyStage;
import com.titanium.product.valueobject.config.BillingConfig;
import com.titanium.product.valueobject.config.ClaimConfig;
import com.titanium.product.valueobject.config.DividendConfig;
import com.titanium.product.valueobject.config.IssuanceProcessConfig;
import com.titanium.product.valueobject.config.MaintenanceConfig;
import com.titanium.product.valueobject.config.PolicyFormConfig;
import com.titanium.product.valueobject.config.PolicyStructureConfig;
import com.titanium.product.valueobject.config.ReinsuranceConfig;
import com.titanium.product.valueobject.config.UnderwritingConfig;
import com.titanium.product.valueobject.pricing.pricing.PricingBasicRule;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 产品模板聚合根
 * <p>
 * 产品工厂的核心聚合根，为不同险种类型提供标准化的产品模板配置。 一个险种类型可以对应多个模板（如车险的交强险模板、商业险模板）。
 * 模板决定：出单模式、核保策略、理赔流程、保全规则等。
 */
@Getter
@SuperBuilder(toBuilder = true)
@Aggregate
public class ProductTemplate extends BaseAggregate {

    @AggregateIdentifier
    private String                templateId;
    private String                templateCode;
    private String                templateName;
    private InsuranceType         insuranceType;
    private String                description;
    private IssuanceProcessConfig issuanceProcessConfig;
    private UnderwritingConfig    underwritingConfig;
    private ClaimConfig           claimsConfig;
    private MaintenanceConfig     maintenanceConfig;
    private PolicyFormConfig      policyFormConfig;
    private PricingBasicRule      pricingBasicRule;
    private List<String>          supportedCoverages;
    private List<String>          supportedExclusions;
    // 以下为行为配置字段：由 UpdateProductTemplateCommand 写入，创建期可为空，更新后成为模板真实状态
    private ProductEnum.IssuanceMode issuanceMode;
    private List<PolicyStage>     policyStages;
    private PolicyStructureConfig policyStructureConfig;
    private BillingConfig         billingConfig;
    private ReinsuranceConfig     reinsuranceConfig;
    // 分红配置：分红险模板专属，红利分配方式 + 三档演示利率，经 UpdateProductTemplateCommand 写入
    private DividendConfig        dividendConfig;
    // 寿险产品规格：寿险模板专属，投保年龄/保额范围/缴费期/保障期，经 ConfigureLifeProductCommand 写入
    private LifeProductSpec       lifeProductSpec;
    private CommonStatus          status;

    public ProductTemplate() {
    }

    @CommandHandler
    public ProductTemplate(CreateProductTemplateCommand command) {
        // 验证必要参数
        String commandName = CreateProductTemplateCommand.class.getSimpleName();
        if (command.templateCode() == null || command.templateName() == null) {
            throw new CommandValidationException(commandName, "templateCode/templateName", "模板代码和名称不能为空");
        }
        if (command.insuranceType() == null) {
            throw new CommandValidationException(commandName, "insuranceType", "险种类型不能为空");
        }
        if (command.issuanceProcessConfig() == null) {
            throw new CommandValidationException(commandName, "issuanceProcessConfig", "出单流程配置不能为空");
        }

        // 发布创建事件
        AggregateLifecycle.apply(new ProductTemplateCreatedEvent(command.templateId(), command.templateCode(),
                command.templateName(), command.insuranceType(), command.description(), command.issuanceProcessConfig(),
                command.underwritingConfig(), command.claimsConfig(), command.maintenanceConfig(),
                command.policyFormConfig(), command.pricingBasicRule(), command.supportedCoverages(),
                command.supportedExclusions(), CommonStatus.ACTIVE, command.tenantId(), command.createdBy(),
                LocalDateTime.now(), command.policyStructure()));
    }

    @EventSourcingHandler
    public void on(ProductTemplateCreatedEvent event) {
        this.templateId = event.templateId();
        this.templateCode = event.templateCode();
        this.templateName = event.templateName();
        this.insuranceType = event.insuranceType();
        this.description = event.description();
        this.issuanceProcessConfig = event.issuanceProcessConfig();
        this.underwritingConfig = event.underwritingConfig();
        this.claimsConfig = event.claimsConfig();
        this.maintenanceConfig = event.maintenanceConfig();
        this.policyFormConfig = event.policyFormConfig();
        this.pricingBasicRule = event.pricingBasicRule();
        this.supportedCoverages = event.supportedCoverages();
        this.supportedExclusions = event.supportedExclusions();
        this.policyStructureConfig = event.policyStructure();
        this.status = event.status();
        this.tenantId = event.tenantId();
        this.createTime = event.occurredAt();
        this.updateTime = event.occurredAt();
    }

    /**
     * 更新模板行为配置：仅 ACTIVE/INACTIVE 态可更新，已删除模板拒绝更新。
     */
    @CommandHandler
    public void handle(UpdateProductTemplateCommand command) {
        if (this.status == CommonStatus.DELETED) {
            throw new ProductStatusPreconditionException(this.templateId, statusName(), "更新");
        }
        // 🔴 D-501-38：配置类字段「未提交（null）即保留原值」。
        // 根因：本命令是**部分字段提交**（BFF 纯透传、前端逐字段构造载荷），而事件的
        // @EventSourcingHandler 是**无条件赋值** ⇒ 客户端没带哪个配置字段，哪个就被静默清空。
        // 实测受害者：policyStages / underwritingConfig / policyStructure 三项**在 UI 上既无填写入口
        // 也无展示位置**，用户点一次「保存配置」即被抹成 NULL 且完全不可见；出单步骤链因此丢失后，
        // 没有任何 UI 入口可恢复。前端 handleSave 中「分红配置仅在选择了分配方式时下发（避免空分红
        // 覆盖非分红险）」的绕过写法，正是该语义缺失的旁证。
        // 🔴 修在**命令处理器**而非 @EventSourcingHandler：后者参与事件重放，改成「null 则跳过」会让
        // 存量事件的回放结果偏离历史事实（同一事件流重放出两种状态），且投影与聚合会就此分叉。
        // 在这里解析则事件依旧无条件承载**最终值**，重放语义保持不变。
        // 🔴 诚实边界：本修复**不回溯**已丢失的数据 —— 事件日志不可变，此前被清空的字段仍是 NULL，
        // 需运维按需回填；本修复只保证「此后不再发生」。
        // 清空某配置的正确表达是提交一个空配置对象，而不是省略该字段。
        AggregateLifecycle.apply(new ProductTemplateUpdatedEvent(command.templateId(), command.templateName(),
                command.issuanceMode(), orCurrent(command.policyStages(), this.policyStages),
                orCurrent(command.underwritingConfig(), this.underwritingConfig),
                orCurrent(command.policyStructure(), this.policyStructureConfig),
                orCurrent(command.maintenanceConfig(), this.maintenanceConfig),
                orCurrent(command.claimConfig(), this.claimsConfig),
                orCurrent(command.billingConfig(), this.billingConfig),
                orCurrent(command.reinsuranceConfig(), this.reinsuranceConfig),
                orCurrent(command.dividendConfig(), this.dividendConfig), command.tenantId(),
                LocalDateTime.now()));
    }

    /** 未提交（null）时保留聚合当前值；已提交则取新值（见 {@link #handle(UpdateProductTemplateCommand)}） */
    private static <T> T orCurrent(T incoming, T current) {
        return incoming != null ? incoming : current;
    }

    /**
     * 配置寿险产品规格：仅非删除态可配置。校验寿险规格非空后写入投保年龄/保额范围/缴费期/保障期选项。
     */
    @CommandHandler
    public void handle(ConfigureLifeProductCommand command) {
        if (this.status == CommonStatus.DELETED) {
            throw new ProductStatusPreconditionException(this.templateId, statusName(), "配置寿险规格");
        }
        if (command.lifeProductSpec() == null) {
            throw new CommandValidationException(ConfigureLifeProductCommand.class.getSimpleName(), "lifeProductSpec",
                    "寿险产品规格不能为空");
        }
        AggregateLifecycle.apply(new LifeProductConfiguredEvent(command.templateId(), command.lifeProductSpec(),
                command.tenantId(), LocalDateTime.now()));
    }

    @EventSourcingHandler
    public void on(LifeProductConfiguredEvent event) {
        this.lifeProductSpec = event.lifeProductSpec();
        this.updateTime = event.occurredAt();
    }

    /**
     * 激活模板：已激活则幂等返回，不重复产生事件。
     */
    @CommandHandler
    public void handle(ActivateProductTemplateCommand command) {
        if (this.status == CommonStatus.ACTIVE) {
            return;
        }
        AggregateLifecycle.apply(new ProductTemplateActivatedEvent(command.templateId(), command.tenantId(),
                LocalDateTime.now()));
    }

    /**
     * 停用模板：仅 ACTIVE 态可停用。
     */
    @CommandHandler
    public void handle(DeactivateProductTemplateCommand command) {
        if (this.status != CommonStatus.ACTIVE) {
            throw new ProductStatusPreconditionException(this.templateId, statusName(), "停用");
        }
        AggregateLifecycle.apply(new ProductTemplateDeactivatedEvent(command.templateId(), command.tenantId(),
                LocalDateTime.now()));
    }

    @EventSourcingHandler
    public void on(ProductTemplateUpdatedEvent event) {
        this.templateName = event.templateName();
        this.issuanceMode = event.issuanceMode();
        this.policyStages = event.policyStages();
        this.underwritingConfig = event.underwritingConfig();
        this.policyStructureConfig = event.policyStructure();
        this.maintenanceConfig = event.maintenanceConfig();
        // 命令/事件的 claimConfig（单数）对应聚合的 claimsConfig（复数）
        this.claimsConfig = event.claimConfig();
        this.billingConfig = event.billingConfig();
        this.reinsuranceConfig = event.reinsuranceConfig();
        this.dividendConfig = event.dividendConfig();
        this.updateTime = event.occurredAt();
    }

    @EventSourcingHandler
    public void on(ProductTemplateActivatedEvent event) {
        this.status = CommonStatus.ACTIVE;
        this.updateTime = event.occurredAt();
    }

    @EventSourcingHandler
    public void on(ProductTemplateDeactivatedEvent event) {
        this.status = CommonStatus.INACTIVE;
        this.updateTime = event.occurredAt();
    }

    /**
     * 当前状态名，null 安全，用于异常信息展示。
     */
    private String statusName() {
        return this.status != null ? this.status.name() : "UNKNOWN";
    }
}
