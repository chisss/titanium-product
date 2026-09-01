package com.titanium.product.event;

import java.util.List;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.valueobject.config.CoveragePeriodConfig;
import com.titanium.product.valueobject.config.InsureCondition;
import com.titanium.product.valueobject.config.IssuanceProcessConfig;
import com.titanium.product.valueobject.config.PaymentConfig;
import com.titanium.product.valueobject.config.PolicyFormConfig;
import com.titanium.product.valueobject.config.SalesChannelConfig;
import com.titanium.product.valueobject.config.UnderwritingConfig;
import com.titanium.product.valueobject.pricing.pricing.ActuarialBasis;
import com.titanium.product.valueobject.pricing.pricing.PricingBasicRule;
import com.titanium.product.valueobject.rate.RateTableRef;

/**
 * 产品修订事件 当产品被修订并生成新版本时发布
 */
public record ProductRevisedEvent(String newProductId, String templateId, String originalProductId, String newVersion,
                                  String productCode, String newProductName, String newProductDesc,
                                  ProductEnum.ProductForm newForm,
                                  InsuranceProductType newInsuranceType, ProductEnum.ProductCategory newCategory,
                                  InsureCondition newInsureCondition, CoveragePeriodConfig newCoveragePeriod,
                                  PaymentConfig newPaymentConfig, List<ProductClauseRel> newClauseRels,
                                  PricingBasicRule newPricingBasicRule, List<SalesChannelConfig> newSalesChannels,
                                  IssuanceProcessConfig newIssuanceProcessConfig, PolicyFormConfig newPolicyFormConfig,
                                  UnderwritingConfig newUnderwritingConfig, List<String> attachProductIds,
                                  PricingMode newPricingMode, RateTableRef newRateTableRef,
                                  ActuarialBasis newActuarialBasis, String tenantId) {
}
