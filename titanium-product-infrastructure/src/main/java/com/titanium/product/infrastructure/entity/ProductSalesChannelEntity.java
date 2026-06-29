package com.titanium.product.infrastructure.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.titanium.metadata.enums.product.ProductEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 产品销售渠道数据库实体
 * 映射产品销售渠道配置表
 */
@Entity
@Table(name = "t_product_sales_channel", indexes = {
        @Index(name = "idx_sales_channel_product_id", columnList = "product_id"),
        @Index(name = "idx_sales_channel_tenant_id", columnList = "tenant_id")
})
@Getter
@Setter
public class ProductSalesChannelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 30, nullable = false)
    private ProductEnum.SalesChannel channelType;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "commission_rate", precision = 10, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;
}
