package com.titanium.product.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

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
