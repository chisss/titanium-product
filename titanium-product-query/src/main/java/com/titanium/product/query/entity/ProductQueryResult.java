package com.titanium.product.query.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * 产品查询实体 用于封装产品查询结果
 */
@Getter
@Setter
public class ProductQueryResult {
    private String        productId;
    private String        productName;
    private String        form;
    private String        insuranceType;
    private String        version;
    private String        status;
    private LocalDateTime effectiveTime;
    private LocalDateTime invalidTime;
    private LocalDateTime createdAt;
    private String        createdBy;
    private LocalDateTime updatedAt;
    private String        updatedBy;
    private String        tenantId;
}
