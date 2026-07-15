--liquibase formatted sql
--changeset weisun:product-1
CREATE TABLE IF NOT EXISTS t_product_view (
    product_id                   VARCHAR(36)  NOT NULL COMMENT '产品ID(聚合根ID,读模型主键)',
    product_code                 VARCHAR(64)  COMMENT '产品编码',
    product_name                 VARCHAR(128) COMMENT '产品名称',
    product_desc                 VARCHAR(512) COMMENT '产品描述',
    form                         VARCHAR(32)  COMMENT '产品形态(ProductForm)',
    insurance_type               VARCHAR(32)  COMMENT '险种类型',
    category                     VARCHAR(32)  COMMENT '产品类别(主/附加)',
    version_no                   VARCHAR(32)  COMMENT '版本号',
    status                       VARCHAR(32)  NOT NULL COMMENT '产品状态(DRAFT/AUDITING/EFFECTIVE/INVALID)',
    original_product_id          VARCHAR(36)  COMMENT '修订溯源:原始产品ID',
    effective_time               DATETIME     COMMENT '生效时间',
    invalid_time                 DATETIME     COMMENT '下架/失效时间',
    sale_start_time              DATETIME     COMMENT '销售起期',
    sale_end_time                DATETIME     COMMENT '销售止期',
    insure_condition_json        TEXT         COMMENT '投保条件配置(JSON)',
    coverage_period_json         TEXT         COMMENT '保障期间配置(JSON)',
    payment_config_json          TEXT         COMMENT '缴费配置(JSON)',
    pricing_basic_rule_json      TEXT         COMMENT '定价基础规则(JSON)',
    issuance_process_config_json TEXT         COMMENT '出单流程配置(JSON)',
    policy_form_config_json      TEXT         COMMENT '保单形态配置(JSON)',
    underwriting_config_json     TEXT         COMMENT '核保配置(JSON)',
    audit_info_json              TEXT         COMMENT '审核信息(JSON)',
    created_at                   DATETIME     COMMENT '业务创建时间(来源事件)',
    tenant_id                    VARCHAR(32)  NOT NULL COMMENT '租户ID',
    create_time                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投影创建时间',
    update_time                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '投影更新时间',
    version                      BIGINT       COMMENT '乐观锁版本(防并发投影覆盖)',
    PRIMARY KEY (product_id),
    KEY idx_product_view_tenant (tenant_id),
    KEY idx_product_view_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品读模型(CQRS Projection)';
--rollback DROP TABLE IF EXISTS t_product_view;

--changeset weisun:product-2
CREATE TABLE IF NOT EXISTS t_product_template_view (
    template_id              VARCHAR(36)  NOT NULL COMMENT '模板ID(读模型主键)',
    template_code            VARCHAR(64)  COMMENT '模板编码',
    template_name            VARCHAR(128) COMMENT '模板名称',
    insurance_category       VARCHAR(32)  COMMENT '险种大类(ProductCategory)',
    insurance_type           VARCHAR(32)  COMMENT '险种类型',
    product_id               VARCHAR(36)  COMMENT '关联产品ID',
    issuance_mode            VARCHAR(512) COMMENT '出单模式',
    policy_stages_json       TEXT         COMMENT '出单阶段定义(JSON)',
    underwriting_config_json TEXT         COMMENT '核保配置(JSON)',
    policy_structure_json    TEXT         COMMENT '保单结构配置(JSON)',
    maintenance_config_json  TEXT         COMMENT '保全配置(JSON)',
    claim_config_json        TEXT         COMMENT '理赔配置(JSON)',
    billing_config_json      TEXT         COMMENT '计费配置(JSON)',
    reinsurance_config_json  TEXT         COMMENT '再保险配置(JSON)',
    status                   VARCHAR(32)  COMMENT '模板状态(CommonStatus)',
    tenant_id                VARCHAR(32)  NOT NULL COMMENT '租户ID',
    create_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投影创建时间',
    update_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '投影更新时间',
    version                  BIGINT       COMMENT '乐观锁版本',
    PRIMARY KEY (template_id),
    KEY idx_product_template_view_tenant (tenant_id),
    KEY idx_product_template_view_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品模板读模型(CQRS Projection)';
--rollback DROP TABLE IF EXISTS t_product_template_view;

--changeset weisun:product-3
CREATE TABLE IF NOT EXISTS t_product_clause_rel_view (
    product_id       VARCHAR(36) NOT NULL COMMENT '产品ID(写侧聚合根ID,读模型主键)',
    clause_rels_json TEXT        COMMENT '条款关联清单(JSON序列化的ProductClauseRel列表)',
    tenant_id        VARCHAR(32) NOT NULL COMMENT '租户ID',
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投影创建时间',
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '投影更新时间',
    version          BIGINT      COMMENT '乐观锁版本',
    PRIMARY KEY (product_id),
    KEY idx_product_clause_rel_view_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品条款关联读模型(CQRS Projection)';
--rollback DROP TABLE IF EXISTS t_product_clause_rel_view;

--changeset weisun:product-4
-- 说明: t_life_product_spec 当前无对应 JPA/View 实体, 依方案清单 §3.2 字段 + 七件套建表, 待寿险投保规则实体落地后校准列
CREATE TABLE IF NOT EXISTS t_life_product_spec (
    id                        VARCHAR(32)  NOT NULL COMMENT '主键(雪花)',
    product_id                VARCHAR(36)  NOT NULL COMMENT '关联产品ID',
    min_age                   INT          COMMENT '最小投保年龄',
    max_age                   INT          COMMENT '最大投保年龄',
    min_sum_insured           DECIMAL(18,2) COMMENT '最低保额',
    max_sum_insured           DECIMAL(18,2) COMMENT '最高保额',
    hesitation_days           INT          COMMENT '犹豫期天数',
    waiting_days              INT          COMMENT '等待期天数',
    coverage_period_type      VARCHAR(32)  COMMENT '保障期间类型',
    premium_term_options_json TEXT         COMMENT '缴费期选项(JSON)',
    tenant_id                 VARCHAR(32)  NOT NULL COMMENT '租户ID',
    create_time               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by                VARCHAR(32)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    updated_by                VARCHAR(32)  NOT NULL DEFAULT 'system' COMMENT '更新人',
    is_deleted                TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(0否1是)',
    PRIMARY KEY (id),
    KEY idx_life_product_spec_tenant (tenant_id),
    KEY idx_life_product_spec_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寿险投保规则(方案清单占位,无实体)';
--rollback DROP TABLE IF EXISTS t_life_product_spec;

--changeset weisun:product-5
-- 补齐销售渠道读模型列: ProductSalesChannelUpdatedEvent 投影目标, 存 SalesChannelConfig 列表(JSON)
ALTER TABLE t_product_view
    ADD COLUMN sales_channels_json TEXT COMMENT '销售渠道配置(JSON序列化的SalesChannelConfig列表)';
--rollback ALTER TABLE t_product_view DROP COLUMN sales_channels_json;

--changeset weisun:product-6
-- 补齐寿险产品规格读模型列: LifeProductConfiguredEvent 投影目标, 存 LifeProductSpec(投保年龄/保额范围/缴费期/保障期, JSON)
ALTER TABLE t_product_template_view
    ADD COLUMN life_product_spec_json TEXT COMMENT '寿险产品规格(JSON序列化的LifeProductSpec)';
--rollback ALTER TABLE t_product_template_view DROP COLUMN life_product_spec_json;
