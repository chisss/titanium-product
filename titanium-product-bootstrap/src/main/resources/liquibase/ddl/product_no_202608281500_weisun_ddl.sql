--liquibase formatted sql
--changeset weisun:product-no-1
ALTER TABLE t_product_view
    ADD COLUMN product_no VARCHAR(32) NULL COMMENT '产品业务号(系统生成)' AFTER product_id;
--rollback ALTER TABLE t_product_view DROP COLUMN product_no;
