--liquibase formatted sql

--changeset weisun:product-v2b-tax-policy-1
CREATE TABLE IF NOT EXISTS t_product_tax_policy (
    policy_id               VARCHAR(36)   NOT NULL COMMENT '税费策略ID',
    product_id              VARCHAR(36)   NOT NULL COMMENT '产品ID',
    policy_code             VARCHAR(64)   NOT NULL COMMENT '策略编码',
    policy_version          VARCHAR(32)   NOT NULL COMMENT '策略版本',
    policy_name             VARCHAR(128)  NOT NULL COMMENT '策略名称',
    description             VARCHAR(500)  COMMENT '说明',
    jurisdiction_code       VARCHAR(64)   NOT NULL COMMENT '司法辖区编码',
    category                VARCHAR(32)   NOT NULL COMMENT '税费分类',
    payer_type              VARCHAR(32)   NOT NULL COMMENT '承担方',
    price_mode              VARCHAR(16)   NOT NULL COMMENT 'EXCLUSIVE/INCLUSIVE',
    tax_rate                DECIMAL(20,8) NOT NULL COMMENT '税率',
    accounting_class        VARCHAR(64)   NOT NULL COMMENT '账务分类',
    regulatory_reference_id VARCHAR(128)  NOT NULL COMMENT '法规依据',
    exemption_feature_code  VARCHAR(64)   COMMENT '布尔免税特征编码',
    effective_from          DATETIME      NOT NULL COMMENT '生效时间(含)',
    effective_to            DATETIME      COMMENT '失效时间(不含)',
    tenant_id               VARCHAR(32)   NOT NULL COMMENT '租户ID',
    status                  VARCHAR(16)   NOT NULL COMMENT '生命周期状态',
    content_hash            VARCHAR(64)   NOT NULL COMMENT '内容SHA-256',
    create_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_id),
    UNIQUE KEY uk_product_tax_policy (tenant_id, product_id, policy_code, policy_version),
    KEY idx_product_tax_policy_effective
        (tenant_id, product_id, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product版本化税费策略';
--rollback DROP TABLE IF EXISTS t_product_tax_policy;

--changeset weisun:product-v2b-tax-base-item-1
CREATE TABLE IF NOT EXISTS t_product_tax_base_item (
    item_id         VARCHAR(36) NOT NULL COMMENT '税基项ID',
    policy_id       VARCHAR(36) NOT NULL COMMENT '税费策略ID',
    component_code  VARCHAR(64) NOT NULL COMMENT '税基费用项编码',
    sort_order      INT         NOT NULL COMMENT '稳定顺序',
    PRIMARY KEY (item_id),
    UNIQUE KEY uk_product_tax_base_item (policy_id, component_code),
    CONSTRAINT fk_product_tax_base_policy FOREIGN KEY (policy_id)
        REFERENCES t_product_tax_policy (policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product税基费用组成';
--rollback DROP TABLE IF EXISTS t_product_tax_base_item;

--changeset weisun:product-v2b-calculation-line-tax-evidence-1
ALTER TABLE t_product_calculation_line
    ADD COLUMN affects_customer_payable TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否影响客户应付' AFTER customer_visible,
    ADD COLUMN jurisdiction_code VARCHAR(64) COMMENT '司法辖区编码' AFTER affects_customer_payable,
    ADD COLUMN regulatory_reference_id VARCHAR(128) COMMENT '法规依据' AFTER jurisdiction_code,
    ADD COLUMN tax_price_mode VARCHAR(16) COMMENT 'EXCLUSIVE/INCLUSIVE' AFTER regulatory_reference_id,
    ADD COLUMN tax_policy_hash VARCHAR(64) COMMENT '税费策略SHA-256' AFTER tax_price_mode,
    ADD COLUMN tax_exempt TINYINT(1) COMMENT '是否免税' AFTER tax_policy_hash;
--rollback ALTER TABLE t_product_calculation_line DROP COLUMN tax_exempt, DROP COLUMN tax_policy_hash, DROP COLUMN tax_price_mode, DROP COLUMN regulatory_reference_id, DROP COLUMN jurisdiction_code, DROP COLUMN affects_customer_payable;

--changeset weisun:product-v2b-pricing-plan-tax-ref-1
CREATE TABLE IF NOT EXISTS t_product_pricing_plan_tax_ref (
    ref_id          VARCHAR(36) NOT NULL COMMENT '引用ID',
    plan_id         VARCHAR(36) NOT NULL COMMENT '定价包ID',
    policy_code     VARCHAR(64) NOT NULL COMMENT '税费策略编码',
    policy_version  VARCHAR(32) NOT NULL COMMENT '税费策略版本',
    policy_hash     VARCHAR(64) NOT NULL COMMENT '税费策略SHA-256',
    sort_order      INT         NOT NULL COMMENT '稳定顺序',
    PRIMARY KEY (ref_id),
    UNIQUE KEY uk_product_pricing_plan_tax_ref (plan_id, policy_code, policy_version),
    CONSTRAINT fk_product_pricing_plan_tax_ref FOREIGN KEY (plan_id)
        REFERENCES t_product_pricing_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定价包锁定税费策略版本';
--rollback DROP TABLE IF EXISTS t_product_pricing_plan_tax_ref;
