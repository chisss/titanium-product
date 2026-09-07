--liquibase formatted sql
--changeset weisun:product-12
-- dev-506b: 产品级保单结构配置下沉——补齐产品读模型标的结构列
-- policyStructureConfig(标的类型/标的字段Schema/多标的/参与方角色)随 ProductCreatedEvent 投影,
-- 存 PolicyStructureConfig 值对象 JSON, 与模板读模型 t_product_template_view.policy_structure_json 语义一致
ALTER TABLE t_product_view
    ADD COLUMN policy_structure_json TEXT COMMENT '保单结构配置(JSON序列化的PolicyStructureConfig:标的类型/标的字段Schema/多标的/参与方角色)';
--rollback ALTER TABLE t_product_view DROP COLUMN policy_structure_json;
