package com.titanium.product.valueobject.rate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;

/**
 * 不可变费率表行。
 * <p>
 * 每个维度一项（或一对）取值，未配置的维度为 {@code null} 表示<b>通配</b>。维度取值域由
 * {@link com.titanium.metadata.enums.product.RateDimensionKey} 唯一定义：{@code TEXT} 单列文本
 * （null/ALL 通配），{@code INTEGER} 单列整数（精确匹配），{@code RANGE} 两列半开区间
 * {@code [From, ToExclusive)}（null 端点表示该侧不限）。
 * </p>
 *
 * @param rowId 费率行ID
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
public record RateTableRow(
        String rowId,
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
        Integer employeeCount,
        BigDecimal payrollAmountFrom,
        BigDecimal payrollAmountToExclusive,
        String propertyUsage,
        BigDecimal propertyValueFrom,
        BigDecimal propertyValueToExclusive,
        String occupancyType,
        String fireProtectionLevel,
        String ncd) {

    /**
     * 兼容构造器：未配置 G01-R3-F1 补齐的 7 个维度（员工人数/工资总额/财产用途/财产价值/
     * 占用性质/消防等级/无赔款优待系数）时按通配处理。
     * <p>
     * 仅供应「确实不涉及这 7 个维度」的构造点使用。新代码若需这些维度语义，必须调用全参构造器——
     * 用兼容构造器会把它们静默变成通配，使费率表在这些维度上<b>不设限</b>。
     * </p>
     */
    public RateTableRow(
            String rowId,
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
            BigDecimal deductibleToExclusive) {
        this(rowId, ageFrom, ageToExclusive, gender, paymentTermYears, coverageTermYears,
                rate, minimumPremium, maximumPremium, occupationClass, region, vehicleType,
                sumInsuredFrom, sumInsuredToExclusive, smoking, socialInsurance,
                deductibleFrom, deductibleToExclusive,
                null, null, null, null, null, null, null, null, null);
    }

    /**
     * 兼容构造器：未配置本轮新增维度（保额/吸烟/社保/免赔额）时按通配处理。
     * <p>
     * 仅供应「确实不涉及新维度」的构造点使用（既有测试夹具、只覆盖旧维度的代码路径）。
     * 新代码若需要新维度语义，必须调用全参构造器——用兼容构造器会把新维度静默变成通配，
     * 使费率表在该维度上<b>不设限</b>。
     * </p>
     */
    public RateTableRow(
            String rowId,
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
        this(rowId, ageFrom, ageToExclusive, gender, paymentTermYears, coverageTermYears,
                rate, minimumPremium, maximumPremium, occupationClass, region, vehicleType,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    /** 兼容构造器：未配置新维度（职业/地区/车型）时按通配处理。 */
    public RateTableRow(
            String rowId,
            Integer ageFrom,
            Integer ageToExclusive,
            String gender,
            Integer paymentTermYears,
            Integer coverageTermYears,
            BigDecimal rate,
            BigDecimal minimumPremium,
            BigDecimal maximumPremium) {
        this(rowId, ageFrom, ageToExclusive, gender, paymentTermYears, coverageTermYears,
                rate, minimumPremium, maximumPremium, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    public RateTableRow {
        if (rowId == null || rowId.isBlank()) {
            throw invalid("费率行ID不能为空");
        }
        if (ageFrom != null && ageToExclusive != null && ageFrom >= ageToExclusive) {
            throw invalid("年龄上界必须大于下界");
        }
        if ((ageFrom != null && ageFrom < 0) || (ageToExclusive != null && ageToExclusive <= 0)) {
            throw invalid("年龄区间不能为负数");
        }
        gender = normalizeGender(gender);
        if (gender != null && !"M".equals(gender) && !"F".equals(gender) && !"ALL".equals(gender)) {
            throw invalid("性别维度仅支持M、F或ALL");
        }
        occupationClass = normalizeText(occupationClass);
        region = normalizeText(region);
        vehicleType = normalizeText(vehicleType);
        smoking = normalizeText(smoking);
        socialInsurance = normalizeText(socialInsurance);
        propertyUsage = normalizeText(propertyUsage);
        occupancyType = normalizeText(occupancyType);
        fireProtectionLevel = normalizeText(fireProtectionLevel);
        ncd = normalizeText(ncd);
        if ((paymentTermYears != null && paymentTermYears <= 0)
                || (coverageTermYears != null && coverageTermYears <= 0)) {
            throw invalid("缴费期和保障期必须大于0");
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid("费率不能为负数或空值");
        }
        if ((minimumPremium != null && minimumPremium.compareTo(BigDecimal.ZERO) < 0)
                || (maximumPremium != null && maximumPremium.compareTo(BigDecimal.ZERO) < 0)) {
            throw invalid("最低保费和最高保费不能为负数");
        }
        if (minimumPremium != null && maximumPremium != null
                && minimumPremium.compareTo(maximumPremium) > 0) {
            throw invalid("最低保费不能大于最高保费");
        }
        requireValidAmountRange(sumInsuredFrom, sumInsuredToExclusive, "保额");
        requireValidAmountRange(deductibleFrom, deductibleToExclusive, "免赔额");
        requireValidAmountRange(payrollAmountFrom, payrollAmountToExclusive, "工资总额");
        requireValidAmountRange(propertyValueFrom, propertyValueToExclusive, "财产价值");
        if (employeeCount != null && employeeCount < 0) {
            throw invalid("员工人数不能为负数");
        }
    }

    /**
     * 区间型金额维度的通用不变量：两侧均非负，且给定时上界必须严格大于下界（半开区间非空）。
     * <p>
     * 独立成方法而非在紧凑构造器内联两遍——两处内联就是「同一个判断写两份」，改一处必漂移。
     * </p>
     */
    private static void requireValidAmountRange(BigDecimal from, BigDecimal toExclusive, String dimensionName) {
        if (from != null && from.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid(dimensionName + "下界不能为负数");
        }
        if (toExclusive != null && toExclusive.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid(dimensionName + "上界不能为负数");
        }
        if (from != null && toExclusive != null && from.compareTo(toExclusive) >= 0) {
            throw invalid(dimensionName + "上界必须大于下界");
        }
    }

    private static String normalizeGender(String gender) {
        return gender == null || gender.isBlank() ? null : gender.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 判断当前费率行是否匹配输入条件。
     * <p>
     * 语义按维度形态分派：区间型（年龄/保额/免赔额）判「输入点落在行区间内」，行端点为空则该侧不限；
     * 文本型判「行通配（null/ALL）恒匹配，行有值时要求输入同值，输入缺省不匹配（防意外命中）」；
     * 整数型判「行空则通配，否则精确相等」。
     * </p>
     */
    public boolean matches(RateTableCriteria criteria) {
        boolean ageMatched = contains(ageFrom, ageToExclusive, criteria.age());
        boolean genderMatched = gender == null || "ALL".equalsIgnoreCase(gender)
                || gender.equalsIgnoreCase(criteria.gender());
        boolean paymentMatched = paymentTermYears == null
                || paymentTermYears == criteria.paymentTermYears();
        boolean coverageMatched = coverageTermYears == null
                || coverageTermYears == criteria.coverageTermYears();
        boolean occupationMatched = textMatched(occupationClass, criteria.occupationClass());
        boolean regionMatched = textMatched(region, criteria.region());
        boolean vehicleMatched = textMatched(vehicleType, criteria.vehicleType());
        boolean sumInsuredMatched = amountMatched(sumInsuredFrom, sumInsuredToExclusive,
                criteria.sumInsured());
        boolean smokingMatched = textMatched(smoking, criteria.smoking());
        boolean socialInsuranceMatched = textMatched(socialInsurance, criteria.socialInsurance());
        boolean deductibleMatched = amountMatched(deductibleFrom, deductibleToExclusive,
                criteria.deductible());
        boolean employeeMatched = employeeCount == null
                || employeeCount.equals(criteria.employeeCount());
        boolean payrollMatched = amountMatched(payrollAmountFrom, payrollAmountToExclusive,
                criteria.payrollAmount());
        boolean propertyUsageMatched = textMatched(propertyUsage, criteria.propertyUsage());
        boolean propertyValueMatched = amountMatched(propertyValueFrom, propertyValueToExclusive,
                criteria.propertyValue());
        boolean occupancyMatched = textMatched(occupancyType, criteria.occupancyType());
        boolean fireProtectionMatched = textMatched(fireProtectionLevel, criteria.fireProtectionLevel());
        boolean ncdMatched = textMatched(ncd, criteria.ncd());
        return ageMatched && genderMatched && paymentMatched && coverageMatched
                && occupationMatched && regionMatched && vehicleMatched
                && sumInsuredMatched && smokingMatched && socialInsuranceMatched && deductibleMatched
                && employeeMatched && payrollMatched && propertyUsageMatched && propertyValueMatched
                && occupancyMatched && fireProtectionMatched && ncdMatched;
    }

    /** 区间型（整数）：行两侧均可空，输入为点。 */
    private static boolean contains(Integer from, Integer toExclusive, int point) {
        return (from == null || point >= from) && (toExclusive == null || point < toExclusive);
    }

    /**
     * 区间型（金额）：行两侧均可空，输入为点。
     * <p>
     * 🔴 输入为 {@code null}（请求方没提供该维度）时：行若把该维度<b>限定了</b>（任一端非空），
     * 判定为<b>不匹配</b>；行完全通配时才匹配。这与 {@link #textMatched} 的「请求缺省不匹配」
     * 同向——否则「没填保额」会命中任意保额档，把「未知」静默当成「全部」。
     * </p>
     */
    private static boolean amountMatched(BigDecimal from, BigDecimal toExclusive, BigDecimal point) {
        boolean unrestricted = from == null && toExclusive == null;
        if (unrestricted) {
            return true;
        }
        if (point == null) {
            return false;
        }
        return (from == null || point.compareTo(from) >= 0)
                && (toExclusive == null || point.compareTo(toExclusive) < 0);
    }

    /** 文本维度匹配语义：行通配（null/ALL）恒匹配；行有值时要求请求同值，请求缺省不匹配（防意外命中）。 */
    private static boolean textMatched(String rowValue, String criteriaValue) {
        if (rowValue == null || "ALL".equalsIgnoreCase(rowValue)) {
            return true;
        }
        return criteriaValue != null && rowValue.equalsIgnoreCase(criteriaValue);
    }

    /**
     * 返回只包含匹配维度的稳定摘要，用于数据库唯一约束和迁移校验。
     * <p>
     * 🔴 G01-R3-F1 起分量数由 14 增至 23（追加 G01 补齐的 7 个维度）。新增分量一律<b>追加到末尾</b>，
     * 保证同一逻辑行在同版本内哈希稳定；跨版本哈希会变，故唯一约束的语义是「同版本内不重复」——
     * 存量行若经写路径重算会得到新哈希，这不会破坏约束（同一行重算后仍是它自己），
     * 但会让「新旧混存」的表在迁移校验时看到两种哈希形态，迁移脚本须以新算法为准。
     * </p>
     */
    public String dimensionHash() {
        String dimensions = String.join("|",
                nullable(ageFrom), nullable(ageToExclusive), nullable(gender),
                nullable(paymentTermYears), nullable(coverageTermYears),
                nullable(occupationClass), nullable(region), nullable(vehicleType),
                nullable(sumInsuredFrom), nullable(sumInsuredToExclusive),
                nullable(smoking), nullable(socialInsurance),
                nullable(deductibleFrom), nullable(deductibleToExclusive),
                nullable(employeeCount), nullable(payrollAmountFrom), nullable(payrollAmountToExclusive),
                nullable(propertyUsage), nullable(propertyValueFrom), nullable(propertyValueToExclusive),
                nullable(occupancyType), nullable(fireProtectionLevel), nullable(ncd));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(dimensions.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持SHA-256", exception);
        }
    }

    private static String nullable(Object value) {
        return value == null ? "*" : value.toString();
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.PRICING_INPUT_INVALID, detail);
    }
}
