package com.titanium.product.port.claim;

import com.titanium.product.valueobject.pricing.ncd.NcdLookbackWindow;

/**
 * 历史赔案取数查询条件
 * <p>
 * 🔴 {@code vin} 与 {@code subjectId} **至少提供一个**（VIN 优先）：VIN 是跨保单的车辆身份，
 * 续保换单后 policyId/subjectId 均变、唯 VIN 不变；subjectId 是保单内标的标识，作兜底路径。
 * 两者皆空属调用方错误（{@code NCD_SUBJECT_IDENTIFIER_REQUIRED}），**不得**当成「查无赔案」。
 * </p>
 *
 * @param vin        车辆识别代号（与 subjectId 至少提供一个）
 * @param subjectId  标的标识（保单内唯一，VIN 缺失时的兜底路径）
 * @param window     出险日期统计区间（日粒度半开，含止）
 */
public record ClaimHistoryQuery(String vin, String subjectId, NcdLookbackWindow window) {
}
