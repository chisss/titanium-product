--liquibase formatted sql
--changeset weisun:product-13
-- G10/g10-02：模板读模型险种键语义切分。此前 t_product_template_view.insurance_type 存遗留扁平码
-- （metadata InsuranceType），与 t_product_view.insurance_type（三级码 InsuranceProductType）同名两列载两义。
-- P1 双写：新增权威键列 insurance_product_type（投影按遗留码「唯一可解析」时写入；一对多/无对映留空不猜值），
-- 原列更名 legacy_insurance_type 保留（写点收敛为投影一处；g10-03 存量迁移前新列可能为空，读侧回落旧列）。
ALTER TABLE t_product_template_view
    ADD COLUMN insurance_product_type VARCHAR(32) NULL COMMENT '险种类型(权威三级码, metadata InsuranceProductType; 一对多/无对映遗留值此处为空)' AFTER insurance_category;
ALTER TABLE t_product_template_view
    CHANGE COLUMN insurance_type legacy_insurance_type VARCHAR(32) NULL COMMENT '遗留扁平险种编码(metadata InsuranceType, G10 起废弃; 权威键为 insurance_product_type)';
--rollback ALTER TABLE t_product_template_view CHANGE COLUMN legacy_insurance_type insurance_type VARCHAR(32) NULL COMMENT '险种类型'; ALTER TABLE t_product_template_view DROP COLUMN insurance_product_type;
