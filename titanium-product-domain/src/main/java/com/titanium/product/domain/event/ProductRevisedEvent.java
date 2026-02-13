package com.titanium.product.domain.event;

import java.util.List;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.entity.ProductClauseRel;
import com.titanium.product.domain.valueobject.*;

/**
 * 产品修订事件
 * 当产品被修订并生成新版本时发布
 */
public record ProductRevisedEvent(
        String newProductId,
        String originalProductId,
        String newVersion,
        String newProductName,
        String newProductDesc,
        ProductEnum.ProductForm newForm,
        InsuranceType newInsuranceType,
        ProductEnum.ProductCategory newCategory,
        InsureCondition newInsureCondition,
        CoveragePeriodConfig newCoveragePeriod,
        PaymentConfig newPaymentConfig,
        List<ProductClauseRel> newClauseRels,
        PricingBasicRule newPricingBasicRule,
        List<SalesChannelConfig> newSalesChannels,
        IssuanceProcessConfig newIssuanceProcessConfig,
        PolicyFormConfig newPolicyFormConfig,
        UnderwritingConfig newUnderwritingConfig
) {
}
