-- 创建产品表
        CREATE TABLE t_product (
        product_id VARCHAR(36) NOT NULL,
        product_name VARCHAR(100) NOT NULL,
        form VARCHAR(20) NOT NULL,
        insurance_type VARCHAR(20) NOT NULL,
        version VARCHAR(10) NOT NULL,
        status VARCHAR(20) NOT NULL,
        effective_time DATETIME,
        invalid_time DATETIME,
        original_product_id VARCHAR(36),
        tenant_id VARCHAR(36) NOT NULL,
        created_at DATETIME NOT NULL,
        created_by VARCHAR(50) NOT NULL,
        updated_at DATETIME NOT NULL,
        updated_by VARCHAR(50) NOT NULL,
        CONSTRAINT pk_product PRIMARY KEY (product_id)
        );

        -- 创建产品表索引
        CREATE INDEX idx_product_tenant_id ON t_product (tenant_id);
        CREATE INDEX idx_product_status ON t_product (status);
        CREATE INDEX idx_product_insurance_type ON t_product (insurance_type);
        CREATE INDEX idx_product_original_id ON t_product (original_product_id);

        -- 创建产品条款关联表
        CREATE TABLE t_product_clause_rel (
        id BIGINT AUTO_INCREMENT,
        product_id VARCHAR(36) NOT NULL,
        clause_id VARCHAR(36) NOT NULL,
        clause_version VARCHAR(10) NOT NULL,
        is_main_clause BOOLEAN NOT NULL DEFAULT FALSE,
        tenant_id VARCHAR(36) NOT NULL,
        created_at DATETIME NOT NULL,
        created_by VARCHAR(50) NOT NULL,
        updated_at DATETIME NOT NULL,
        updated_by VARCHAR(50) NOT NULL,
        CONSTRAINT pk_product_clause_rel PRIMARY KEY (id)
        );

        -- 创建产品条款关联表索引
        CREATE INDEX idx_product_clause_rel_product_id ON t_product_clause_rel (product_id);
        CREATE INDEX idx_product_clause_rel_clause_id ON t_product_clause_rel (clause_id);
        CREATE INDEX idx_product_clause_rel_tenant_id ON t_product_clause_rel (tenant_id);