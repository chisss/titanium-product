--liquibase formatted sql
--changeset weisun:product-surrender-value-handling-fee-202609301400
-- g05-05（R-2 裁示）：退保手续费率——与 internal_cost_retention_rate（内部成本留存率，仅披露不扣减）
-- 是两个独立概念，故独立成列。**可空**：NULL 表示该产品不适用（不收退保手续费），
-- 与「配置为 0.0」是两种业务事实（前者明细记 NONE、后者记 DEBIT/0），不得以默认值抹平。
-- 存量行保持 NULL ⇒ 明细无手续费因子扣减，退费金额与改造前逐分相同。
ALTER TABLE t_product_surrender_value_policy
    ADD COLUMN handling_fee_rate DECIMAL(20,8) NULL
        COMMENT '退保手续费率(NULL不适用/0适用但为0/(0,1]实扣)' AFTER internal_cost_retention_rate;
--rollback ALTER TABLE t_product_surrender_value_policy DROP COLUMN handling_fee_rate;

--changeset weisun:product-surrender-value-handling-fee-hash-202609301401
-- 手续费率已纳入策略发布内容哈希（canonicalContent，位于内留率与退费基准之间），
-- 种子行存量哈希须同批重算，否则取证字段与实际内容不符、退费争议时无从复算（g05-02 先例）。
-- 种子行未配置手续费率 ⇒ 该位渲染为 '*'（与代码 null 分支逐字一致）。
UPDATE t_product_surrender_value_policy
SET content_hash = SHA2(CONCAT(product_id,
    '|LIFE-SURRENDER-CASH-VALUE|V1.0|1|15|0.6|0|*|CASH_VALUE|2026-01-01T00:00|*|', tenant_id), 256)
WHERE policy_id = 'd3a10000-0000-4000-8000-000000000001';
--rollback UPDATE t_product_surrender_value_policy SET content_hash = SHA2(CONCAT(product_id, '|LIFE-SURRENDER-CASH-VALUE|V1.0|1|15|0.6|0|CASH_VALUE|2026-01-01T00:00|*|', tenant_id), 256) WHERE policy_id = 'd3a10000-0000-4000-8000-000000000001';
