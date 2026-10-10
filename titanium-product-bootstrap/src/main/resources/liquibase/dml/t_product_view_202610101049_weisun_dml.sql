--liquibase formatted sql
--changeset weisun:g10-03b-product-1

-- G10/g10-03b：两个 NULL 险种行的权威码回填（AC-04 迁移片③，R-2/D-3 裁示补 AUTO）。
--
-- R801-B3-101032（product_code=CFT-AUTO-COM-0904）与 R801-C2-101032（V2A-AUTO-001）均为
-- DRAFT 测试品、挂车险模板 TPL-1604A20B；其险种列为 NULL（修订链事件 newInsuranceType=null
-- 经投影 NullValuePropertyMappingStrategy.IGNORE 不入库）。
-- 稳定性：修订事件该字段为 null（不覆盖），SQL 回填值不会被投影重放改写。
-- 幂等：仅命中 insurance_type IS NULL 的行，重复执行零副作用。
UPDATE t_product_view
SET insurance_type = 'AUTO'
WHERE product_id = 'R801-B3-101032' AND insurance_type IS NULL;

UPDATE t_product_view
SET insurance_type = 'AUTO'
WHERE product_id = 'R801-C2-101032' AND insurance_type IS NULL;

--rollback UPDATE t_product_view SET insurance_type = NULL WHERE product_id IN ('R801-B3-101032','R801-C2-101032') AND insurance_type = 'AUTO';
