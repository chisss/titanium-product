package com.titanium.product.api.response;

import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 保障期间配置响应（api 契约，镜像领域值对象 CoveragePeriodConfig）。
 *
 * @param periodType 保障期间类型（FIXED_TERM/WHOLE_LIFE/CUSTOM）
 * @param fixedTermOptions 固定期限选项（如[1,5,10,20,30]年）
 * @param periodUnit 期间单位（YEAR/MONTH/DAY）
 * @param minPeriod 最小期限
 * @param maxPeriod 最大期限
 */
public record CoveragePeriodConfigResponse(ProductEnum.CoveragePeriodType periodType, List<Integer> fixedTermOptions,
                                           ProductEnum.PeriodUnit periodUnit, Integer minPeriod, Integer maxPeriod) {
}
