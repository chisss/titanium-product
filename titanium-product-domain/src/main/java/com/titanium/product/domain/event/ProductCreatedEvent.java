package com.titanium.product.domain.event;

import java.time.LocalDateTime;
import java.util.List;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.entity.ProductClauseRel;
import com.titanium.product.domain.valueobject.*;

/**
 * 产品创建事件
 * 当新的保险产品被创建时发布
 */
public record ProductCreatedEvent(
        String productId,
        String productCode,
        String productName,
        String productDesc,
        ProductEnum.ProductForm form,
        InsuranceType insuranceType,
        ProductEnum.ProductCategory category,
        String version,
        ProductEnum.ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        InsureCondition insureCondition,
        CoveragePeriodConfig coveragePeriod,
        PaymentConfig paymentConfig,
        PricingBasicRule pricingBasicRule,
        List<ProductClauseRel> clauseRels,
        List<SalesChannelConfig> salesChannels,
        List<String> attachProductIds,
        IssuanceProcessConfig issuanceProcessConfig,
        PolicyFormConfig policyFormConfig,
        UnderwritingConfig underwritingConfig,
        String tenantId
) {
}
