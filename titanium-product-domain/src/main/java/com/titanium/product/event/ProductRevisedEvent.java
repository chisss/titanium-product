package com.titanium.product.event;

import java.util.List;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.valueobject.ActuarialBasis;
import com.titanium.product.valueobject.CoveragePeriodConfig;
import com.titanium.product.valueobject.InsureCondition;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.PaymentConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.SalesChannelConfig;
import com.titanium.product.valueobject.UnderwritingConfig;

/**
 * 产品修订事件 当产品被修订并生成新版本时发布
 */
public record ProductRevisedEvent(String newProductId, String templateId, String originalProductId, String newVersion,
                                  String newProductName, String newProductDesc, ProductEnum.ProductForm newForm,
                                  InsuranceProductType newInsuranceType, ProductEnum.ProductCategory newCategory,
                                  InsureCondition newInsureCondition, CoveragePeriodConfig newCoveragePeriod,
                                  PaymentConfig newPaymentConfig, List<ProductClauseRel> newClauseRels,
                                  PricingBasicRule newPricingBasicRule, List<SalesChannelConfig> newSalesChannels,
                                  IssuanceProcessConfig newIssuanceProcessConfig, PolicyFormConfig newPolicyFormConfig,
                                  UnderwritingConfig newUnderwritingConfig, List<String> attachProductIds,
                                  PricingMode newPricingMode, RateTableRef newRateTableRef,
                                  ActuarialBasis newActuarialBasis) {
}
