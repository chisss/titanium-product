package com.titanium.product.event;

import java.time.LocalDateTime;
import java.util.List;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.valueobject.config.CoveragePeriodConfig;
import com.titanium.product.valueobject.config.DocumentConfig;
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
 * 产品创建事件 当新的保险产品被创建时发布
 */
public record ProductCreatedEvent(String productId, String templateId, String productCode, String productName,
                                  String productDesc, ProductEnum.ProductForm form, InsuranceProductType insuranceType,
                                  ProductEnum.ProductCategory category, String version,
                                  ProductEnum.ProductStatus status, LocalDateTime createdAt,
                                  LocalDateTime saleStartTime, LocalDateTime saleEndTime,
                                  InsureCondition insureCondition, CoveragePeriodConfig coveragePeriod,
                                  PaymentConfig paymentConfig, PricingBasicRule pricingBasicRule,
                                  List<ProductClauseRel> clauseRels, List<SalesChannelConfig> salesChannels,
                                  List<String> attachProductIds, IssuanceProcessConfig issuanceProcessConfig,
                                  PolicyFormConfig policyFormConfig, UnderwritingConfig underwritingConfig,
                                  String tenantId, PricingMode pricingMode, RateTableRef rateTableRef,
                                  ActuarialBasis actuarialBasis, DocumentConfig documentConfig, String createdBy) {
}
