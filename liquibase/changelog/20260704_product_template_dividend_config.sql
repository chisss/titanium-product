-- 阶段四 4.8：分红险形态建模
-- 为产品模板写侧表 t_product_template 增加分红配置列（红利分配方式 + 三档演示利率，整体 JSON 存储）
-- 采用 IF NOT EXISTS 保证幂等，兼容既有环境。

-- changeset wei.sun:20260704-product-template-dividend-config
ALTER TABLE t_product_template ADD COLUMN IF NOT EXISTS dividend_config_json TEXT COMMENT '分红配置(JSON)：红利分配方式+三档演示利率，分红险模板专属' AFTER base_config;
