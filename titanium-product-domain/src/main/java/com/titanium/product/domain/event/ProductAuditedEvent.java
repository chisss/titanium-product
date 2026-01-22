package com.titanium.product.domain.event;

import java.time.LocalDateTime;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 产品审核通过事件 当产品审核通过并生效时发布
 */
public record ProductAuditedEvent(String productId, ProductEnum.ProductStatus status, LocalDateTime effectiveTime) {
}
