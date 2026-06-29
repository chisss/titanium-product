package com.titanium.product.domain.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

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
 * 修订产品命令
 * 用于修订产品，生成新版本的产品（携带完整的新配置）
 */
public record ReviseProductCommand(
        @TargetAggregateIdentifier String productId,
        String newProductId,
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
