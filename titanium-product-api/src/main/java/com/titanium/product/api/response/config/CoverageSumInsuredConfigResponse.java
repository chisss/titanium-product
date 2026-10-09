package com.titanium.product.api.response.config;

import java.math.BigDecimal;
import java.util.List;

/**
 * 责任级保额档次响应（api 契约，镜像领域值对象 CoverageSumInsuredConfig）
 * <p>
 * 供承保前置链路（投保单录入）按产品取「每个责任可填什么保额」。一个产品下每个责任一条，
 * 责任编码为裸字符串——其真伪由产品域在写入时向条款域求证（见 {@code ClauseExistencePort}）。
 * </p>
 *
 * @param coverageCode       责任编码（对应条款域的责任标识，如 HOSPITAL / DEATH）
 * @param tiers              可选的保额档位清单；{@code allowCustomAmount=true} 时仍可填区间内的自由值
 * @param minSumInsured      保额区间下界（含）
 * @param maxSumInsured      保额区间上界（含）
 * @param allowCustomAmount  是否开放自由填（{@code false} 时只能选档，且区间必与档位对齐）
 * @param defaultTierCode    默认档位编码（必须命中 {@code tiers} 中的某一项）
 */
public record CoverageSumInsuredConfigResponse(String coverageCode,
                                               List<CoverageSumInsuredTierResponse> tiers,
                                               BigDecimal minSumInsured,
                                               BigDecimal maxSumInsured,
                                               Boolean allowCustomAmount,
                                               String defaultTierCode) {
}
