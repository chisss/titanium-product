package com.titanium.product.infrastructure.projection;

import java.time.LocalDateTime;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.titanium.product.domain.event.ProductTemplateActivatedEvent;
import com.titanium.product.domain.event.ProductTemplateCreatedEvent;
import com.titanium.product.domain.event.ProductTemplateDeactivatedEvent;
import com.titanium.product.domain.event.ProductTemplateUpdatedEvent;
import com.titanium.product.infrastructure.entity.ProductTemplateDO;
import com.titanium.product.infrastructure.repository.jpa.ProductTemplateJpaRepository;

/**
 * 产品模板投影处理器
 * 将领域事件持久化到数据库
 */
@Component
public class ProductTemplateProjection {

    @Autowired
    private ProductTemplateJpaRepository jpaRepository;

    @EventHandler
    public void on(ProductTemplateCreatedEvent event) {
        ProductTemplateDO entity = new ProductTemplateDO();
        entity.setTemplateId(event.templateId());
        entity.setTemplateCode(event.templateCode());
        entity.setTemplateName(event.templateName());
        entity.setInsuranceCategory(event.insuranceCategory());
        entity.setInsuranceType(event.insuranceType().getCode());
        entity.setProductId(event.productId());
        entity.setIssuanceMode(event.issuanceMode().getCode());
        entity.setPolicyStagesJson(JSON.toJSONString(event.policyStages()));
        entity.setUnderwritingConfigJson(JSON.toJSONString(event.underwritingConfig()));
        entity.setPolicyStructureJson(JSON.toJSONString(event.policyStructure()));
        entity.setMaintenanceConfigJson(JSON.toJSONString(event.maintenanceConfig()));
        entity.setClaimConfigJson(JSON.toJSONString(event.claimConfig()));
        entity.setBillingConfigJson(JSON.toJSONString(event.billingConfig()));
        entity.setReinsuranceConfigJson(event.reinsuranceConfig() != null
                ? JSON.toJSONString(event.reinsuranceConfig()) : null);
        entity.setStatus(event.status());
        entity.setTenantId(event.tenantId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        jpaRepository.save(entity);
    }

    @EventHandler
    public void on(ProductTemplateUpdatedEvent event) {
        jpaRepository.findById(event.templateId()).ifPresent(entity -> {
            entity.setTemplateName(event.templateName());
            entity.setIssuanceMode(event.issuanceMode().getCode());
            entity.setPolicyStagesJson(JSON.toJSONString(event.policyStages()));
            entity.setUnderwritingConfigJson(JSON.toJSONString(event.underwritingConfig()));
            entity.setPolicyStructureJson(JSON.toJSONString(event.policyStructure()));
            entity.setMaintenanceConfigJson(JSON.toJSONString(event.maintenanceConfig()));
            entity.setClaimConfigJson(JSON.toJSONString(event.claimConfig()));
            entity.setBillingConfigJson(JSON.toJSONString(event.billingConfig()));
            entity.setReinsuranceConfigJson(event.reinsuranceConfig() != null
                    ? JSON.toJSONString(event.reinsuranceConfig()) : null);
            entity.setUpdatedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }

    @EventHandler
    public void on(ProductTemplateActivatedEvent event) {
        jpaRepository.findById(event.templateId()).ifPresent(entity -> {
            entity.setStatus("ACTIVE");
            entity.setUpdatedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }

    @EventHandler
    public void on(ProductTemplateDeactivatedEvent event) {
        jpaRepository.findById(event.templateId()).ifPresent(entity -> {
            entity.setStatus("INACTIVE");
            entity.setUpdatedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }
}
