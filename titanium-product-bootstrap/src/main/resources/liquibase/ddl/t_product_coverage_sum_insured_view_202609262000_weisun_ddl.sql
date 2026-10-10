--liquibase formatted sql
--changeset weisun:product-coverage-sum-insured-202609262000
-- 责任级保额档次配置读模型（G01-03 / AC-04）
-- 一条产品一行、整产品整体替换：档次总是按产品整体替换，拆成「一条责任一行」会让整体替换语义
-- 退化成逐责任拼装（并引入部分成功/部分失败的中间态）。
-- 🔴 无需存量回填：本能力为新建，存量产品的档次配置在业务上本就是空的；读侧查不到行即返回空列表
--（语义等价于「尚未配置」），首次配置或修订时由投影 upsert 建行。
CREATE TABLE IF NOT EXISTS t_product_coverage_sum_insured_view (
    product_id   VARCHAR(36) NOT NULL COMMENT '产品ID(写侧聚合根ID,读模型主键)',
    configs_json TEXT        COMMENT '责任保额档次配置清单(JSON序列化的CoverageSumInsuredConfig列表)',
    tenant_id    VARCHAR(32) NOT NULL COMMENT '租户ID',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投影创建时间',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '投影更新时间',
    version      BIGINT      COMMENT '乐观锁版本',
    PRIMARY KEY (product_id),
    KEY idx_product_coverage_sum_insured_view_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品责任保额档次配置读模型(CQRS Projection)';
--rollback DROP TABLE IF EXISTS t_product_coverage_sum_insured_view;
