--liquibase formatted sql

-- Feature Center 使用 UUID:revision 形式的不透明版本令牌，预留后续版本格式扩展空间。
--changeset weisun:product-dynamic-factor-version-length-v2c-202608201410
ALTER TABLE t_product_dynamic_factor
    MODIFY COLUMN feature_definition_version VARCHAR(128) NOT NULL COMMENT 'Feature Center 特征定义版本令牌';
--rollback ALTER TABLE t_product_dynamic_factor MODIFY COLUMN feature_definition_version VARCHAR(32) NOT NULL COMMENT '特征定义版本';
