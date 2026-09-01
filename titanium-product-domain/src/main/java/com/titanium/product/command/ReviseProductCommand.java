package com.titanium.product.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

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
 * 修订产品命令 用于修订产品，生成新版本的产品（携带完整的新配置）
 */
public record ReviseProductCommand(@TargetAggregateIdentifier String productId, String newProductId,
                                   String newProductName, String newProductDesc, ProductEnum.ProductForm newForm,
                                   InsuranceProductType newInsuranceType, ProductEnum.ProductCategory newCategory,
                                   InsureCondition newInsureCondition, CoveragePeriodConfig newCoveragePeriod,
                                   PaymentConfig newPaymentConfig, List<ProductClauseRel> newClauseRels,
                                   PricingBasicRule newPricingBasicRule, List<SalesChannelConfig> newSalesChannels,
                                   IssuanceProcessConfig newIssuanceProcessConfig, PolicyFormConfig newPolicyFormConfig,
                                   UnderwritingConfig newUnderwritingConfig, PricingMode newPricingMode,
                                   RateTableRef newRateTableRef, ActuarialBasis newActuarialBasis) {
}
