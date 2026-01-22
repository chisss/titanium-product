package com.titanium.product.infrastructure.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 产品数据库实体 映射产品表，包含产品的基本信息、状态、版本等
 */
@Entity
@Table(name = "t_product")
@Getter
@Setter
public class ProductDO {
    @Id
    @Column(name = "product_id", length = 36, nullable = false)
    private String        productId;

    @Column(name = "product_name", length = 100, nullable = false)
    private String        productName;

    @Column(name = "form", length = 20, nullable = false)
    private String        form;

    @Column(name = "insurance_type", length = 20, nullable = false)
    private String        insuranceType;

    @Column(name = "version", length = 10, nullable = false)
    private String        version;

    @Column(name = "status", length = 20, nullable = false)
    private String        status;

    @Column(name = "effective_time")
    private LocalDateTime effectiveTime;

    @Column(name = "invalid_time")
    private LocalDateTime invalidTime;

    @Column(name = "original_product_id", length = 36)
    private String        originalProductId;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String        tenantId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50, nullable = false)
    private String        createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50, nullable = false)
    private String        updatedBy;
}
