package com.titanium.product.valueobject;

import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 保障期间配置值对象 定义产品的保障期间类型及选项
 *
 * @param periodType 保障期间类型（FIXED_TERM/WHOLE_LIFE/CUSTOM）
 * @param fixedTermOptions 固定期限选项（如[1,5,10,20,30]年）
 * @param periodUnit 期间单位（YEAR/MONTH/DAY）
 * @param minPeriod 最小期限
 * @param maxPeriod 最大期限
 */
public record CoveragePeriodConfig(ProductEnum.CoveragePeriodType periodType, List<Integer> fixedTermOptions,
                                   ProductEnum.PeriodUnit periodUnit, Integer minPeriod, Integer maxPeriod) {
}
