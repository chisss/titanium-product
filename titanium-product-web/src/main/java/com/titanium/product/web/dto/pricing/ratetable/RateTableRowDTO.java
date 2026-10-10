package com.titanium.product.web.dto.pricing.ratetable;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 费率行导入请求。
 * <p>
 * 每个字段对应 {@link com.titanium.metadata.enums.product.RateDimensionKey} 中的一个维度：
 * 文本型留空表示通配，区间型留空表示该侧不限。{@code dimensionKeys} 里声明了某个维度、
 * 但行上该维度留空时，表示这一行在该维度上<b>不设限</b>。
 * </p>
 */
public record RateTableRowDTO(
        /** 年龄下界（含），留空表示不限制 */
        @Min(0) Integer ageFrom,
        /** 年龄上界（不含），留空表示不限制 */
        @Min(1) Integer ageToExclusive,
        /** 性别，M/F/ALL，留空等同 ALL */
        String gender,
        /** 缴费期年数，留空表示通配 */
        @Min(1) Integer paymentTermYears,
        /** 保障期年数，留空表示通配 */
        @Min(1) Integer coverageTermYears,
        /** 费率或固定金额，必填 */
        @NotNull @DecimalMin("0") BigDecimal rate,
        /** 最低保费 */
        @DecimalMin("0") BigDecimal minimumPremium,
        /** 最高保费 */
        @DecimalMin("0") BigDecimal maximumPremium,
        /** 职业类别，留空/ALL 表示通配 */
        String occupationClass,
        /** 地区代码，留空/ALL 表示通配 */
        String region,
        /** 车型代码，留空/ALL 表示通配 */
        String vehicleType,
        /** 保额下界（含），留空表示不限制 */
        @DecimalMin("0") BigDecimal sumInsuredFrom,
        /** 保额上界（不含），留空表示不限制 */
        @DecimalMin("0") BigDecimal sumInsuredToExclusive,
        /** 吸烟情况，留空/ALL 表示通配 */
        String smoking,
        /** 社保情况，留空/ALL 表示通配 */
        String socialInsurance,
        /** 免赔额下界（含），留空表示不限制 */
        @DecimalMin("0") BigDecimal deductibleFrom,
        /** 免赔额上界（不含），留空表示不限制 */
        @DecimalMin("0") BigDecimal deductibleToExclusive,
        // ── 以下 7 个维度为 G01-R3-F1 补齐（2026-09-26）──
        // 🔴 它们原已存在于现网费率表数据，但此前 DTO 未承接：Spring 默认忽略未知 JSON 属性，
        //    前端提交这几个维度会被**静默丢弃**（落库恒 NULL），编译与单测都不报错。
        /** 员工人数，留空表示通配 */
        @Min(0) Integer employeeCount,
        /** 工资总额下界（含），留空表示不限制 */
        @DecimalMin("0") BigDecimal payrollAmountFrom,
        /** 工资总额上界（不含），留空表示不限制 */
        @DecimalMin("0") BigDecimal payrollAmountToExclusive,
        /** 财产用途，留空/ALL 表示通配 */
        String propertyUsage,
        /** 财产价值下界（含），留空表示不限制 */
        @DecimalMin("0") BigDecimal propertyValueFrom,
        /** 财产价值上界（不含），留空表示不限制 */
        @DecimalMin("0") BigDecimal propertyValueToExclusive,
        /** 占用性质，留空/ALL 表示通配 */
        String occupancyType,
        /** 消防等级，留空/ALL 表示通配 */
        String fireProtectionLevel,
        /** 无赔款优待系数，留空/ALL 表示通配 */
        String ncd) {
}
