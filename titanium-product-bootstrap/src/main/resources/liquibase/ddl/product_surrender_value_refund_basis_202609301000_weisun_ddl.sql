--liquibase formatted sql
--changeset weisun:product-surrender-value-refund-basis-202609301000
-- g05-02：退费基准（择一）——长期险按现金价值、一年期短期险按未满期保费。
-- 缺省 CASH_VALUE 使存量策略行零行为变化（口径与改造前逐分相同）。
ALTER TABLE t_product_surrender_value_policy
    ADD COLUMN refund_basis VARCHAR(32) NOT NULL DEFAULT 'CASH_VALUE'
        COMMENT '退费基准(CASH_VALUE现金价值/UNEARNED_PREMIUM未满期保费)' AFTER internal_cost_retention_rate;
--rollback ALTER TABLE t_product_surrender_value_policy DROP COLUMN refund_basis;

--changeset weisun:product-surrender-value-refund-basis-hash-202609301001
-- 退费基准已纳入策略发布内容哈希（canonicalContent），故种子行的存量哈希须同批重算，
-- 否则「哈希与实际内容不符」——取证字段一旦对不上，退费争议时无从复算。
UPDATE t_product_surrender_value_policy
SET content_hash = SHA2(CONCAT(product_id,
    '|LIFE-SURRENDER-CASH-VALUE|V1.0|1|15|0.6|0|CASH_VALUE|2026-01-01T00:00|*|', tenant_id), 256)
WHERE policy_id = 'd3a10000-0000-4000-8000-000000000001';
--rollback UPDATE t_product_surrender_value_policy SET content_hash = SHA2(CONCAT(product_id, '|LIFE-SURRENDER-CASH-VALUE|V1.0|1|15|0.6|0|2026-01-01T00:00|*|', tenant_id), 256) WHERE policy_id = 'd3a10000-0000-4000-8000-000000000001';
