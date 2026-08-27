--liquibase formatted sql
--changeset weisun:product-pricing-1
CREATE TABLE IF NOT EXISTS t_product_rate_table (
    table_id              VARCHAR(36)  NOT NULL COMMENT '费率表ID',
    product_id            VARCHAR(36)  NOT NULL COMMENT '所属产品ID',
    table_code            VARCHAR(64)  NOT NULL COMMENT '费率表编码',
    table_version         VARCHAR(32)  NOT NULL COMMENT '不可变费率表版本',
    status                VARCHAR(16)  NOT NULL COMMENT '状态(DRAFT/PUBLISHED/RETIRED)',
    rate_unit             VARCHAR(32)  NOT NULL COMMENT '费率单位',
    currency              VARCHAR(3)   NOT NULL COMMENT '币种',
    effective_from        DATETIME     NOT NULL COMMENT '生效时间(含)',
    effective_to          DATETIME     COMMENT '失效时间(不含)',
    dimension_keys_json   TEXT         NOT NULL COMMENT '启用的维度编码(JSON)',
    content_hash          VARCHAR(64)  NOT NULL COMMENT '发布内容SHA-256',
    row_count             BIGINT       NOT NULL COMMENT '发布费率行数',
    tenant_id             VARCHAR(32)  NOT NULL COMMENT '租户ID',
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (table_id),
    UNIQUE KEY uk_product_rate_table_version (tenant_id, product_id, table_code, table_version),
    KEY idx_product_rate_table_effective (tenant_id, product_id, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product费率表元数据';
--rollback DROP TABLE IF EXISTS t_product_rate_table;

--changeset weisun:product-pricing-2
CREATE TABLE IF NOT EXISTS t_product_rate_table_row (
    row_id                  VARCHAR(36)   NOT NULL COMMENT '费率行ID',
    table_id                VARCHAR(36)   NOT NULL COMMENT '费率表ID',
    dimension_hash          VARCHAR(64)   NOT NULL COMMENT '规范化维度SHA-256',
    age_from                INT           COMMENT '年龄下界(含)',
    age_to_exclusive        INT           COMMENT '年龄上界(不含)',
    gender                  VARCHAR(8)    COMMENT '性别(M/F/ALL)',
    payment_term_years      INT           COMMENT '缴费期年数(null为通配)',
    coverage_term_years     INT           COMMENT '保障期年数(null为通配)',
    rate                    DECIMAL(18,8) NOT NULL COMMENT '费率或固定金额',
    minimum_premium         DECIMAL(18,2) COMMENT '最低保费',
    maximum_premium         DECIMAL(18,2) COMMENT '最高保费',
    tenant_id               VARCHAR(32)   NOT NULL COMMENT '租户ID',
    create_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (row_id),
    UNIQUE KEY uk_product_rate_row_dimension (tenant_id, table_id, dimension_hash),
    KEY idx_product_rate_row_match
        (tenant_id, table_id, gender, payment_term_years, coverage_term_years, age_from, age_to_exclusive),
    CONSTRAINT fk_product_rate_row_table FOREIGN KEY (table_id)
        REFERENCES t_product_rate_table (table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product不可变费率表行';
--rollback DROP TABLE IF EXISTS t_product_rate_table_row;
