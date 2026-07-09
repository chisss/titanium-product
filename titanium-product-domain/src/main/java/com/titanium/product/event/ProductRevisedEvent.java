package com.titanium.product.event;

import java.util.List;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.valueobject.CoveragePeriodConfig;
import com.titanium.product.valueobject.InsureCondition;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.PaymentConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.valueobject.SalesChannelConfig;
import com.titanium.product.valueobject.UnderwritingConfig;

/**
 * 产品修订事件 当产品被修订并生成新版本时发布
 */
public record ProductRevisedEvent(String newProductId, String originalProductId, String newVersion,
                                  String newProductName, String newProductDesc, ProductEnum.ProductForm newForm,
                                  InsuranceType newInsuranceType, ProductEnum.ProductCategory newCategory,
                                  InsureCondition newInsureCondition, CoveragePeriodConfig newCoveragePeriod,
                                  PaymentConfig newPaymentConfig, List<ProductClauseRel> newClauseRels,
                                  PricingBasicRule newPricingBasicRule, List<SalesChannelConfig> newSalesChannels,
                                  IssuanceProcessConfig newIssuanceProcessConfig, PolicyFormConfig newPolicyFormConfig,
                                  UnderwritingConfig newUnderwritingConfig) {
}
