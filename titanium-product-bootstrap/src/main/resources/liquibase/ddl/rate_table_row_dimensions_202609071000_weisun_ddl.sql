--liquibase formatted sql
--changeset weisun:product-ratetable-row-dimensions-1

-- 费率表行新增三个可选维度列：职业类别/地区/车型（dev-507，G6 费率表维度扩展）
ALTER TABLE t_product_rate_table_row
    ADD COLUMN occupation_class VARCHAR(32) NULL COMMENT '职业类别(如意外险1-6类，null为通配)' AFTER coverage_term_years,
    ADD COLUMN region            VARCHAR(64) NULL COMMENT '地区代码(null为通配)' AFTER occupation_class,
    ADD COLUMN vehicle_type      VARCHAR(64) NULL COMMENT '车型代码(null为通配)' AFTER region;

--rollback ALTER TABLE t_product_rate_table_row DROP COLUMN occupation_class, DROP COLUMN region, DROP COLUMN vehicle_type;
