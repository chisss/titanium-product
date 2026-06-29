package com.titanium.product.infrastructure.projection;


import java.time.LocalDateTime;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.product.domain.event.ProductTemplateActivatedEvent;
import com.titanium.product.domain.event.ProductTemplateCreatedEvent;
import com.titanium.product.domain.event.ProductTemplateDeactivatedEvent;
import com.titanium.product.domain.event.ProductTemplateUpdatedEvent;
import com.titanium.product.infrastructure.entity.ProductTemplateEntity;
import com.titanium.product.infrastructure.repository.jpa.ProductTemplateJpaRepository;

import lombok.RequiredArgsConstructor;

/**
 * 产品模板投影处理器
 * 将领域事件持久化到数据库
 */
@Component
@RequiredArgsConstructor
public class ProductTemplateProjection {

    private final ProductTemplateJpaRepository jpaRepository;

    @EventHandler
    public void on(ProductTemplateCreatedEvent event) {
        ProductTemplateEntity entity = new ProductTemplateEntity();
        entity.setTemplateId(event.templateId());
        entity.setTemplateCode(event.templateCode());
        entity.setTemplateName(event.templateName());
        entity.setInsuranceType(event.insuranceType());
        // 出单流程配置整体序列化存入 issuance_mode 列（创建事件携带的是 IssuanceProcessConfig）
        entity.setIssuanceMode(event.issuanceProcessConfig() != null
                ? JSON.toJSONString(event.issuanceProcessConfig()) : "DEFAULT");
        entity.setUnderwritingConfigJson(JSON.toJSONString(event.underwritingConfig()));
        entity.setMaintenanceConfigJson(JSON.toJSONString(event.maintenanceConfig()));
        entity.setClaimConfigJson(JSON.toJSONString(event.claimsConfig()));
        // 保单形态/定价规则序列化存入 policy_structure 列
        entity.setPolicyStructureJson(event.policyFormConfig() != null
                ? JSON.toJSONString(event.policyFormConfig()) : null);
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
            entity.setIssuanceMode(event.issuanceMode().name());
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
            entity.setStatus(CommonStatus.ACTIVE);
            entity.setUpdatedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }

    @EventHandler
    public void on(ProductTemplateDeactivatedEvent event) {
        jpaRepository.findById(event.templateId()).ifPresent(entity -> {
            entity.setStatus(CommonStatus.INACTIVE);
            entity.setUpdatedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }
}
