--liquibase formatted sql
--changeset weisun:product-ratetable-row-g01r-dimensions-1

-- 费率表行补齐七个维度（G01-R3-F1）：员工人数 / 工资总额 / 财产用途 / 财产价值 / 占用性质 /
-- 消防等级 / 无赔款优待系数，共九列。
-- 这七个维度键**原已存在于现网费率表数据**（t_product_rate_table.dimension_keys_json 与
-- t_product_pricing_plan.rate_dimension_keys_json 两处同写），却不在 RateDimensionKey 枚举内
-- ⇒ 读侧 RateTableDefinition.validateDimensionKeys 一律 isSupported=false ⇒ 抛
-- RATE_TABLE_VALIDATION_FAILED(60000111)，存量费率表**恒 400 打不开**（真机实测：
-- STD-RATE-EL / STD-RATE-ENT / STD-RATE-HOUSE 三张标准产品表全部命中）。
-- 本 changeset 补齐其行级承载列，使「声明（枚举）↔ 存储（列）↔ 数据（值）」三者一致。
-- 形态：区间型（工资总额/财产价值）From 含、ToExclusive 不含，两列均 NULL 表示该侧不限；
--       文本型（财产用途/占用性质/消防等级/无赔款优待系数）NULL 或 'ALL' 表示通配；
--       整数型（员工人数）NULL 表示通配，否则精确匹配。
ALTER TABLE t_product_rate_table_row
    ADD COLUMN employee_count              INT            NULL COMMENT '员工人数，null为通配' AFTER deductible_to_exclusive,
    ADD COLUMN payroll_amount_from         DECIMAL(18, 2) NULL COMMENT '工资总额下界(含)，null为该侧不限' AFTER employee_count,
    ADD COLUMN payroll_amount_to_exclusive DECIMAL(18, 2) NULL COMMENT '工资总额上界(不含)，null为该侧不限' AFTER payroll_amount_from,
    ADD COLUMN property_usage              VARCHAR(32)    NULL COMMENT '财产用途(null/ALL为通配)' AFTER payroll_amount_to_exclusive,
    ADD COLUMN property_value_from         DECIMAL(18, 2) NULL COMMENT '财产价值下界(含)，null为该侧不限' AFTER property_usage,
    ADD COLUMN property_value_to_exclusive DECIMAL(18, 2) NULL COMMENT '财产价值上界(不含)，null为该侧不限' AFTER property_value_from,
    ADD COLUMN occupancy_type              VARCHAR(32)    NULL COMMENT '占用性质(null/ALL为通配)' AFTER property_value_to_exclusive,
    ADD COLUMN fire_protection_level       VARCHAR(32)    NULL COMMENT '消防等级(null/ALL为通配)' AFTER occupancy_type,
    ADD COLUMN ncd                         VARCHAR(16)    NULL COMMENT '无赔款优待系数(null/ALL为通配)' AFTER fire_protection_level;

-- 🔴 存量行的 dimension_hash 会与新算法不一致（新算法把九列一并纳入摘要，分量数 14→23）。
-- 无需数据订正，依据同 rate_table_row_amount_dimensions_202609262100：
-- 费率行的写入路径是「按 tableId 整表 delete + saveAll」（JpaRateTableManagementRepository#save
-- 仅在 DRAFT 态重建行集合），同一张表内不可能出现新旧两种摘要格式并存的局面；
-- 且 domain 侧 RateTableRow 在紧凑构造器里重算摘要，恒以领域侧为准，库列仅服务于唯一约束
-- uk_product_rate_row_dimension(tenant_id, table_id, dimension_hash)。

--rollback ALTER TABLE t_product_rate_table_row DROP COLUMN employee_count, DROP COLUMN payroll_amount_from, DROP COLUMN payroll_amount_to_exclusive, DROP COLUMN property_usage, DROP COLUMN property_value_from, DROP COLUMN property_value_to_exclusive, DROP COLUMN occupancy_type, DROP COLUMN fire_protection_level, DROP COLUMN ncd;
