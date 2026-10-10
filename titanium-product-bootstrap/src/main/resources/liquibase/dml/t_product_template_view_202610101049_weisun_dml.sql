--liquibase formatted sql
--changeset weisun:g10-03b-template-1

-- G10/g10-03b：标准种子模板的险种编码存量迁移（AC-04 迁移片②）。
--
-- 背景：multi-insurance-nine-products.sql 曾用一段 CASE 批量 UPDATE 把 9 个 STD 模板的
-- legacy_insurance_type 统一改写为大类扁平码（该脚本 :26-40，本批同批删除）。对其中 4 行，
-- 改写丢失了可解析的特化维度（源 INSERT 原值即权威三级码或其同义扁平码）：
--   STD-TPL-CI    源值 CRITICAL_ILLNESS（同名异体系）       被洗为 MEDICAL
--   STD-TPL-AUTO  源值 CAR（AUTO 同义）                     被洗为 PROPERTY
--   STD-TPL-HOUSE 源值 HOUSEHOLD（HOUSEHOLD_PROPERTY 同义） 被洗为 PROPERTY
--   STD-TPL-EL    源值 LIABILITY（一对多，权威列留空不猜值） 被洗为 PROPERTY
-- 其余 5 行（TERM/WHOLE/MED/ACC/ENT）源值与洗值一致，无需迁移。
--
-- 判据：R-2 业务确认（2026-10-10，g10-r2-worksheet-20261010.md D-1/D-2 + 三行粗化恢复裁示）。
-- 稳定性：4 行均无 Axon 事件流（夹具直写读模型），迁移值不会被投影重放覆盖。
-- 幂等：仅命中「现值=被洗值」的行，重复执行零副作用。
UPDATE t_product_template_view
SET legacy_insurance_type = 'CRITICAL_ILLNESS', insurance_product_type = 'CRITICAL_ILLNESS'
WHERE template_id = 'STD-TPL-CI' AND legacy_insurance_type = 'MEDICAL';

UPDATE t_product_template_view
SET legacy_insurance_type = 'CAR', insurance_product_type = 'AUTO'
WHERE template_id = 'STD-TPL-AUTO' AND legacy_insurance_type = 'PROPERTY';

UPDATE t_product_template_view
SET legacy_insurance_type = 'HOUSEHOLD', insurance_product_type = 'HOUSEHOLD_PROPERTY'
WHERE template_id = 'STD-TPL-HOUSE' AND legacy_insurance_type = 'PROPERTY';

-- EL：LIABILITY 在 LegacyInsuranceTypeMapping 中为一对多（无唯一权威码），权威列留空由读侧折算命中
UPDATE t_product_template_view
SET legacy_insurance_type = 'LIABILITY', insurance_product_type = NULL
WHERE template_id = 'STD-TPL-EL' AND legacy_insurance_type = 'PROPERTY';

--rollback UPDATE t_product_template_view SET legacy_insurance_type='PROPERTY', insurance_product_type=NULL WHERE template_id='STD-TPL-EL' AND legacy_insurance_type='LIABILITY'; UPDATE t_product_template_view SET legacy_insurance_type='PROPERTY', insurance_product_type=NULL WHERE template_id='STD-TPL-HOUSE' AND legacy_insurance_type='HOUSEHOLD'; UPDATE t_product_template_view SET legacy_insurance_type='PROPERTY', insurance_product_type=NULL WHERE template_id='STD-TPL-AUTO' AND legacy_insurance_type='CAR'; UPDATE t_product_template_view SET legacy_insurance_type='MEDICAL', insurance_product_type=NULL WHERE template_id='STD-TPL-CI' AND legacy_insurance_type='CRITICAL_ILLNESS';
