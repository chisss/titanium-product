package com.titanium.product.query.result;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 责任级保额档次配置查询结果（CQRS 读侧 DTO）
 * <p>
 * 表达<b>一条责任</b>的保额档次配置，作为查询返回的稳定契约，与写侧值对象
 * {@code CoverageSumInsuredConfig} 解耦，不泄漏持久化细节。
 * </p>
 * <p>
 * 本对象是承保前置链路（G02 投保单录入）的输入契约：录入侧据 {@code allowCustomAmount} 与
 * {@code tiers} 裁决一条录入保额属于「选档」还是「自由填」（判定实现见写侧值对象
 * {@code CoverageSumInsuredConfig#resolveSelection}，读侧只提供数据、不复制判定逻辑）。
 * </p>
 */
@Getter
@Setter
public class CoverageSumInsuredConfigQueryResult {

    /** 责任编码（档次归属键，同一产品下唯一） */
    private String coverageCode;

    /** 档位清单（按保额严格递增） */
    private List<CoverageSumInsuredTierResult> tiers;

    /** 允许的保额区间下界 */
    private BigDecimal minSumInsured;

    /** 允许的保额区间上界 */
    private BigDecimal maxSumInsured;

    /** 是否允许录入档位之外、区间之内的保额 */
    private Boolean allowCustomAmount;

    /** 默认档位编码（须存在于 {@link #tiers} 中） */
    private String defaultTierCode;
}
