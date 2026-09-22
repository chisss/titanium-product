--liquibase formatted sql
--changeset weisun:product-view-created-at-backfill-1

-- 回填 t_product_view.created_at（A4 版本历史）：修订产出的版本行 created_at 为空。
--
-- 根因：`ProductCreatedEvent` 携带 createdAt/createdBy，而 `ProductRevisedEvent` 不携带
-- （见事件 record 字段表），故 create 路径两列有值、revise 路径两列为空。
-- 后果：产品列表与新增的「版本历史」抽屉里，**最新的一版**反而没有创建时间，
-- 且列表/版本线均按 created_at 倒序（MySQL 的 DESC 把 NULL 排在最后）⇒ 最新版本沉底。
--
-- 回填值取本行的 create_time（投影写入本行的时刻，即该版本生成之时），语义与事件的 createdAt 一致。
-- 幂等：仅命中 created_at IS NULL 的行；投影已同步修正（revise 分支补记 createdAt），不会再生新空值。
UPDATE t_product_view
SET created_at = create_time
WHERE created_at IS NULL;

--rollback UPDATE t_product_view SET created_at = NULL WHERE created_at = create_time;
