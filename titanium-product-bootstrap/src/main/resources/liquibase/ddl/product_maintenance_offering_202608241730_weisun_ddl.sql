--liquibase formatted sql

--changeset weisun:product-maintenance-offering-202608241730
-- Product 域按产品、计划和业务时点发布可受理保全项清单，供 Maintenance 建案冻结适用性证据。
CREATE TABLE t_product_maintenance_offering (
    offering_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    product_version VARCHAR(64) NOT NULL,
    plan_version VARCHAR(64) NOT NULL,
    offering_version VARCHAR(64) NOT NULL,
    effective_from DATETIME NOT NULL,
    effective_to DATETIME NULL,
    status VARCHAR(16) NOT NULL,
    allowed_policy_statuses_json LONGTEXT NOT NULL,
    allowed_channels_json LONGTEXT NOT NULL,
    allowed_item_codes_json LONGTEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (offering_id),
    UNIQUE KEY uk_product_maintenance_offering_version
        (tenant_id, product_id, product_version, plan_version, offering_version),
    KEY idx_product_maintenance_offering_effective
        (tenant_id, product_id, product_version, plan_version, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product版本化保全Offering';

--rollback DROP TABLE IF EXISTS t_product_maintenance_offering;
