--liquibase formatted sql
--changeset weisun:product-lifecycle-reversal-v2d3b-1
ALTER TABLE t_product_premium_lifecycle_adjustment
    ADD COLUMN reversal_of_adjustment_id VARCHAR(36) NULL COMMENT '被冲正的原生命周期差额事实ID' AFTER adjustment_request_id;

--rollback ALTER TABLE t_product_premium_lifecycle_adjustment DROP COLUMN reversal_of_adjustment_id;

--changeset weisun:product-lifecycle-reversal-v2d3b-2
CREATE INDEX idx_product_lifecycle_adjustment_reversal
    ON t_product_premium_lifecycle_adjustment (tenant_id, reversal_of_adjustment_id);

--rollback DROP INDEX idx_product_lifecycle_adjustment_reversal ON t_product_premium_lifecycle_adjustment;

--changeset weisun:product-lifecycle-reversal-v2d3b-3
CREATE UNIQUE INDEX uk_product_lifecycle_adjustment_reversal
    ON t_product_premium_lifecycle_adjustment (tenant_id, reversal_of_adjustment_id);

--rollback DROP INDEX uk_product_lifecycle_adjustment_reversal ON t_product_premium_lifecycle_adjustment;
