package com.titanium.product.domain.aggregate;

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
import com.titanium.metadata.exception.CommandValidationException;
import com.titanium.product.domain.command.CreateProductTemplateCommand;
import com.titanium.product.domain.event.ProductTemplateCreatedEvent;
import com.titanium.product.domain.valueobject.ClaimConfig;
import com.titanium.product.domain.valueobject.IssuanceProcessConfig;
import com.titanium.product.domain.valueobject.MaintenanceConfig;
import com.titanium.product.domain.valueobject.PolicyFormConfig;
import com.titanium.product.domain.valueobject.PricingBasicRule;
import com.titanium.product.domain.valueobject.UnderwritingConfig;

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
    private CommonStatus          status;

    public ProductTemplate() {
    }

    /**
     * 从持久化状态重建模板聚合（仓储读取用，仅设置真实字段）
     */
    public static ProductTemplate reconstruct(String templateId, String templateCode, String templateName,
                                              InsuranceType insuranceType, IssuanceProcessConfig issuanceProcessConfig,
                                              UnderwritingConfig underwritingConfig, ClaimConfig claimsConfig,
                                              MaintenanceConfig maintenanceConfig, PolicyFormConfig policyFormConfig,
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
}
