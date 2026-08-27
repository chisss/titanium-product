--liquibase formatted sql

--changeset weisun:product-actuarial-charge-component-1
CREATE TABLE IF NOT EXISTS t_product_charge_component (
    component_id        VARCHAR(36)  NOT NULL COMMENT '费用项ID',
    product_id          VARCHAR(36)  NOT NULL COMMENT '产品ID',
    component_code      VARCHAR(64)  NOT NULL COMMENT '费用项编码',
    component_version   VARCHAR(32)  NOT NULL COMMENT '费用项版本',
    component_name      VARCHAR(128) NOT NULL COMMENT '费用项名称',
    description         VARCHAR(500) COMMENT '费用项说明',
    category            VARCHAR(32)  NOT NULL COMMENT '费用业务分类',
    amount_channel      VARCHAR(32)  NOT NULL COMMENT 'CUSTOMER_PRICE/INTERNAL_COST',
    direction           VARCHAR(16)  NOT NULL COMMENT 'DEBIT/CREDIT',
    payer_type          VARCHAR(32)  NOT NULL COMMENT '费用承担方',
    calculation_source  VARCHAR(32)  NOT NULL COMMENT '计算来源',
    accounting_class    VARCHAR(64)  NOT NULL COMMENT '账务分类',
    customer_visible    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '客户是否可见',
    effective_from      DATETIME     NOT NULL COMMENT '生效时间(含)',
    effective_to        DATETIME     COMMENT '失效时间(不含)',
    tenant_id           VARCHAR(32)  NOT NULL COMMENT '租户ID',
    status              VARCHAR(16)  NOT NULL COMMENT '生命周期状态',
    content_hash        VARCHAR(64)  NOT NULL COMMENT '内容SHA-256',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (component_id),
    UNIQUE KEY uk_product_charge_component (tenant_id, product_id, component_code, component_version),
    KEY idx_product_charge_component_status (tenant_id, product_id, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product版本化费用项目录';
--rollback DROP TABLE IF EXISTS t_product_charge_component;

--changeset weisun:product-actuarial-calculation-model-1
CREATE TABLE IF NOT EXISTS t_product_calculation_model (
    model_id          VARCHAR(36)  NOT NULL COMMENT '计算模型ID',
    product_id        VARCHAR(36)  NOT NULL COMMENT '产品ID',
    model_code        VARCHAR(64)  NOT NULL COMMENT '模型编码',
    model_version     VARCHAR(32)  NOT NULL COMMENT '模型版本',
    model_name        VARCHAR(128) NOT NULL COMMENT '模型名称',
    description       VARCHAR(500) COMMENT '模型说明',
    currency          VARCHAR(3)   NOT NULL COMMENT '币种',
    effective_from    DATETIME     NOT NULL COMMENT '生效时间(含)',
    effective_to      DATETIME     COMMENT '失效时间(不含)',
    tenant_id         VARCHAR(32)  NOT NULL COMMENT '租户ID',
    status            VARCHAR(16)  NOT NULL COMMENT '生命周期状态',
    content_hash      VARCHAR(64)  NOT NULL COMMENT '内容SHA-256',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (model_id),
    UNIQUE KEY uk_product_calculation_model (tenant_id, product_id, model_code, model_version),
    KEY idx_product_calculation_model_status (tenant_id, product_id, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product版本化结构化计算模型';
--rollback DROP TABLE IF EXISTS t_product_calculation_model;

--changeset weisun:product-actuarial-calculation-node-1
CREATE TABLE IF NOT EXISTS t_product_calculation_node (
    node_id            VARCHAR(36)   NOT NULL COMMENT '节点ID',
    model_id           VARCHAR(36)   NOT NULL COMMENT '计算模型ID',
    node_code          VARCHAR(64)   NOT NULL COMMENT '节点编码',
    node_name          VARCHAR(128)  NOT NULL COMMENT '节点名称',
    node_type          VARCHAR(16)   NOT NULL COMMENT '节点类型',
    operator_type      VARCHAR(32)   NOT NULL COMMENT '运算符',
    component_code     VARCHAR(64)   COMMENT '费用项编码',
    component_version  VARCHAR(32)   COMMENT '费用项版本',
    parameter_value    DECIMAL(20,8) COMMENT '固定金额或比例参数',
    execution_order    INT           NOT NULL COMMENT '同层稳定执行顺序',
    PRIMARY KEY (node_id),
    UNIQUE KEY uk_product_calculation_node (model_id, node_code),
    CONSTRAINT fk_product_calculation_node_model FOREIGN KEY (model_id)
        REFERENCES t_product_calculation_model (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product计算模型节点';
--rollback DROP TABLE IF EXISTS t_product_calculation_node;

--changeset weisun:product-actuarial-calculation-edge-1
CREATE TABLE IF NOT EXISTS t_product_calculation_edge (
    edge_id         VARCHAR(36) NOT NULL COMMENT '依赖边ID',
    model_id        VARCHAR(36) NOT NULL COMMENT '计算模型ID',
    from_node_code  VARCHAR(64) NOT NULL COMMENT '前序节点编码',
    to_node_code    VARCHAR(64) NOT NULL COMMENT '后序节点编码',
    PRIMARY KEY (edge_id),
    UNIQUE KEY uk_product_calculation_edge (model_id, from_node_code, to_node_code),
    CONSTRAINT fk_product_calculation_edge_model FOREIGN KEY (model_id)
        REFERENCES t_product_calculation_model (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product计算模型依赖边';
--rollback DROP TABLE IF EXISTS t_product_calculation_edge;

--changeset weisun:product-pricing-plan-calculation-model-ref-1
ALTER TABLE t_product_pricing_plan
    ADD COLUMN calculation_model_code VARCHAR(64) COMMENT '结构化计算模型编码' AFTER artifact_hash,
    ADD COLUMN calculation_model_version VARCHAR(32) COMMENT '结构化计算模型版本' AFTER calculation_model_code,
    ADD COLUMN calculation_model_hash VARCHAR(64) COMMENT '结构化计算模型SHA-256' AFTER calculation_model_version;
--rollback ALTER TABLE t_product_pricing_plan DROP COLUMN calculation_model_hash, DROP COLUMN calculation_model_version, DROP COLUMN calculation_model_code;

--changeset weisun:product-premium-calculation-v2-totals-1
CREATE TABLE IF NOT EXISTS t_product_calculation_total (
    calculation_id       VARCHAR(36)   NOT NULL COMMENT '确认计算ID',
    premium_subtotal     DECIMAL(20,8) NOT NULL COMMENT '客户保费小计',
    tax_and_levy_total   DECIMAL(20,8) NOT NULL COMMENT '税费及征费合计',
    customer_payable     DECIMAL(20,8) NOT NULL COMMENT '客户应付总额',
    internal_cost_total  DECIMAL(20,8) NOT NULL COMMENT '内部成本合计',
    PRIMARY KEY (calculation_id),
    CONSTRAINT fk_product_calculation_total_calculation FOREIGN KEY (calculation_id)
        REFERENCES t_product_premium_calculation (calculation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product确认计算V2总计';
--rollback DROP TABLE IF EXISTS t_product_calculation_total;

--changeset weisun:product-premium-calculation-v2-line-1
CREATE TABLE IF NOT EXISTS t_product_calculation_line (
    calculation_id    VARCHAR(36)   NOT NULL COMMENT '确认计算ID',
    line_id           VARCHAR(64)   NOT NULL COMMENT '费用明细ID',
    component_code    VARCHAR(64)   NOT NULL COMMENT '费用项编码',
    component_version VARCHAR(32)   NOT NULL COMMENT '费用项版本',
    category          VARCHAR(32)   NOT NULL COMMENT '费用分类',
    amount_channel    VARCHAR(32)   NOT NULL COMMENT '金额通道',
    direction         VARCHAR(16)   NOT NULL COMMENT '金额方向',
    payer_type        VARCHAR(32)   NOT NULL COMMENT '费用承担方',
    accounting_class  VARCHAR(64)   NOT NULL COMMENT '账务分类',
    currency          VARCHAR(3)    NOT NULL COMMENT '币种',
    base_amount       DECIMAL(20,8) COMMENT '计算基数',
    rate_value        DECIMAL(20,8) COMMENT '比例',
    calculated_amount DECIMAL(20,8) NOT NULL COMMENT '非负费用金额',
    node_code         VARCHAR(64)   NOT NULL COMMENT '来源节点编码',
    customer_visible  TINYINT(1)    NOT NULL COMMENT '客户是否可见',
    description       VARCHAR(255)  COMMENT '费用说明',
    PRIMARY KEY (calculation_id, line_id),
    KEY idx_product_calculation_line_component (calculation_id, component_code),
    CONSTRAINT fk_product_calculation_line_calculation FOREIGN KEY (calculation_id)
        REFERENCES t_product_premium_calculation (calculation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product确认计算V2结构化费用明细';
--rollback DROP TABLE IF EXISTS t_product_calculation_line;

--changeset weisun:product-premium-calculation-v2-model-evidence-1
ALTER TABLE t_product_premium_calculation
    ADD COLUMN calculation_model_code VARCHAR(64) COMMENT '结构化计算模型编码' AFTER rule_artifact_hash,
    ADD COLUMN calculation_model_version VARCHAR(32) COMMENT '结构化计算模型版本' AFTER calculation_model_code,
    ADD COLUMN calculation_model_hash VARCHAR(64) COMMENT '结构化计算模型SHA-256' AFTER calculation_model_version;
--rollback ALTER TABLE t_product_premium_calculation DROP COLUMN calculation_model_hash, DROP COLUMN calculation_model_version, DROP COLUMN calculation_model_code;
