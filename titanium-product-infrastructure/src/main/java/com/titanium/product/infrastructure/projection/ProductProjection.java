package com.titanium.product.infrastructure.projection;

import java.time.LocalDateTime;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.event.ProductAuditRejectedEvent;
import com.titanium.product.domain.event.ProductAuditedEvent;
import com.titanium.product.domain.event.ProductClauseRelUpdatedEvent;
import com.titanium.product.domain.event.ProductCreatedEvent;
import com.titanium.product.domain.event.ProductInvalidatedEvent;
import com.titanium.product.domain.event.ProductSalesChannelUpdatedEvent;
import com.titanium.product.domain.event.ProductSubmittedForAuditEvent;
import com.titanium.product.infrastructure.entity.ProductClauseRelEntity;
import com.titanium.product.infrastructure.entity.ProductEntity;
import com.titanium.product.infrastructure.mapper.ProductInfraMapper;
import com.titanium.product.infrastructure.repository.jpa.ProductClauseRelJpaRepository;
import com.titanium.product.infrastructure.repository.jpa.ProductJpaRepository;

/**
 * 产品投影处理器
 * 监听领域事件，同步更新读模型（ProductEntity）
 * 实现CQRS中的投影（Projection）职责
 */
@Component
public class ProductProjection {

    private final ProductJpaRepository productJpaRepository;
    private final ProductClauseRelJpaRepository productClauseRelJpaRepository;

    public ProductProjection(ProductJpaRepository productJpaRepository,
                             ProductClauseRelJpaRepository productClauseRelJpaRepository) {
        this.productJpaRepository = productJpaRepository;
        this.productClauseRelJpaRepository = productClauseRelJpaRepository;
    }

    @EventHandler
    public void on(ProductCreatedEvent event) {
        ProductInfraMapper mapper = ProductInfraMapper.INSTANCE;
        ProductEntity entity = new ProductEntity();
        entity.setProductId(event.productId());
        entity.setProductCode(event.productCode());
        entity.setProductName(event.productName());
        entity.setProductDesc(event.productDesc());
        entity.setForm(event.form());
        entity.setInsuranceType(event.insuranceType());
        entity.setCategory(event.category());
        entity.setVersion(event.version());
        entity.setStatus(event.status());
        entity.setSaleStartTime(event.saleStartTime());
        entity.setSaleEndTime(event.saleEndTime());
        entity.setInsureCondition(mapper.insureConditionToJson(event.insureCondition()));
        entity.setCoveragePeriod(mapper.coveragePeriodConfigToJson(event.coveragePeriod()));
        entity.setPaymentConfig(mapper.paymentConfigToJson(event.paymentConfig()));
        entity.setPricingBasicRule(mapper.pricingBasicRuleToJson(event.pricingBasicRule()));
        entity.setIssuanceProcessConfig(mapper.issuanceProcessConfigToJson(event.issuanceProcessConfig()));
        entity.setPolicyFormConfig(mapper.policyFormConfigToJson(event.policyFormConfig()));
        entity.setUnderwritingConfig(mapper.underwritingConfigToJson(event.underwritingConfig()));
        entity.setSalesChannels(mapper.salesChannelsToJson(event.salesChannels()));
        entity.setAttachProductIds(mapper.attachProductIdsToJson(event.attachProductIds()));
        entity.setTenantId(event.tenantId());
        entity.setCreateTime(event.createdAt());
        entity.setCreatedBy("system");
        entity.setUpdateTime(event.createdAt());
        entity.setUpdatedBy("system");
        productJpaRepository.save(entity);

        // 保存条款关联
        if (event.clauseRels() != null) {
            event.clauseRels().forEach(clauseRel -> {
                ProductClauseRelEntity relDO = new ProductClauseRelEntity();
                relDO.setProductId(event.productId());
                relDO.setClauseId(clauseRel.getClauseId());
                relDO.setClauseVersion(clauseRel.getClauseVersion());
                relDO.setIsMainClause(clauseRel.getIsMainClause());
                relDO.setTenantId(event.tenantId());
                relDO.setCreatedAt(LocalDateTime.now());
                relDO.setCreatedBy("system");
                productClauseRelJpaRepository.save(relDO);
            });
        }
    }

