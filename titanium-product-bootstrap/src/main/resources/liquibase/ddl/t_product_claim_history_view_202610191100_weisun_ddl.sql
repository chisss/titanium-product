--liquibase formatted sql

--changeset weisun:product-claim-history-view-202610191100
-- m27-04/PKG-04：产品域历史赔案读模型（NCD 反算本地投影）。
-- 由 claim-created / claim-updated / claim-status-changed / claim-rejected 四个主题投影维护，
-- 替代原先对理赔域 ClaimApi 的同步 Feign 调用（消除 product→claim 反馈边）。
CREATE TABLE IF NOT EXISTS t_product_claim_history_view (
    claim_id VARCHAR(36) NOT NULL COMMENT '赔案ID（理赔域聚合标识，跨域投影主键）',
    tenant_id VARCHAR(32) NOT NULL COMMENT '租户ID',
    vin VARCHAR(64) NULL COMMENT '车辆识别代号（NCD 按车取数的主路径）',
    subject_id VARCHAR(64) NULL COMMENT '标的标识（VIN 缺失时的兜底路径）',
    incident_date DATETIME NULL COMMENT '出险时间（NCD 统计区间过滤列；状态桩可能为空）',
    status VARCHAR(20) NULL COMMENT '赔案状态（ClaimStatus 序列化值，如 PENDING/APPROVED/REJECTED）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投影创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '投影更新时间',
    version BIGINT NULL COMMENT '乐观锁版本',
    PRIMARY KEY (claim_id),
    KEY idx_product_claim_history_tenant_vin (tenant_id, vin, incident_date),
    KEY idx_product_claim_history_tenant_subject (tenant_id, subject_id, incident_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品域历史赔案读模型（NCD 反算本地投影，m27-04/PKG-04）';
--rollback DROP TABLE IF EXISTS t_product_claim_history_view;
