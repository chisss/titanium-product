package com.titanium.product.infrastructure.pricing.entity.ncd;

import java.time.LocalDateTime;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** NCD（无赔款优待）规则集持久化实体（t_product_ncd_rule_set）。档位另表 t_product_ncd_rule。 */
@Entity
@Table(name = "t_product_ncd_rule_set")
@Getter
@Setter
public class NcdRuleSetDO {

    @Id
    @Column(name = "rule_set_id", nullable = false, length = 36)
    private String ruleSetId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "rule_set_code", nullable = false, length = 64)
    private String ruleSetCode;

    @Column(name = "rule_set_version", nullable = false, length = 32)
    private String ruleSetVersion;

    /** 历史赔案统计区间长度（月）。🔴 取值口径属 R-3 待确认项，本列只承载配置值、不设默认。 */
    @Column(name = "lookback_months", nullable = false)
    private int lookbackMonths;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ActuarialDefinitionStatus status;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

}