    @EventHandler
    public void on(ProductSubmittedForAuditEvent event) {
        productJpaRepository.findById(event.productId()).ifPresent(entity -> {
            entity.setStatus(ProductEnum.ProductStatus.AUDITING);
            entity.setUpdateTime(event.submittedAt());
            entity.setUpdatedBy(event.submitterName());
            productJpaRepository.save(entity);
        });
    }

    @EventHandler
    public void on(ProductAuditedEvent event) {
        productJpaRepository.findById(event.productId()).ifPresent(entity -> {
            entity.setStatus(event.status());
            entity.setEffectiveTime(event.effectiveTime());
            if (event.auditInfo() != null) {
                entity.setAuditorId(event.auditInfo().auditorId());
                entity.setAuditorName(event.auditInfo().auditorName());
                entity.setAuditOpinion(event.auditInfo().auditOpinion());
                entity.setAuditTime(event.auditInfo().auditTime());
                entity.setAuditResult(event.auditInfo().auditResult());
            }
            entity.setUpdateTime(LocalDateTime.now());
            entity.setUpdatedBy("system");
            productJpaRepository.save(entity);
        });
    }

    @EventHandler
    public void on(ProductAuditRejectedEvent event) {
        productJpaRepository.findById(event.productId()).ifPresent(entity -> {
            entity.setStatus(event.status());
            if (event.auditInfo() != null) {
                entity.setAuditorId(event.auditInfo().auditorId());
                entity.setAuditorName(event.auditInfo().auditorName());
                entity.setAuditOpinion(event.auditInfo().auditOpinion());
                entity.setAuditTime(event.auditInfo().auditTime());
                entity.setAuditResult(event.auditInfo().auditResult());
            }
            entity.setUpdateTime(event.rejectedAt());
            entity.setUpdatedBy("system");
            productJpaRepository.save(entity);
        });
    }

    @EventHandler
    public void on(ProductInvalidatedEvent event) {
        productJpaRepository.findById(event.productId()).ifPresent(entity -> {
            entity.setStatus(event.status());
            entity.setInvalidTime(event.invalidTime());
            entity.setUpdateTime(LocalDateTime.now());
            entity.setUpdatedBy("system");
            productJpaRepository.save(entity);
        });
    }

    @EventHandler
    public void on(ProductClauseRelUpdatedEvent event) {
        // 先删除旧的关联
        productClauseRelJpaRepository.deleteByProductId(event.productId());
        // 再保存新的关联
        if (event.clauseRels() != null) {
            event.clauseRels().forEach(clauseRel -> {
                ProductClauseRelEntity relDO = new ProductClauseRelEntity();
                relDO.setProductId(event.productId());
                relDO.setClauseId(clauseRel.getClauseId());
                relDO.setClauseVersion(clauseRel.getClauseVersion());
                relDO.setIsMainClause(clauseRel.getIsMainClause());
                relDO.setTenantId("default");
                relDO.setCreatedAt(LocalDateTime.now());
                relDO.setCreatedBy("system");
                productClauseRelJpaRepository.save(relDO);
            });
        }
    }

    @EventHandler
    public void on(ProductSalesChannelUpdatedEvent event) {
        ProductInfraMapper mapper = ProductInfraMapper.INSTANCE;
        productJpaRepository.findById(event.productId()).ifPresent(entity -> {
            entity.setSalesChannels(mapper.salesChannelsToJson(event.salesChannels()));
            entity.setUpdateTime(LocalDateTime.now());
            entity.setUpdatedBy("system");
            productJpaRepository.save(entity);
        });
    }
}
