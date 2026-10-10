--liquibase formatted sql
--changeset weisun:product-reduced-paid-up-policy-202609281800
CREATE TABLE IF NOT EXISTS t_product_reduced_paid_up_policy (
    policy_id VARCHAR(36) NOT NULL COMMENT '减额缴清策略ID',
    product_id VARCHAR(36) NOT NULL COMMENT '产品ID',
    policy_code VARCHAR(64) NOT NULL COMMENT '策略编码',
    policy_version VARCHAR(32) NOT NULL COMMENT '策略版本',
    net_single_premium_rate_per_unit DECIMAL(20,8) NOT NULL COMMENT '每元保额趸缴净费率(>0,无上界)',
    effective_from DATETIME NOT NULL COMMENT '生效时间',
    effective_to DATETIME NULL COMMENT '失效时间',
    tenant_id VARCHAR(32) NOT NULL COMMENT '租户ID',
    status VARCHAR(16) NOT NULL COMMENT '生命周期状态',
    content_hash VARCHAR(64) NOT NULL COMMENT '发布内容hash',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_id),
    UNIQUE KEY uk_product_reduced_paid_up_policy_version
        (tenant_id, product_id, policy_code, policy_version),
    KEY idx_product_reduced_paid_up_policy_effective
        (tenant_id, product_id, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product 版本化减额缴清净费率策略(g04-05)';
--rollback DROP TABLE IF EXISTS t_product_reduced_paid_up_policy;
