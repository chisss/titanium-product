package com.titanium.product.infrastructure.pricing.entity.ncd;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** NCD 规则档位持久化实体（t_product_ncd_rule）。 */
@Entity
@Table(name = "t_product_ncd_rule")
@Getter
@Setter
public class NcdRuleDO {

    @Id
    @Column(name = "rule_id", nullable = false, length = 36)
    private String ruleId;

    @Column(name = "rule_set_id", nullable = false, length = 36)
    private String ruleSetId;

    @Column(name = "min_count", nullable = false)
    private Integer minCount;

    /** 次数的闭区间上界；{@code null} 表示无上界（该次数及以上档）。 */
    @Column(name = "max_count")
    private Integer maxCount;

    /** 车型标识；{@code null} 表示通用档（适用全部车型）。 */
    @Column(name = "vehicle_type", length = 64)
    private String vehicleType;

    /**
     * 档位键冗余列（{@code 下界|上界|车型}，无量值以 {@code *} 占位）。
     * <p>
     * 🔴 存在的唯一理由：MySQL 唯一键对 NULL 不去重，{@code max_count}/{@code vehicle_type} 可空，
     * 直接对二者建唯一键**挡不住**「无上界档」这种最常见的重复情形（两行 (3, NULL, NULL) 互不冲突）。
     * 本列非空且经 SHA 无关的规范化拼接，唯一键才真正生效。
     * </p>
     */
    @Column(name = "band_key", nullable = false, length = 128)
    private String bandKey;

    /** NCD 系数。 */
    @Column(name = "coefficient", nullable = false, precision = 18, scale = 8)
    private BigDecimal coefficient;

    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;

}
