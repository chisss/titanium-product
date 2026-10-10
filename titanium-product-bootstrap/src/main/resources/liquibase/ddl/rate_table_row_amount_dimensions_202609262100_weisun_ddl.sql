--liquibase formatted sql
--changeset weisun:product-ratetable-row-amount-dimensions-1

-- 费率表行新增四个维度（保额/吸烟/社保/免赔额），共六列（G01/AC-05 费率维度字典化与扩展）
-- 保额、免赔额为区间型：From 含、ToExclusive 不含，两列均为 NULL 表示该维度不限；
-- 吸烟、社保为文本型：NULL 或 'ALL' 表示通配。
ALTER TABLE t_product_rate_table_row
    ADD COLUMN sum_insured_from         DECIMAL(18, 2) NULL COMMENT '保额下界(含)，null为该侧不限' AFTER vehicle_type,
    ADD COLUMN sum_insured_to_exclusive DECIMAL(18, 2) NULL COMMENT '保额上界(不含)，null为该侧不限' AFTER sum_insured_from,
    ADD COLUMN smoking                  VARCHAR(32)    NULL COMMENT '吸烟情况(null/ALL为通配)' AFTER sum_insured_to_exclusive,
    ADD COLUMN social_insurance         VARCHAR(32)    NULL COMMENT '社保情况(null/ALL为通配)' AFTER smoking,
    ADD COLUMN deductible_from          DECIMAL(18, 2) NULL COMMENT '免赔额下界(含)，null为该侧不限' AFTER social_insurance,
    ADD COLUMN deductible_to_exclusive  DECIMAL(18, 2) NULL COMMENT '免赔额上界(不含)，null为该侧不限' AFTER deductible_from;

-- 🔴 存量行的 dimension_hash 会与新算法不一致（新算法把六列一并纳入摘要）。
-- 无需数据订正：费率行的写入路径是「按 tableId 整表 delete + saveAll」
-- （JpaRateTableManagementRepository#save 仅在 DRAFT 态重建行集合），
-- 因此同一张表内不可能出现新旧两种摘要格式并存的局面；
-- 且 domain 侧 RateTableRow 在紧凑构造器里重算摘要，恒以领域侧为准，库列仅服务于唯一约束。

--rollback ALTER TABLE t_product_rate_table_row DROP COLUMN sum_insured_from, DROP COLUMN sum_insured_to_exclusive, DROP COLUMN smoking, DROP COLUMN social_insurance, DROP COLUMN deductible_from, DROP COLUMN deductible_to_exclusive;
