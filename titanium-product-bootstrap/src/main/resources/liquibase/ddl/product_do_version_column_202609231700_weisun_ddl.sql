--liquibase formatted sql

--changeset weisun:product-do-version-202609231700
--comment m20-09 / PER-01：写侧 DO 补 @Version 乐观锁列。原实现无 @Version，JPA 走无条件 UPDATE，并发更新后写者静默覆盖前写者；补列后 UPDATE 带 WHERE version=?，冲突抛 ObjectOptimisticLockingFailureException（映射 409）。注：本域原本已有 @Version 的 DO（如 t_billing_payment_allocation）不在本变更内。
ALTER TABLE t_product_calculation_line ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_calculation_edge ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_calculation_model ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_calculation_node ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_calculation_total ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_charge_component ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_dynamic_factor ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_maintenance_offering ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_premium_calculation ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_premium_lifecycle_adjustment ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_premium_lifecycle_adjustment_line ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_pricing_plan ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_pricing_plan_commission_ref ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_pricing_plan_dynamic_factor_ref ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_pricing_plan_tax_ref ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_pricing_test_case ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_rate_table ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_rate_table_row ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_surrender_value_policy ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_tax_base_item ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
ALTER TABLE t_product_tax_policy ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本(JPA维护)';
