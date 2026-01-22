package com.titanium.product.domain.event;

import java.util.List;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.entity.ProductClauseRel;
import com.titanium.product.domain.valueobject.InsureCondition;
import com.titanium.product.domain.valueobject.PricingBasicRule;

/**
 * 产品修订事件 当产品被修订并生成新版本时发布
 */
public record ProductRevisedEvent(String newProductId, String originalProductId, String newVersion,
                                  String newProductName, ProductEnum.ProductForm newForm,
                                  InsuranceType newInsuranceType, InsureCondition newInsureCondition,
                                  List<ProductClauseRel> newClauseRels, PricingBasicRule newPricingBasicRule) {
}
