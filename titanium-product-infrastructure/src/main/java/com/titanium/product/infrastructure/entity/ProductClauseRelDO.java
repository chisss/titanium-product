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
 * 产品-条款关联数据库实体 映射产品和条款的关联关系表
 */
@Entity
@Table(name = "t_product_clause_rel", indexes = {
        @Index(name = "t_product_product_id_index", columnList = "product_id"),
        @Index(name = "idx_product_insurance_type", columnList = "insurance_type"),
        @Index(name = "idx_product_original_id", columnList = "original_product_id"),
        @Index(name = "idx_product_status", columnList = "status"),
        @Index(name = "idx_product_tenant_id",columnList = "tenant_id"),
        @Index(name = "idx_product_type_status_index",columnList = "insurance_type,status")})
@Getter
@Setter
public class ProductClauseRelDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long          id;

    @Column(name = "product_id", length = 36, nullable = false)
    private String        productId;

    @Column(name = "clause_id", length = 36, nullable = false)
    private String        clauseId;

    @Column(name = "clause_version", length = 10, nullable = false)
    private String        clauseVersion;

    @Column(name = "is_main_clause", nullable = false)
    private Boolean       isMainClause;

    @Column(name = "bind_time", nullable = false)
    private LocalDateTime bindTime;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String        tenantId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50, nullable = false)
    private String        createdBy;
}
