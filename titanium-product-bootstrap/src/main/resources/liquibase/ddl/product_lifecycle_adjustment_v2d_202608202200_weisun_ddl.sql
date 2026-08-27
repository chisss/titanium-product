--liquibase formatted sql
--changeset weisun:product-lifecycle-adjustment-v2d-1
CREATE TABLE IF NOT EXISTS t_product_premium_lifecycle_adjustment (
    adjustment_id               VARCHAR(36)   NOT NULL COMMENT '生命周期差额事实ID',
    adjustment_request_id       VARCHAR(64)   NOT NULL COMMENT '幂等请求ID',
    biz_no                      VARCHAR(128)  NOT NULL COMMENT '保全/续期业务号',
    lifecycle_type              VARCHAR(24)   NOT NULL COMMENT '生命周期类型',
    tenant_id                   VARCHAR(32)   NOT NULL COMMENT '租户ID',
    product_id                  VARCHAR(36)   NOT NULL COMMENT '产品ID',
    original_calculation_id     VARCHAR(36)   NOT NULL COMMENT '原确认计算ID',
    original_result_hash        VARCHAR(64)   NOT NULL COMMENT '原确认计算结果hash',
    replacement_calculation_id  VARCHAR(36)   NOT NULL COMMENT '替代确认计算ID',
    replacement_result_hash     VARCHAR(64)   NOT NULL COMMENT '替代确认计算结果hash',
    business_time               DATETIME      NOT NULL COMMENT '业务时点',
    currency                    VARCHAR(3)    NOT NULL COMMENT '币种',
    direction                   VARCHAR(12)   NOT NULL COMMENT '客户余额方向',
    customer_amount             DECIMAL(20,8) NOT NULL COMMENT '客户追加应收/应退绝对金额',
    tax_direction               VARCHAR(12)   NOT NULL COMMENT '税费调整方向',
    tax_amount                  DECIMAL(20,8) NOT NULL COMMENT '税费调整绝对金额',
    internal_cost_direction     VARCHAR(12)   NOT NULL COMMENT '内部成本调整方向',
    internal_cost_amount        DECIMAL(20,8) NOT NULL COMMENT '内部成本调整绝对金额',
    reason                      VARCHAR(500)  NOT NULL COMMENT '生命周期变更原因',
    request_hash                VARCHAR(64)   NOT NULL COMMENT '规范化请求hash',
    result_hash                 VARCHAR(64)   NOT NULL COMMENT '差额结果hash',
    create_time                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (adjustment_id),
    UNIQUE KEY uk_product_lifecycle_adjustment_request (tenant_id, adjustment_request_id),
    KEY idx_product_lifecycle_adjustment_biz (tenant_id, biz_no, create_time),
    KEY idx_product_lifecycle_adjustment_original (tenant_id, original_calculation_id),
    KEY idx_product_lifecycle_adjustment_replacement (tenant_id, replacement_calculation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product不可变生命周期费用差额事实';
--rollback DROP TABLE IF EXISTS t_product_premium_lifecycle_adjustment;

--changeset weisun:product-lifecycle-adjustment-v2d-2
CREATE TABLE IF NOT EXISTS t_product_premium_lifecycle_adjustment_line (
    adjustment_id                 VARCHAR(36)   NOT NULL COMMENT '生命周期差额事实ID',
    line_id                       VARCHAR(64)   NOT NULL COMMENT '差额行ID',
    component_code                VARCHAR(64)   NOT NULL COMMENT '费用项编码',
    original_component_version    VARCHAR(32)   COMMENT '原费用项版本',
    replacement_component_version VARCHAR(32)   COMMENT '替代费用项版本',
    category                      VARCHAR(32)   NOT NULL COMMENT '费用分类',
    amount_channel                VARCHAR(24)   NOT NULL COMMENT '金额通道',
    direction                     VARCHAR(12)   NOT NULL COMMENT '差额方向',
    payer_type                    VARCHAR(24)   NOT NULL COMMENT '承担方',
    accounting_class              VARCHAR(64)   NOT NULL COMMENT '账务分类',
    currency                      VARCHAR(3)    NOT NULL COMMENT '币种',
    original_direction            VARCHAR(12)   COMMENT '原费用方向',
    before_amount                 DECIMAL(20,8) NOT NULL COMMENT '变更前绝对金额',
    replacement_direction         VARCHAR(12)   COMMENT '替代费用方向',
    after_amount                  DECIMAL(20,8) NOT NULL COMMENT '变更后绝对金额',
    difference_amount             DECIMAL(20,8) NOT NULL COMMENT '差额绝对金额',
    customer_visible              BOOLEAN       NOT NULL COMMENT '客户是否可见',
    affects_customer_payable      BOOLEAN       NOT NULL COMMENT '是否影响客户应付',
    description                   VARCHAR(500)  COMMENT '差额说明',
    PRIMARY KEY (adjustment_id, line_id),
    KEY idx_product_lifecycle_adjustment_line_component (adjustment_id, component_code),
    CONSTRAINT fk_product_lifecycle_adjustment_line_header FOREIGN KEY (adjustment_id)
        REFERENCES t_product_premium_lifecycle_adjustment (adjustment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product生命周期费用结构化差额行';
--rollback DROP TABLE IF EXISTS t_product_premium_lifecycle_adjustment_line;
