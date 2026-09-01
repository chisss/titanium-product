package com.titanium.product.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
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
 * 创建产品命令 用于创建新的保险产品，包含完整的产品配置信息
 */
public record CreateProductCommand(@TargetAggregateIdentifier String productId, String templateId, String productCode,
                                   String productName, String productDesc, ProductEnum.ProductForm form,
                                   InsuranceProductType insuranceType, ProductEnum.ProductCategory category,
                                   LocalDateTime effectiveTime,
                                   LocalDateTime saleStartTime, LocalDateTime saleEndTime,
                                   InsureCondition insureCondition, CoveragePeriodConfig coveragePeriod,
                                   PaymentConfig paymentConfig, PricingBasicRule pricingBasicRule,
                                   List<String> clauseIds, Map<String, String> clauseVersionMap, String mainClauseId,
                                   List<SalesChannelConfig> salesChannels, List<String> attachProductIds,
                                   IssuanceProcessConfig issuanceProcessConfig, PolicyFormConfig policyFormConfig,
                                   UnderwritingConfig underwritingConfig, String tenantId,
                                   PricingMode pricingMode, RateTableRef rateTableRef,
                                   ActuarialBasis actuarialBasis, DocumentConfig documentConfig, String createdBy) {
}
