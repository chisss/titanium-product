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
import com.titanium.product.valueobject.BillingConfig;
import com.titanium.product.valueobject.ClaimConfig;
import com.titanium.product.valueobject.DividendConfig;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.valueobject.MaintenanceConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PolicyStage;
import com.titanium.product.valueobject.PolicyStructureConfig;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.valueobject.ReinsuranceConfig;
import com.titanium.product.valueobject.UnderwritingConfig;

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

    /**
     * 从持久化状态重建模板聚合（仓储读取用）
     * <p>
     * 补齐行为配置字段（出单模式/保单阶段/保单结构/计费/再保）的恢复，
     * 避免更新后的模板经仓储读回时行为配置丢失。这些字段在写侧表
     * {@code t_product_template} 均有对应列（issuance_mode/policy_stages_json 等）。
     * </p>
     */
    public static ProductTemplate reconstruct(String templateId, String templateCode, String templateName,
                                              InsuranceType insuranceType, IssuanceProcessConfig issuanceProcessConfig,
                                              UnderwritingConfig underwritingConfig, ClaimConfig claimsConfig,
                                              MaintenanceConfig maintenanceConfig, PolicyFormConfig policyFormConfig,
                                              ProductEnum.IssuanceMode issuanceMode, List<PolicyStage> policyStages,
                                              PolicyStructureConfig policyStructureConfig, BillingConfig billingConfig,
                                              ReinsuranceConfig reinsuranceConfig, DividendConfig dividendConfig,
                                              CommonStatus status, String tenantId) {
        ProductTemplate template = new ProductTemplate();
        template.templateId = templateId;
        template.templateCode = templateCode;
        template.templateName = templateName;
        template.insuranceType = insuranceType;
        template.issuanceProcessConfig = issuanceProcessConfig;
        template.underwritingConfig = underwritingConfig;
        template.claimsConfig = claimsConfig;
        template.maintenanceConfig = maintenanceConfig;
        template.policyFormConfig = policyFormConfig;
        template.issuanceMode = issuanceMode;
        template.policyStages = policyStages;
        template.policyStructureConfig = policyStructureConfig;
        template.billingConfig = billingConfig;
        template.reinsuranceConfig = reinsuranceConfig;
        template.dividendConfig = dividendConfig;
        template.status = status;
        template.tenantId = tenantId;
        return template;
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
                LocalDateTime.now()));
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
        AggregateLifecycle.apply(new ProductTemplateUpdatedEvent(command.templateId(), command.templateName(),
                command.issuanceMode(), command.policyStages(), command.underwritingConfig(),
                command.policyStructure(), command.maintenanceConfig(), command.claimConfig(),
                command.billingConfig(), command.reinsuranceConfig(), command.dividendConfig(), command.tenantId(),
                LocalDateTime.now()));
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
