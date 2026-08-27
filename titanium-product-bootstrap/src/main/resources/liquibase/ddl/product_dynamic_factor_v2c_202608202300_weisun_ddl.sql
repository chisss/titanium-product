--liquibase formatted sql
--changeset weisun:product-dynamic-factor-v2c-202608202300
CREATE TABLE IF NOT EXISTS t_product_dynamic_factor (
    factor_id VARCHAR(36) NOT NULL COMMENT '动态因子ID', product_id VARCHAR(36) NOT NULL COMMENT '产品ID',
    factor_code VARCHAR(64) NOT NULL COMMENT '输出因子编码', factor_version VARCHAR(32) NOT NULL COMMENT '因子版本',
    factor_name VARCHAR(128) NOT NULL COMMENT '因子名称', description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '说明',
    feature_code VARCHAR(64) NOT NULL COMMENT 'Feature Center 特征编码',
    feature_definition_version VARCHAR(32) NOT NULL COMMENT '特征定义版本',
    source_type VARCHAR(32) NOT NULL COMMENT '特征来源', value_time_policy VARCHAR(32) NOT NULL COMMENT '取值时点策略',
    lower_bound DECIMAL(20,8) NULL COMMENT '原始值下限', upper_bound DECIMAL(20,8) NULL COMMENT '原始值上限',
    missing_policy VARCHAR(24) NOT NULL COMMENT '缺失策略', default_value DECIMAL(20,8) NULL COMMENT '缺失默认值',
    transform_type VARCHAR(24) NOT NULL COMMENT '变换类型', multiplier DECIMAL(20,8) NOT NULL COMMENT '线性乘数',
    offset_value DECIMAL(20,8) NOT NULL COMMENT '线性偏移量', replayable TINYINT(1) NOT NULL COMMENT '是否可重放',
    effective_from DATETIME NOT NULL COMMENT '生效时间', effective_to DATETIME NULL COMMENT '失效时间',
    tenant_id VARCHAR(32) NOT NULL COMMENT '租户ID', status VARCHAR(16) NOT NULL COMMENT '生命周期状态',
    content_hash VARCHAR(64) NOT NULL DEFAULT '' COMMENT '发布内容hash',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (factor_id),
    UNIQUE KEY uk_product_dynamic_factor (tenant_id, product_id, factor_code, factor_version),
    KEY idx_product_dynamic_factor_effective (tenant_id, product_id, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product 动态定价因子';
--rollback DROP TABLE IF EXISTS t_product_dynamic_factor;

--changeset weisun:product-pricing-plan-dynamic-factor-ref-v2c-202608202301
CREATE TABLE IF NOT EXISTS t_product_pricing_plan_dynamic_factor_ref (
    ref_id VARCHAR(36) NOT NULL COMMENT '引用ID', plan_id VARCHAR(36) NOT NULL COMMENT '定价包ID',
    factor_code VARCHAR(64) NOT NULL COMMENT '动态因子编码', factor_version VARCHAR(32) NOT NULL COMMENT '动态因子版本',
    factor_hash VARCHAR(64) NOT NULL COMMENT '动态因子内容hash', sort_order INT NOT NULL COMMENT '顺序',
    PRIMARY KEY (ref_id), UNIQUE KEY uk_product_pricing_plan_factor_ref (plan_id, factor_code),
    CONSTRAINT fk_product_pricing_plan_factor_ref FOREIGN KEY (plan_id)
        REFERENCES t_product_pricing_plan (plan_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定价包动态因子精确引用';
--rollback DROP TABLE IF EXISTS t_product_pricing_plan_dynamic_factor_ref;

--changeset weisun:product-premium-calculation-dynamic-factor-evidence-v2c-202608202302
ALTER TABLE t_product_premium_calculation
    ADD COLUMN dynamic_factor_evidence_json LONGTEXT NULL COMMENT '动态因子版本证据(JSON)' AFTER feature_snapshot_id;
--rollback ALTER TABLE t_product_premium_calculation DROP COLUMN dynamic_factor_evidence_json;
