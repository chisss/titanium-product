package com.titanium.product.domain.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.valueobject.*;

/**
 * 创建产品命令
 * 用于创建新的保险产品，包含完整的产品配置信息
 */
public record CreateProductCommand(
        @TargetAggregateIdentifier String productId,
        String productCode,
        String productName,
        String productDesc,
        ProductEnum.ProductForm form,
        InsuranceType insuranceType,
        ProductEnum.ProductCategory category,
        LocalDateTime effectiveTime,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        InsureCondition insureCondition,
        CoveragePeriodConfig coveragePeriod,
        PaymentConfig paymentConfig,
        PricingBasicRule pricingBasicRule,
        List<String> clauseIds,
        Map<String, String> clauseVersionMap,
        String mainClauseId,
        List<SalesChannelConfig> salesChannels,
        List<String> attachProductIds,
        IssuanceProcessConfig issuanceProcessConfig,
        PolicyFormConfig policyFormConfig,
        UnderwritingConfig underwritingConfig,
        String tenantId
) {
}
