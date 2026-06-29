package com.titanium.product.domain.event;

import java.util.List;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.entity.ProductClauseRel;
import com.titanium.product.domain.valueobject.CoveragePeriodConfig;
import com.titanium.product.domain.valueobject.InsureCondition;
import com.titanium.product.domain.valueobject.IssuanceProcessConfig;
import com.titanium.product.domain.valueobject.PaymentConfig;
import com.titanium.product.domain.valueobject.PolicyFormConfig;
import com.titanium.product.domain.valueobject.PricingBasicRule;
import com.titanium.product.domain.valueobject.SalesChannelConfig;
import com.titanium.product.domain.valueobject.UnderwritingConfig;

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
