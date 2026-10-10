--liquibase formatted sql
--changeset weisun:product-ncd-rule-set-202610101520
-- G11/g11-03：NCD（无赔款优待）反算规则集。规则本体落**产品域**（与费率配置同域），
-- 用独立规则表承载，不复用/污染 PremiumRule、PayoutRule。
-- 🔴 本次只建**结构**、不落任何规则值（R-3 未裁前不得写死档位系数）——
-- 空表是刻意的：反算在无规则时以 NCD_RULE_NOT_CONFIGURED 显式失败，绝不回落 1.0。
CREATE TABLE IF NOT EXISTS t_product_ncd_rule_set (
    rule_set_id VARCHAR(36) NOT NULL COMMENT '规则集ID',
    product_id VARCHAR(36) NOT NULL COMMENT '产品ID',
    rule_set_code VARCHAR(64) NOT NULL COMMENT '规则集编码',
    rule_set_version VARCHAR(32) NOT NULL COMMENT '规则集版本',
    lookback_months INT NOT NULL COMMENT '历史赔案统计区间长度(月,>=1;取值口径属R-3待确认项,本列只承载配置值)',
    effective_from DATETIME NOT NULL COMMENT '生效时间',
    effective_to DATETIME NULL COMMENT '失效时间(NULL=长期有效)',
    tenant_id VARCHAR(32) NOT NULL COMMENT '租户ID',
    status VARCHAR(16) NOT NULL COMMENT '生命周期状态(DRAFT/APPROVED/PUBLISHED/RETIRED)',
    content_hash VARCHAR(64) NOT NULL COMMENT '发布内容hash(档位按bandKey排序后拼接,序无关)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (rule_set_id),
    UNIQUE KEY uk_product_ncd_rule_set_version
        (tenant_id, product_id, rule_set_code, rule_set_version),
    KEY idx_product_ncd_rule_set_effective
        (tenant_id, product_id, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product NCD反算规则集(g11-03,只建结构不落规则值)';
--rollback DROP TABLE IF EXISTS t_product_ncd_rule_set;

--changeset weisun:product-ncd-rule-202610101521
-- NCD 档位（次数 → 系数）。区间口径为**闭区间** [min_count, max_count]（离散计数，半开会让
-- 恰好等于上界的次数落空），与时间区间的半开 [from, to) 是两套口径、不可互相套用。
-- 🔴 band_key 是唯一键载体：max_count / vehicle_type 可空，而 MySQL 唯一键对 NULL 不去重，
-- 直接对二者建唯一键挡不住「两条 (3, NULL, NULL) 无上界档」这种最常见的重复情形；
-- 本列非空（无量值以 '*' 占位）且与 bandKey() 逐字同构，约束才真正生效。
CREATE TABLE IF NOT EXISTS t_product_ncd_rule (
    rule_id VARCHAR(36) NOT NULL COMMENT '档位ID',
    rule_set_id VARCHAR(36) NOT NULL COMMENT '规则集ID',
    min_count INT NOT NULL COMMENT '次数区间下界(含,>=0)',
    max_count INT NULL COMMENT '次数区间上界(含;NULL=无上界)',
    vehicle_type VARCHAR(64) NULL COMMENT '车型标识(NULL=通用档,适用全部车型)',
    band_key VARCHAR(128) NOT NULL COMMENT '档位键冗余列(下界|上界|车型,无量值以*占位;唯一键载体)',
    coefficient DECIMAL(18,8) NOT NULL COMMENT 'NCD系数(>0)',
    tenant_id VARCHAR(32) NOT NULL COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (rule_id),
    UNIQUE KEY uk_product_ncd_rule_band
        (tenant_id, rule_set_id, band_key),
    KEY idx_product_ncd_rule_set
        (tenant_id, rule_set_id, min_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product NCD档位(次数闭区间→系数,g11-03)';
--rollback DROP TABLE IF EXISTS t_product_ncd_rule;
