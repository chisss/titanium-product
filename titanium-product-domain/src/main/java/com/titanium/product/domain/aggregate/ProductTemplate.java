package com.titanium.product.domain.aggregate;

import java.util.List;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.domain.command.ActivateProductTemplateCommand;
import com.titanium.product.domain.command.CreateProductTemplateCommand;
import com.titanium.product.domain.command.DeactivateProductTemplateCommand;
import com.titanium.product.domain.command.UpdateProductTemplateCommand;
import com.titanium.product.domain.event.ProductTemplateActivatedEvent;
import com.titanium.product.domain.event.ProductTemplateCreatedEvent;
import com.titanium.product.domain.event.ProductTemplateDeactivatedEvent;
import com.titanium.product.domain.event.ProductTemplateUpdatedEvent;
import com.titanium.product.domain.valueobject.BillingConfig;
import com.titanium.product.domain.valueobject.ClaimConfig;
import com.titanium.product.domain.valueobject.IssuanceMode;
import com.titanium.product.domain.valueobject.MaintenanceConfig;
import com.titanium.product.domain.valueobject.PolicyStage;
import com.titanium.product.domain.valueobject.PolicyStructureConfig;
import com.titanium.product.domain.valueobject.ReinsuranceConfig;
import com.titanium.product.domain.valueobject.UnderwritingConfig;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 产品模板聚合根 定义产品的行为配置，驱动各业务域的差异化行为 包含：出单模式、核保策略、保单结构、保全配置、理赔配置、缴费配置、再保配置
 */
@Getter
@Builder(builderMethodName = "builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Aggregate
public class ProductTemplate {

    @AggregateIdentifier
    private String                templateId;

    /** 模板编码，如 "TPL_CAR_COMPREHENSIVE" */
    private String                templateCode;

    /** 模板名称 */
    private String                templateName;

    /** 险种大类: LIFE / PROPERTY / HEALTH / ACCIDENT */
    private String                insuranceCategory;

    /** 险种类型（引用已有的 InsuranceType 枚举） */
    private InsuranceType         insuranceType;

    /** 关联的产品ID（InsuranceProduct） */
    private String                productId;

    /** 出单模式 */
    private IssuanceMode          issuanceMode;

    /** 出单阶段定义（有序列表） */
    private List<PolicyStage>     policyStages;

    /** 核保配置 */
    private UnderwritingConfig    underwritingConfig;

    /** 保单结构配置 */
    private PolicyStructureConfig policyStructure;

    /** 保全配置 */
    private MaintenanceConfig     maintenanceConfig;

    /** 理赔配置 */
    private ClaimConfig           claimConfig;

    /** 缴费配置 */
    private BillingConfig         billingConfig;

    /** 再保险配置 */
    private ReinsuranceConfig     reinsuranceConfig;

    /** 模板状态：DRAFT / ACTIVE / INACTIVE */
    private String                status;

    /** 租户ID */
    private String                tenantId;

    /** Axon 反射必备无参构造 */
    public ProductTemplate() {
    }

    // ==================== 命令处理 ====================

    @CommandHandler
    public ProductTemplate(CreateProductTemplateCommand command) {
        validateCreateCommand(command);

        AggregateLifecycle.apply(new ProductTemplateCreatedEvent(command.templateId(), command.templateCode(),
                command.templateName(), command.insuranceCategory(), command.insuranceType(), command.productId(),
                command.issuanceMode(), command.policyStages(), command.underwritingConfig(), command.policyStructure(),
                command.maintenanceConfig(), command.claimConfig(), command.billingConfig(),
                command.reinsuranceConfig(), "DRAFT", command.tenantId()));
    }

    @CommandHandler
    public void handle(UpdateProductTemplateCommand command) {
        if (!"DRAFT".equals(this.status)) {
            throw new IllegalStateException("仅草稿状态的产品模板可以修改");
        }
        AggregateLifecycle.apply(new ProductTemplateUpdatedEvent(command.templateId(), command.templateName(),
                command.issuanceMode(), command.policyStages(), command.underwritingConfig(), command.policyStructure(),
                command.maintenanceConfig(), command.claimConfig(), command.billingConfig(),
                command.reinsuranceConfig(), command.tenantId()));
    }

    @CommandHandler
    public void handle(ActivateProductTemplateCommand command) {
        if ("ACTIVE".equals(this.status)) {
            throw new IllegalStateException("产品模板已处于激活状态");
        }
        if (!"DRAFT".equals(this.status) && !"INACTIVE".equals(this.status)) {
            throw new IllegalStateException("仅草稿或停用状态的产品模板可激活");
        }
        AggregateLifecycle.apply(new ProductTemplateActivatedEvent(command.templateId(), command.tenantId()));
    }

    @CommandHandler
    public void handle(DeactivateProductTemplateCommand command) {
        if (!"ACTIVE".equals(this.status)) {
            throw new IllegalStateException("仅激活状态的产品模板可以停用");
        }
        AggregateLifecycle.apply(new ProductTemplateDeactivatedEvent(command.templateId(), command.tenantId()));
    }

    // ==================== 事件溯源 ====================

    @EventSourcingHandler
    public void on(ProductTemplateCreatedEvent event) {
        this.templateId = event.templateId();
        this.templateCode = event.templateCode();
        this.templateName = event.templateName();
        this.insuranceCategory = event.insuranceCategory();
        this.insuranceType = event.insuranceType();
        this.productId = event.productId();
        this.issuanceMode = event.issuanceMode();
        this.policyStages = event.policyStages();
        this.underwritingConfig = event.underwritingConfig();
        this.policyStructure = event.policyStructure();
        this.maintenanceConfig = event.maintenanceConfig();
        this.claimConfig = event.claimConfig();
        this.billingConfig = event.billingConfig();
        this.reinsuranceConfig = event.reinsuranceConfig();
        this.status = event.status();
        this.tenantId = event.tenantId();
    }

    @EventSourcingHandler
    public void on(ProductTemplateUpdatedEvent event) {
        this.templateName = event.templateName();
        this.issuanceMode = event.issuanceMode();
        this.policyStages = event.policyStages();
        this.underwritingConfig = event.underwritingConfig();
        this.policyStructure = event.policyStructure();
        this.maintenanceConfig = event.maintenanceConfig();
        this.claimConfig = event.claimConfig();
        this.billingConfig = event.billingConfig();
        this.reinsuranceConfig = event.reinsuranceConfig();
    }

    @EventSourcingHandler
    public void on(ProductTemplateActivatedEvent event) {
        this.status = "ACTIVE";
    }

    @EventSourcingHandler
    public void on(ProductTemplateDeactivatedEvent event) {
        this.status = "INACTIVE";
    }

    // ==================== 私有校验 ====================

    private void validateCreateCommand(CreateProductTemplateCommand command) {
        if (command.templateId() == null || command.templateCode() == null) {
            throw new IllegalArgumentException("模板ID和编码不能为空");
        }
        if (command.templateName() == null) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
        if (command.insuranceType() == null) {
            throw new IllegalArgumentException("险种类型不能为空");
        }
        if (command.issuanceMode() == null) {
            throw new IllegalArgumentException("出单模式不能为空");
        }
        if (command.policyStages() == null || command.policyStages().isEmpty()) {
            throw new IllegalArgumentException("出单阶段定义不能为空");
        }
    }
}
