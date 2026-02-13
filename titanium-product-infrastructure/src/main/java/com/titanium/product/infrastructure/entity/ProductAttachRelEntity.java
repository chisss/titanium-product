package com.titanium.product.infrastructure.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 产品附加险关联数据库实体
 * 映射主险-附加险关联关系表
 */
@Entity
@Table(name = "t_product_attach_rel", indexes = {
        @Index(name = "idx_attach_rel_main_product_id", columnList = "main_product_id"),
        @Index(name = "idx_attach_rel_tenant_id", columnList = "tenant_id")
})
@Getter
@Setter
public class ProductAttachRelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "main_product_id", length = 36, nullable = false)
    private String mainProductId;

    @Column(name = "attach_product_id", length = 36, nullable = false)
    private String attachProductId;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;
}
