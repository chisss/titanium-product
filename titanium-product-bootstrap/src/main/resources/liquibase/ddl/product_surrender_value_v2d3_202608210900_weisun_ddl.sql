--liquibase formatted sql
--changeset weisun:product-surrender-value-v2d3-202608210900
CREATE TABLE IF NOT EXISTS t_product_surrender_value_policy (
    policy_id VARCHAR(36) NOT NULL COMMENT '退保价值策略ID',
    product_id VARCHAR(36) NOT NULL COMMENT '产品ID',
    policy_code VARCHAR(64) NOT NULL COMMENT '策略编码',
    policy_version VARCHAR(32) NOT NULL COMMENT '策略版本',
    policy_year INT NOT NULL COMMENT '适用保单年度',
    cooling_off_days INT NOT NULL COMMENT '犹豫期天数',
    cash_value_rate DECIMAL(20,8) NOT NULL COMMENT '期外现金价值率',
    internal_cost_retention_rate DECIMAL(20,8) NOT NULL COMMENT '退保后内部成本保留率',
    effective_from DATETIME NOT NULL COMMENT '生效时间',
    effective_to DATETIME NULL COMMENT '失效时间',
    tenant_id VARCHAR(32) NOT NULL COMMENT '租户ID',
    status VARCHAR(16) NOT NULL COMMENT '生命周期状态',
    content_hash VARCHAR(64) NOT NULL COMMENT '发布内容hash',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_id),
    UNIQUE KEY uk_product_surrender_value_version
        (tenant_id, product_id, policy_code, policy_version, policy_year),
    KEY idx_product_surrender_value_effective
        (tenant_id, product_id, policy_year, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product 版本化退保价值策略';
--rollback DROP TABLE IF EXISTS t_product_surrender_value_policy;

--changeset weisun:product-surrender-value-life-gold-v2d3-202608210901
INSERT INTO t_product_surrender_value_policy (
    policy_id, product_id, policy_code, policy_version, policy_year, cooling_off_days,
    cash_value_rate, internal_cost_retention_rate, effective_from, effective_to,
    tenant_id, status, content_hash)
SELECT 'd3a10000-0000-4000-8000-000000000001', p.product_id,
       'LIFE-SURRENDER-CASH-VALUE', 'V1.0', 1, 15, 0.60000000, 0.00000000,
       '2026-01-01 00:00:00', NULL, p.tenant_id, 'PUBLISHED',
       SHA2(CONCAT(p.product_id,
           '|LIFE-SURRENDER-CASH-VALUE|V1.0|1|15|0.6|0|2026-01-01T00:00|*|', p.tenant_id), 256)
FROM t_product_view p
WHERE p.product_id = '9a163467-131d-450c-a2d8-1555e034fc87' AND p.tenant_id = '1'
  AND NOT EXISTS (
      SELECT 1 FROM t_product_surrender_value_policy s
      WHERE s.tenant_id = p.tenant_id AND s.product_id = p.product_id
        AND s.policy_code = 'LIFE-SURRENDER-CASH-VALUE'
        AND s.policy_version = 'V1.0' AND s.policy_year = 1);
--rollback DELETE FROM t_product_surrender_value_policy WHERE policy_id = 'd3a10000-0000-4000-8000-000000000001';
