-- V2: 产品表重构迁移脚本
-- 新增字段：product_code, product_desc, category, sale_start_time, sale_end_time
-- 新增JSON字段：insure_condition, coverage_period, payment_config, pricing_basic_rule,
--               issuance_process_config, policy_form_config, underwriting_config,
--               sales_channels, attach_product_ids
-- 新增审核信息字段：auditor_id, auditor_name, audit_opinion, audit_time, audit_result
-- 新增规则引擎预留字段
-- 新增产品销售渠道表 t_product_sales_channel
-- 新增产品附加险关联表 t_product_attach_rel

-- 产品表新增字段
ALTER TABLE t_product ADD COLUMN product_code VARCHAR(50) AFTER product_id;
ALTER TABLE t_product ADD COLUMN product_desc TEXT AFTER product_name;
ALTER TABLE t_product ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'MAIN' AFTER insurance_type;
ALTER TABLE t_product ADD COLUMN sale_start_time DATETIME AFTER invalid_time;
ALTER TABLE t_product ADD COLUMN sale_end_time DATETIME AFTER sale_start_time;

-- JSON值对象字段
ALTER TABLE t_product ADD COLUMN insure_condition JSON AFTER sale_end_time;
ALTER TABLE t_product ADD COLUMN coverage_period JSON AFTER insure_condition;
ALTER TABLE t_product ADD COLUMN payment_config JSON AFTER coverage_period;
ALTER TABLE t_product ADD COLUMN pricing_basic_rule JSON AFTER payment_config;
ALTER TABLE t_product ADD COLUMN issuance_process_config JSON AFTER pricing_basic_rule;
ALTER TABLE t_product ADD COLUMN policy_form_config JSON AFTER issuance_process_config;
ALTER TABLE t_product ADD COLUMN underwriting_config JSON AFTER policy_form_config;
ALTER TABLE t_product ADD COLUMN sales_channels JSON AFTER underwriting_config;
ALTER TABLE t_product ADD COLUMN attach_product_ids JSON AFTER sales_channels;

-- 审核信息字段
ALTER TABLE t_product ADD COLUMN auditor_id VARCHAR(50) AFTER attach_product_ids;
ALTER TABLE t_product ADD COLUMN auditor_name VARCHAR(50) AFTER auditor_id;
ALTER TABLE t_product ADD COLUMN audit_opinion VARCHAR(500) AFTER auditor_name;
ALTER TABLE t_product ADD COLUMN audit_time DATETIME AFTER audit_opinion;
ALTER TABLE t_product ADD COLUMN audit_result VARCHAR(20) AFTER audit_time;

-- 规则引擎预留字段
ALTER TABLE t_product ADD COLUMN pricing_rule_set_id VARCHAR(50) AFTER audit_result;
ALTER TABLE t_product ADD COLUMN insure_condition_rule_set_id VARCHAR(50) AFTER pricing_rule_set_id;
ALTER TABLE t_product ADD COLUMN underwriting_rule_set_id VARCHAR(50) AFTER insure_condition_rule_set_id;

-- 产品代码唯一索引
CREATE UNIQUE INDEX uk_product_code_tenant ON t_product (product_code, tenant_id);

-- 创建产品销售渠道表
CREATE TABLE t_product_sales_channel (
    id BIGINT AUTO_INCREMENT,
    product_id VARCHAR(36) NOT NULL,
    channel_type VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    commission_rate DECIMAL(10,4),
    tenant_id VARCHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    CONSTRAINT pk_product_sales_channel PRIMARY KEY (id)
);

CREATE INDEX idx_sales_channel_product_id ON t_product_sales_channel (product_id);
CREATE INDEX idx_sales_channel_tenant_id ON t_product_sales_channel (tenant_id);

-- 创建产品附加险关联表
CREATE TABLE t_product_attach_rel (
    id BIGINT AUTO_INCREMENT,
    main_product_id VARCHAR(36) NOT NULL,
    attach_product_id VARCHAR(36) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id VARCHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    CONSTRAINT pk_product_attach_rel PRIMARY KEY (id)
);

CREATE INDEX idx_attach_rel_main_product_id ON t_product_attach_rel (main_product_id);
CREATE INDEX idx_attach_rel_tenant_id ON t_product_attach_rel (tenant_id);

-- 条款关联表添加bind_time字段（如果不存在）
ALTER TABLE t_product_clause_rel ADD COLUMN IF NOT EXISTS bind_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER is_main_clause;
