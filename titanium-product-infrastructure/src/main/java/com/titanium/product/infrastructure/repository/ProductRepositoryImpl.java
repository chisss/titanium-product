package com.titanium.product.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.aggregate.InsuranceProduct;
import com.titanium.product.domain.repository.ProductRepository;
import com.titanium.product.domain.valueobject.AuditInfo;
import com.titanium.product.infrastructure.entity.ProductClauseRelEntity;
import com.titanium.product.infrastructure.entity.ProductEntity;
import com.titanium.product.infrastructure.mapper.ProductInfraMapper;
import com.titanium.product.infrastructure.repository.jpa.ProductClauseRelJpaRepository;
import com.titanium.product.infrastructure.repository.jpa.ProductJpaRepository;

/**
 * 产品仓储实现类
 * 使用JPA实现产品领域数据访问，聚合根↔持久化实体之间的转换由手动方法完成
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;
    private final ProductClauseRelJpaRepository productClauseRelJpaRepository;

    public ProductRepositoryImpl(ProductJpaRepository productJpaRepository,
                                 ProductClauseRelJpaRepository productClauseRelJpaRepository) {
        this.productJpaRepository = productJpaRepository;
        this.productClauseRelJpaRepository = productClauseRelJpaRepository;
    }

    @Override
    public InsuranceProduct findById(String productId) {
        return productJpaRepository.findById(productId)
                .map(this::toInsuranceProduct)
                .orElse(null);
    }

    @Override
    public List<InsuranceProduct> findByCondition(ProductEnum.ProductForm form, InsuranceType type,
                                                   ProductEnum.ProductStatus status) {
        String formStr = form != null ? form.name() : null;
        // 使用第一页、大量数据的简单查询
        Page<ProductEntity> page = productJpaRepository.findByCondition(
                formStr, type, status, "default", PageRequest.of(0, 1000));
        return page.getContent().stream()
                .map(this::toInsuranceProduct)
                .collect(Collectors.toList());
    }

    @Override
    public List<InsuranceProduct> findHistoryByOriginalId(String originalProductId) {
        return productJpaRepository.findByOriginalProductId(originalProductId).stream()
                .map(this::toInsuranceProduct)
                .collect(Collectors.toList());
    }

    @Override
    public void save(InsuranceProduct product) {
        ProductEntity entity = toProductEntity(product);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy("system");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy("system");
        productJpaRepository.save(entity);

        // 保存产品条款关联
        if (product.getClauseRels() != null) {
            List<ProductClauseRelEntity> clauseRelDOs = ProductInfraMapper.INSTANCE
                    .toProductClauseRelEntitys(product.getClauseRels());
            clauseRelDOs.forEach(clauseRelDO -> {
                clauseRelDO.setProductId(product.getProductId());
                clauseRelDO.setTenantId(product.getTenantId() != null ? product.getTenantId() : "default");
                clauseRelDO.setCreatedAt(LocalDateTime.now());
                clauseRelDO.setCreatedBy("system");
            });
            productClauseRelJpaRepository.saveAll(clauseRelDOs);
        }
    }

    // ==================== 手动转换方法 ====================

    private InsuranceProduct toInsuranceProduct(ProductEntity entity) {
        ProductInfraMapper mapper = ProductInfraMapper.INSTANCE;
        return InsuranceProduct.builder()
                .productId(entity.getProductId())
                .productCode(entity.getProductCode())
                .productName(entity.getProductName())
                .productDesc(entity.getProductDesc())
                .form(entity.getForm())
                .insuranceType(entity.getInsuranceType())
                .category(entity.getCategory())
                .version(entity.getVersion())
                .status(entity.getStatus())
                .originalProductId(entity.getOriginalProductId())
                .effectiveTime(entity.getEffectiveTime())
                .invalidTime(entity.getInvalidTime())
                .saleStartTime(entity.getSaleStartTime())
                .saleEndTime(entity.getSaleEndTime())
                .insureCondition(mapper.jsonToInsureCondition(entity.getInsureCondition()))
                .coveragePeriod(mapper.jsonToCoveragePeriodConfig(entity.getCoveragePeriod()))
                .paymentConfig(mapper.jsonToPaymentConfig(entity.getPaymentConfig()))
                .pricingBasicRule(mapper.jsonToPricingBasicRule(entity.getPricingBasicRule()))
                .issuanceProcessConfig(mapper.jsonToIssuanceProcessConfig(entity.getIssuanceProcessConfig()))
                .policyFormConfig(mapper.jsonToPolicyFormConfig(entity.getPolicyFormConfig()))
                .underwritingConfig(mapper.jsonToUnderwritingConfig(entity.getUnderwritingConfig()))
                .salesChannels(mapper.jsonToSalesChannels(entity.getSalesChannels()))
                .attachProductIds(mapper.jsonToAttachProductIds(entity.getAttachProductIds()))
                .auditInfo(entity.getAuditResult() != null ? new AuditInfo(
                        entity.getAuditorId(), entity.getAuditorName(),
                        entity.getAuditOpinion(), entity.getAuditTime(),
                        entity.getAuditResult()
                ) : null)
                .tenantId(entity.getTenantId())
                .build();
    }

    private ProductEntity toProductEntity(InsuranceProduct product) {
        ProductInfraMapper mapper = ProductInfraMapper.INSTANCE;
        ProductEntity entity = new ProductEntity();
        entity.setProductId(product.getProductId());
        entity.setProductCode(product.getProductCode());
        entity.setProductName(product.getProductName());
        entity.setProductDesc(product.getProductDesc());
        entity.setForm(product.getForm());
        entity.setInsuranceType(product.getInsuranceType());
        entity.setCategory(product.getCategory());
        entity.setVersion(product.getVersion());
        entity.setStatus(product.getStatus());
        entity.setOriginalProductId(product.getOriginalProductId());
        entity.setEffectiveTime(product.getEffectiveTime());
        entity.setInvalidTime(product.getInvalidTime());
        entity.setSaleStartTime(product.getSaleStartTime());
        entity.setSaleEndTime(product.getSaleEndTime());
        entity.setInsureCondition(mapper.insureConditionToJson(product.getInsureCondition()));
        entity.setCoveragePeriod(mapper.coveragePeriodConfigToJson(product.getCoveragePeriod()));
        entity.setPaymentConfig(mapper.paymentConfigToJson(product.getPaymentConfig()));
        entity.setPricingBasicRule(mapper.pricingBasicRuleToJson(product.getPricingBasicRule()));
        entity.setIssuanceProcessConfig(mapper.issuanceProcessConfigToJson(product.getIssuanceProcessConfig()));
        entity.setPolicyFormConfig(mapper.policyFormConfigToJson(product.getPolicyFormConfig()));
        entity.setUnderwritingConfig(mapper.underwritingConfigToJson(product.getUnderwritingConfig()));
        entity.setSalesChannels(mapper.salesChannelsToJson(product.getSalesChannels()));
        entity.setAttachProductIds(mapper.attachProductIdsToJson(product.getAttachProductIds()));
        entity.setTenantId(product.getTenantId() != null ? product.getTenantId() : "default");

        if (product.getAuditInfo() != null) {
            entity.setAuditorId(product.getAuditInfo().auditorId());
            entity.setAuditorName(product.getAuditInfo().auditorName());
            entity.setAuditOpinion(product.getAuditInfo().auditOpinion());
            entity.setAuditTime(product.getAuditInfo().auditTime());
            entity.setAuditResult(product.getAuditInfo().auditResult());
        }
        return entity;
    }
}
