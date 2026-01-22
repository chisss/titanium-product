package com.titanium.product.domain.event;

import java.time.LocalDateTime;
import java.util.List;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.entity.ProductClauseRel;
import com.titanium.product.domain.valueobject.InsureCondition;
import com.titanium.product.domain.valueobject.PricingBasicRule;

/**
 * 产品创建事件 当新的保险产品被创建时发布
 */
public record ProductCreatedEvent(String productId, String productName, ProductEnum.ProductForm form,
                                  InsuranceType insuranceType, String version, ProductEnum.ProductStatus status,
                                  LocalDateTime createdAt, InsureCondition insureCondition,
                                  List<ProductClauseRel> clauseRels, PricingBasicRule pricingBasicRule) {
}
