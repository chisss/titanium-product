--liquibase formatted sql
--changeset weisun:product-14
-- G11/g11-04: 产品险别列——与险种正交的维度(法定强制/自愿投保)
-- 车险「交强+商业同单」即同一 AUTO 险种下的两个险别,产品侧以 insuranceMandateType 承载,
-- 随策略状态快照冻结进保单险种段(见 PolicyProduct.insuranceMandateType)。
-- 可空: NULL = 未声明险别(存量产品与全部非车险产品均不声明), 消费方按「非强制」处理。
ALTER TABLE t_product_view
    ADD COLUMN insurance_mandate_type VARCHAR(32) COMMENT '险别(法定强制COMPULSORY/自愿投保VOLUNTARY;与险种正交,NULL=未声明;G11/g11-04)' AFTER insurance_type;
--rollback ALTER TABLE t_product_view DROP COLUMN insurance_mandate_type;
