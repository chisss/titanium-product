package com.titanium.product.domain.valueobject;

import java.util.List;

/**
 * 投保条件值对象 表示产品的投保条件配置，包含年龄范围、职业限制、团险人数限制等
 *
 * @param minAge 年龄范围（个险必填，团险可不填）
 * @param forbiddenOccupations 职业限制（如寿险排除高危职业）
 * @param minGroupSize 团险专属：最小/最大参保人数
 * @param healthNotice 其他条件（如健康告知要求）
 */
public record InsureCondition(Integer minAge, Integer maxAge, List<String> forbiddenOccupations, Integer minGroupSize,
                              Integer maxGroupSize, String healthNotice) {
}
