package com.titanium.product.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.ActuarialBasis;
import com.titanium.product.valueobject.CoveragePeriodConfig;
import com.titanium.product.valueobject.DocumentConfig;
import com.titanium.product.valueobject.InsureCondition;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.PaymentConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.SalesChannelConfig;
import com.titanium.product.valueobject.UnderwritingConfig;

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
