package com.titanium.product.event;

import java.time.LocalDateTime;

import org.axonframework.serialization.Revision;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 产品下架事件 当产品被下架时发布
 */
@Revision("1")
public record ProductInvalidatedEvent(String productId, ProductEnum.ProductStatus status, LocalDateTime invalidTime,
                                      String tenantId) {
}
