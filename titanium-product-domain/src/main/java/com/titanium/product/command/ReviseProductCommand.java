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
 *
 * <p>🔴 {@code tenantId} 是 R8-01 补入的：修订携带的条款绑定必须经跨域校验（条款是否存在、是否 ACTIVE），
 * 而该调用发生在**命令到达聚合之前**的应用层，聚合内继承的 {@code this.tenantId} 那时还取不到。
 * 此前该字段缺失使修订路径完全绕开条款门禁——而它恰是 UI 上唯一可达的改绑定入口。</p>
 *
 * <p>🔴 {@code newClauseRels} 为空表示<b>继承原版本绑定</b>（与 {@code newSalesChannels} 等同规则），
 * 不是「清空」——绑定非空是硬不变量，清空从来不是合法意图。见
 * {@code InsuranceProduct#handle(ReviseProductCommand)}。</p>
 */
public record ReviseProductCommand(@TargetAggregateIdentifier String productId, String newProductId,
                                   String newProductName, String newProductDesc, ProductEnum.ProductForm newForm,
                                   InsuranceProductType newInsuranceType, ProductEnum.ProductCategory newCategory,
                                   InsureCondition newInsureCondition, CoveragePeriodConfig newCoveragePeriod,
                                   PaymentConfig newPaymentConfig, List<ProductClauseRel> newClauseRels,
                                   PricingBasicRule newPricingBasicRule, List<SalesChannelConfig> newSalesChannels,
                                   IssuanceProcessConfig newIssuanceProcessConfig, PolicyFormConfig newPolicyFormConfig,
                                   UnderwritingConfig newUnderwritingConfig, PricingMode newPricingMode,
                                   RateTableRef newRateTableRef, ActuarialBasis newActuarialBasis, String tenantId) {
}
