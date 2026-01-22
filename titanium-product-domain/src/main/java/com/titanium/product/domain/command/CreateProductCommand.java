package com.titanium.product.domain.command;

import java.util.List;
import java.util.Map;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.valueobject.InsureCondition;
import com.titanium.product.domain.valueobject.PricingBasicRule;

/**
 * 创建产品命令 用于创建新的保险产品
 */
public record CreateProductCommand(@TargetAggregateIdentifier String productId, String productName,
                                   ProductEnum.ProductForm form, InsuranceType insuranceType,
                                   InsureCondition insureCondition, List<String> clauseIds,
                                   Map<String, String> clauseVersionMap, String mainClauseId,
                                   PricingBasicRule pricingBasicRule) {
}
