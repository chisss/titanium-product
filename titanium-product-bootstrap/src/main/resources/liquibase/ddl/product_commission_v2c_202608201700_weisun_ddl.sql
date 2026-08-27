--liquibase formatted sql

--changeset weisun:product-pricing-plan-commission-ref-v2c-1
CREATE TABLE IF NOT EXISTS t_product_pricing_plan_commission_ref (
    ref_id         VARCHAR(36) NOT NULL COMMENT '引用ID',
    plan_id        VARCHAR(36) NOT NULL COMMENT '定价包ID',
    channel_id     VARCHAR(64) NOT NULL COMMENT '渠道ID',
    scheme_code    VARCHAR(64) NOT NULL COMMENT '佣金方案编码',
    scheme_version VARCHAR(32) NOT NULL COMMENT '佣金方案版本',
    scheme_hash    VARCHAR(64) NOT NULL COMMENT '佣金方案SHA-256',
    sort_order     INT NOT NULL COMMENT '显示顺序',
    PRIMARY KEY (ref_id),
    UNIQUE KEY uk_product_plan_commission_channel (plan_id, channel_id),
    KEY idx_product_plan_commission_scheme (scheme_code, scheme_version),
    CONSTRAINT fk_product_plan_commission_ref FOREIGN KEY (plan_id)
        REFERENCES t_product_pricing_plan (plan_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product定价包佣金方案版本引用';
--rollback DROP TABLE IF EXISTS t_product_pricing_plan_commission_ref;

--changeset weisun:product-calculation-line-commission-evidence-v2c-1
ALTER TABLE t_product_calculation_line
    ADD COLUMN commission_channel_id VARCHAR(64) COMMENT '佣金渠道ID' AFTER tax_exempt,
    ADD COLUMN commission_scheme_code VARCHAR(64) COMMENT '佣金方案编码' AFTER commission_channel_id,
    ADD COLUMN commission_scheme_version VARCHAR(32) COMMENT '佣金方案版本' AFTER commission_scheme_code,
    ADD COLUMN commission_scheme_hash VARCHAR(64) COMMENT '佣金方案SHA-256' AFTER commission_scheme_version,
    ADD COLUMN commission_beneficiary_type VARCHAR(32) COMMENT '佣金受益方类型' AFTER commission_scheme_hash,
    ADD COLUMN commission_beneficiary_id VARCHAR(64) COMMENT '佣金受益方ID' AFTER commission_beneficiary_type,
    ADD COLUMN commission_split_rate DECIMAL(20,8) COMMENT '佣金分润比例' AFTER commission_beneficiary_id,
    ADD COLUMN commission_gross_amount DECIMAL(20,8) COMMENT '分润前佣金总额' AFTER commission_split_rate,
    ADD COLUMN commission_installment_count INT COMMENT '佣金应付分期数' AFTER commission_gross_amount,
    ADD COLUMN commission_clawback_months INT COMMENT '佣金回拨窗口月数' AFTER commission_installment_count;
--rollback ALTER TABLE t_product_calculation_line DROP COLUMN commission_clawback_months, DROP COLUMN commission_installment_count, DROP COLUMN commission_gross_amount, DROP COLUMN commission_split_rate, DROP COLUMN commission_beneficiary_id, DROP COLUMN commission_beneficiary_type, DROP COLUMN commission_scheme_hash, DROP COLUMN commission_scheme_version, DROP COLUMN commission_scheme_code, DROP COLUMN commission_channel_id;
