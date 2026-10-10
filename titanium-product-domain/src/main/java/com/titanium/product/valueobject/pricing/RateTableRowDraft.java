package com.titanium.product.valueobject.pricing;

import java.math.BigDecimal;

/**
 * 待导入的费率行，不接受调用方指定内部行ID。
 * <p>
 * 每个分量的取值域与存储形态由 {@link com.titanium.metadata.enums.product.RateDimensionKey} 定义；
 * 未配置的维度为 {@code null}，表示该维度<b>通配</b>。
 * </p>
 *
 * @param ageFrom 年龄下界（含），null 表示不限制
 * @param ageToExclusive 年龄上界（不含），null 表示不限制
 * @param gender 性别，M/F/ALL，null 等同 ALL
 * @param paymentTermYears 缴费期年数，null 表示通配
 * @param coverageTermYears 保障期年数，null 表示通配
 * @param rate 费率或固定金额
 * @param minimumPremium 最低保费
 * @param maximumPremium 最高保费
 * @param occupationClass 职业类别（如意外险1-6类），null/ALL 表示通配
 * @param region 地区代码，null/ALL 表示通配
 * @param vehicleType 车型代码，null/ALL 表示通配
 * @param sumInsuredFrom 保额下界（含），null 表示不限制
 * @param sumInsuredToExclusive 保额上界（不含），null 表示不限制
 * @param smoking 吸烟情况，null/ALL 表示通配
 * @param socialInsurance 社保情况，null/ALL 表示通配
 * @param deductibleFrom 免赔额下界（含），null 表示不限制
 * @param deductibleToExclusive 免赔额上界（不含），null 表示不限制
 * @param employeeCount 员工人数，null 表示通配
 * @param payrollAmountFrom 工资总额下界（含），null 表示不限制
 * @param payrollAmountToExclusive 工资总额上界（不含），null 表示不限制
 * @param propertyUsage 财产用途，null/ALL 表示通配
 * @param propertyValueFrom 财产价值下界（含），null 表示不限制
 * @param propertyValueToExclusive 财产价值上界（不含），null 表示不限制
 * @param occupancyType 占用性质，null/ALL 表示通配
 * @param fireProtectionLevel 消防等级，null/ALL 表示通配
 * @param ncd 无赔款优待系数，null/ALL 表示通配
 */
public record RateTableRowDraft(
        Integer ageFrom,
        Integer ageToExclusive,
        String gender,
        Integer paymentTermYears,
        Integer coverageTermYears,
        BigDecimal rate,
        BigDecimal minimumPremium,
        BigDecimal maximumPremium,
        String occupationClass,
        String region,
        String vehicleType,
        BigDecimal sumInsuredFrom,
        BigDecimal sumInsuredToExclusive,
        String smoking,
        String socialInsurance,
        BigDecimal deductibleFrom,
        BigDecimal deductibleToExclusive,
        // G01-R3-F1 补齐（2026-09-26）：以下 7 个维度原已存在于现网费率表数据。
        // 🔴 本 record 是 web 入参到领域行的**唯一**中转载体：此处缺分量，则即便 DTO 承接了取值，
        //    toRateTableRow 也搬不到领域对象上——整条写侧静默丢值。
        Integer employeeCount,
        BigDecimal payrollAmountFrom,
        BigDecimal payrollAmountToExclusive,
        String propertyUsage,
        BigDecimal propertyValueFrom,
        BigDecimal propertyValueToExclusive,
        String occupancyType,
        String fireProtectionLevel,
        String ncd) {

    /** 兼容构造器：未配置本轮新增维度（保额/吸烟/社保/免赔额）时按通配处理。 */
    public RateTableRowDraft(
            Integer ageFrom,
            Integer ageToExclusive,
            String gender,
            Integer paymentTermYears,
            Integer coverageTermYears,
            BigDecimal rate,
            BigDecimal minimumPremium,
            BigDecimal maximumPremium,
            String occupationClass,
            String region,
            String vehicleType) {
        this(ageFrom, ageToExclusive, gender, paymentTermYears, coverageTermYears, rate,
                minimumPremium, maximumPremium, occupationClass, region, vehicleType,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    /** 兼容构造器：未配置职业/地区/车型及本轮新增维度时全部按通配处理。 */
    public RateTableRowDraft(
            Integer ageFrom,
            Integer ageToExclusive,
            String gender,
            Integer paymentTermYears,
            Integer coverageTermYears,
            BigDecimal rate,
            BigDecimal minimumPremium,
            BigDecimal maximumPremium) {
        this(ageFrom, ageToExclusive, gender, paymentTermYears, coverageTermYears, rate,
                minimumPremium, maximumPremium, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }
}
