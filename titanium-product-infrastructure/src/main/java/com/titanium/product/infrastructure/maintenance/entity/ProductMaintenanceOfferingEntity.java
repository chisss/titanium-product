package com.titanium.product.infrastructure.maintenance.entity;

import java.time.LocalDateTime;

import com.titanium.product.common.enums.ProductMaintenanceOfferingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Product 保全 Offering 持久化实体。 */
@Entity
@Table(name = "t_product_maintenance_offering")
@Getter
@Setter
public class ProductMaintenanceOfferingEntity {

    @Id
    @Column(name = "offering_id", nullable = false, length = 36)
    private String offeringId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "product_version", nullable = false, length = 64)
    private String productVersion;

    @Column(name = "plan_version", nullable = false, length = 64)
    private String planVersion;

    @Column(name = "offering_version", nullable = false, length = 64)
    private String offeringVersion;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProductMaintenanceOfferingStatus status;

    @Column(name = "allowed_policy_statuses_json", nullable = false, columnDefinition = "LONGTEXT")
    private String allowedPolicyStatusesJson;

    @Column(name = "allowed_channels_json", nullable = false, columnDefinition = "LONGTEXT")
    private String allowedChannelsJson;

    @Column(name = "allowed_item_codes_json", nullable = false, columnDefinition = "LONGTEXT")
    private String allowedItemCodesJson;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
