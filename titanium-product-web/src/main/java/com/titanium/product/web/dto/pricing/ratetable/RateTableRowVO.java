package com.titanium.product.web.dto.pricing.ratetable;

import java.math.BigDecimal;

/**
 * 费率行响应。
 * <p>
 * 字段与 {@link com.titanium.metadata.enums.product.RateDimensionKey} 一一对应：文本型为单值
 * （{@code null} 即通配），区间型为 {@code XxxFrom}（含）/ {@code XxxToExclusive}（不含）两列，
 * 端点 {@code null} 表示该侧不限。{@code dimensionHash} 是全部维度取值的稳定摘要，供前端做行去重提示。
 * </p>
 */
public record RateTableRowVO(
        /** 费率行ID */
        String rowId,
        /** 年龄下界（含） */
        Integer ageFrom,
        /** 年龄上界（不含） */
        Integer ageToExclusive,
        /** 性别 */
        String gender,
        /** 缴费期年数 */
        Integer paymentTermYears,
        /** 保障期年数 */
        Integer coverageTermYears,
        /** 费率或固定金额 */
        BigDecimal rate,
        /** 最低保费 */
        BigDecimal minimumPremium,
        /** 最高保费 */
        BigDecimal maximumPremium,
        /** 职业类别 */
        String occupationClass,
        /** 地区代码 */
        String region,
        /** 车型代码 */
        String vehicleType,
        /** 保额下界（含） */
        BigDecimal sumInsuredFrom,
        /** 保额上界（不含） */
        BigDecimal sumInsuredToExclusive,
        /** 吸烟情况 */
        String smoking,
        /** 社保情况 */
        String socialInsurance,
        /** 免赔额下界（含） */
        BigDecimal deductibleFrom,
        /** 免赔额上界（不含） */
        BigDecimal deductibleToExclusive,
        /** 员工人数 */
        Integer employeeCount,
        /** 工资总额下界（含） */
        BigDecimal payrollAmountFrom,
        /** 工资总额上界（不含） */
        BigDecimal payrollAmountToExclusive,
        /** 财产用途 */
        String propertyUsage,
        /** 财产价值下界（含） */
        BigDecimal propertyValueFrom,
        /** 财产价值上界（不含） */
        BigDecimal propertyValueToExclusive,
        /** 占用性质 */
        String occupancyType,
        /** 消防等级 */
        String fireProtectionLevel,
        /** 无赔款优待系数 */
        String ncd,
        /** 维度摘要（SHA-256 十六进制） */
        String dimensionHash) {
}
