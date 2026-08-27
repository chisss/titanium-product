--liquibase formatted sql
--changeset weisun:product-pricing-plan-1
CREATE TABLE IF NOT EXISTS t_product_pricing_plan (
    plan_id                    VARCHAR(36)  NOT NULL COMMENT '定价方案ID',
    product_id                 VARCHAR(36)  NOT NULL COMMENT '产品ID',
    product_version            VARCHAR(32)  NOT NULL COMMENT '产品版本',
    plan_version               VARCHAR(32)  NOT NULL COMMENT '定价方案版本',
    pricing_mode               VARCHAR(32)  NOT NULL COMMENT '定价模式',
    status                     VARCHAR(16)  NOT NULL COMMENT 'DRAFT/APPROVED/PUBLISHED/RETIRED',
    currency                   VARCHAR(3)   NOT NULL COMMENT '币种',
    effective_from             DATETIME     NOT NULL COMMENT '生效时间(含)',
    effective_to               DATETIME     COMMENT '失效时间(不含)',
    rate_table_code            VARCHAR(64)  COMMENT '费率表编码',
    rate_table_version         VARCHAR(32)  COMMENT '费率表版本',
    rate_dimension_keys_json   TEXT         COMMENT '费率维度(JSON)',
    feature_contract_id        VARCHAR(64)  COMMENT '特征契约ID',
    feature_contract_version   VARCHAR(32)  COMMENT '特征契约版本',
    feature_requirements_json  TEXT         NOT NULL COMMENT '特征需求快照(JSON)',
    artifact_code              VARCHAR(64)  COMMENT '规则工件编码',
    artifact_version           VARCHAR(32)  COMMENT '规则工件版本',
    input_schema_version       VARCHAR(32)  COMMENT '规则输入Schema版本',
    artifact_hash              VARCHAR(64)  COMMENT '规则工件SHA-256',
    rounding_scale             INT          NOT NULL COMMENT '金额精度',
    rounding_mode              VARCHAR(32)  NOT NULL COMMENT '舍入模式',
    content_hash               VARCHAR(64)  NOT NULL COMMENT '审批内容SHA-256',
    test_case_count            INT          NOT NULL COMMENT '测试用例数',
    tenant_id                  VARCHAR(32)  NOT NULL COMMENT '租户ID',
    create_time                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (plan_id),
    UNIQUE KEY uk_product_pricing_plan_version (tenant_id, product_id, plan_version),
    KEY idx_product_pricing_plan_effective (tenant_id, product_id, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product版本化定价方案';
--rollback DROP TABLE IF EXISTS t_product_pricing_plan;

--changeset weisun:product-pricing-plan-2
CREATE TABLE IF NOT EXISTS t_product_pricing_test_case (
    case_id                VARCHAR(36)   NOT NULL COMMENT '测试用例ID',
    plan_id                VARCHAR(36)   NOT NULL COMMENT '定价方案ID',
    case_code              VARCHAR(64)   NOT NULL COMMENT '用例编码',
    description            VARCHAR(255)  COMMENT '用例说明',
    business_time          DATETIME      NOT NULL COMMENT '业务时点',
    sum_insured            DECIMAL(20,2) NOT NULL COMMENT '保额',
    age                    INT           NOT NULL COMMENT '年龄',
    gender                 VARCHAR(8)    NOT NULL COMMENT '性别',
    payment_term_years     INT           NOT NULL COMMENT '缴费期',
    coverage_term_years    INT           NOT NULL COMMENT '保障期',
    payment_periods        INT           NOT NULL COMMENT '缴费期数',
    request_snapshot_json  LONGTEXT      NOT NULL COMMENT '业务请求快照(JSON)',
    expected_premium       DECIMAL(20,8) NOT NULL COMMENT '期望保费',
    tolerance              DECIMAL(20,8) NOT NULL COMMENT '允许误差',
    tenant_id              VARCHAR(32)   NOT NULL COMMENT '租户ID',
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (case_id),
    UNIQUE KEY uk_product_pricing_test_case (tenant_id, plan_id, case_code),
    CONSTRAINT fk_product_pricing_test_case_plan FOREIGN KEY (plan_id)
        REFERENCES t_product_pricing_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product定价方案发布回归用例';
--rollback DROP TABLE IF EXISTS t_product_pricing_test_case;
