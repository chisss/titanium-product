package com.titanium.product.domain.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.entity.ProductClauseRel;
import com.titanium.product.domain.valueobject.InsureCondition;
import com.titanium.product.domain.valueobject.PricingBasicRule;

/**
 * 修订产品命令 用于修订产品，生成新版本的产品
 */
public record ReviseProductCommand(@TargetAggregateIdentifier String productId, String newProductId,
                                   String newProductName, ProductEnum.ProductForm newForm,
                                   InsuranceType newInsuranceType, InsureCondition newInsureCondition,
                                   List<ProductClauseRel> newClauseRels, PricingBasicRule newPricingBasicRule) {
}
