package com.titanium.product.event;

import java.util.List;

import com.titanium.metadata.enums.insurance.InsuranceMandateType;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.valueobject.config.CoveragePeriodConfig;
import com.titanium.product.valueobject.config.CoverageSumInsuredConfig;
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
 * <p>
 * 🔴 <b>分量只允许在尾部追加，禁止在中间插入或删除</b>：事件存储中已有存量载荷，
 * 授权序列化按<b>属性名</b>绑定，追加尾部分量时存量载荷读到 {@code null}（由
 * {@code InsuranceProduct#on(ProductRevisedEvent)} 兜底为承接原版本的空列表），
 * 而在中间插入会让同名分量错位、语义整体偏移。
 * </p>
 * <p>
 * 尾部分量 {@code insuranceMandateType}（G11/g11-04 追加）不带 {@code new} 前缀，与
 * {@code templateId}/{@code productCode}/{@code attachProductIds} 同族——是<b>继承值</b>而非本次修订的变更项。
 * </p>
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
                                  ActuarialBasis newActuarialBasis, String tenantId,
                                  List<CoverageSumInsuredConfig> newCoverageSumInsuredConfigs,
                                  InsuranceMandateType insuranceMandateType) {
}
