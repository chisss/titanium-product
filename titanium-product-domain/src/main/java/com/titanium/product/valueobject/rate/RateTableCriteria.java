package com.titanium.product.valueobject.rate;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import com.titanium.metadata.enums.product.RateDimensionKey;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;

/**
 * 首期费率表匹配条件。
 * <p>
 * 前四个分量是<b>出单必备要素</b>（年龄/性别/缴费期/保障期），恒有值；其余分量是<b>可选维度</b>，
 * 取自试算请求的 {@code requestSnapshot}，{@code null} 表示请求方未提供该维度——语义上等价于
 * 「未知」，因此<b>不会命中</b>把该维度限定了的费率行（详见 {@code RateTableRow#matches}）。
 * </p>
 * <p>
 * 🔴 区间型维度（保额/免赔额）在此是<b>点</b>（{@link BigDecimal} 单值），行侧才是区间。匹配即
 * 「点落区间内」；行两侧端点为空则该侧不限。
 * </p>
 *
 * @param age 被保人周岁年龄
 * @param gender 性别，M 或 F
 * @param paymentTermYears 缴费期年数
 * @param coverageTermYears 保障期年数
 * @param occupationClass 职业类别（如意外险1-6类），null 表示未提供
 * @param region 地区代码，null 表示未提供
 * @param vehicleType 车型代码，null 表示未提供
 * @param sumInsured 保额，null 表示未提供
 * @param smoking 吸烟情况，null 表示未提供
 * @param socialInsurance 社保情况，null 表示未提供
 * @param deductible 免赔额，null 表示未提供
 * @param employeeCount 员工人数，null 表示未提供
 * @param payrollAmount 工资总额，null 表示未提供
 * @param propertyUsage 财产用途，null 表示未提供
 * @param propertyValue 财产价值，null 表示未提供
 * @param occupancyType 占用性质，null 表示未提供
 * @param fireProtectionLevel 消防等级，null 表示未提供
 * @param ncd 无赔款优待系数，null 表示未提供
 */
public record RateTableCriteria(
        int age,
        String gender,
        int paymentTermYears,
        int coverageTermYears,
        String occupationClass,
        String region,
        String vehicleType,
        BigDecimal sumInsured,
        String smoking,
        String socialInsurance,
        BigDecimal deductible,
        Integer employeeCount,
        BigDecimal payrollAmount,
        String propertyUsage,
        BigDecimal propertyValue,
        String occupancyType,
        String fireProtectionLevel,
        String ncd) {

    /** 兼容构造器：未提供新维度（职业/地区/车型）时按缺省处理。 */
    public RateTableCriteria(int age, String gender, int paymentTermYears, int coverageTermYears) {
        this(age, gender, paymentTermYears, coverageTermYears, null, null, null, null, null, null, null);
    }

    /** 兼容构造器：只提供职业/地区/车型三个维度时，其余新维度按缺省处理。 */
    public RateTableCriteria(
            int age,
            String gender,
            int paymentTermYears,
            int coverageTermYears,
            String occupationClass,
            String region,
            String vehicleType) {
        this(age, gender, paymentTermYears, coverageTermYears,
                occupationClass, region, vehicleType, null, null, null, null);
    }

    /**
     * 兼容构造器：未提供 G01-R3-F1 补齐的 7 个维度（员工人数/工资总额/财产用途/财产价值/
     * 占用性质/消防等级/无赔款优待系数）时按缺省处理。
     * <p>
     * 仅供应「确实不涉及这 7 个维度」的构造点使用。新代码若需这些维度语义，必须调用全参构造器——
     * 用兼容构造器会把它们静默变成「未提供」，使费率表在这些维度上<b>不命中</b>（非通配放行）。
     * </p>
     */
    public RateTableCriteria(
            int age,
            String gender,
            int paymentTermYears,
            int coverageTermYears,
            String occupationClass,
            String region,
            String vehicleType,
            BigDecimal sumInsured,
            String smoking,
            String socialInsurance,
            BigDecimal deductible) {
        this(age, gender, paymentTermYears, coverageTermYears, occupationClass, region, vehicleType,
                sumInsured, smoking, socialInsurance, deductible, null, null, null, null, null, null, null);
    }

    /**
     * 从基础四要素 + 请求快照构造匹配条件。
     * <p>
     * 🔴 快照键名<b>不在此处另写字面量</b>，一律取 {@link RateDimensionKey} 的 {@code code}——
     * 该枚举是维度编码的唯一权威（产品域建表校验与 admin 下拉字典同源）。此处再抄一份字符串
     * 就是「同一个判断写两份」，改一处必漂移，且漂移后表现为「费率表配了维度却永不命中」这类
     * 静默失效，不会报错。
     * </p>
     */
    public static RateTableCriteria fromSnapshot(
            int age,
            String gender,
            int paymentTermYears,
            int coverageTermYears,
            Map<String, Object> requestSnapshot) {
        return new RateTableCriteria(age, gender, paymentTermYears, coverageTermYears,
                snapshotText(requestSnapshot, RateDimensionKey.OCCUPATION_CLASS),
                snapshotText(requestSnapshot, RateDimensionKey.REGION),
                snapshotText(requestSnapshot, RateDimensionKey.VEHICLE_TYPE),
                snapshotAmount(requestSnapshot, RateDimensionKey.SUM_INSURED),
                snapshotText(requestSnapshot, RateDimensionKey.SMOKING),
                snapshotText(requestSnapshot, RateDimensionKey.SOCIAL_INSURANCE),
                snapshotAmount(requestSnapshot, RateDimensionKey.DEDUCTIBLE),
                snapshotInt(requestSnapshot, RateDimensionKey.EMPLOYEE_COUNT),
                snapshotAmount(requestSnapshot, RateDimensionKey.PAYROLL_AMOUNT),
                snapshotText(requestSnapshot, RateDimensionKey.PROPERTY_USAGE),
                snapshotAmount(requestSnapshot, RateDimensionKey.PROPERTY_VALUE),
                snapshotText(requestSnapshot, RateDimensionKey.OCCUPANCY_TYPE),
                snapshotText(requestSnapshot, RateDimensionKey.FIRE_PROTECTION_LEVEL),
                snapshotText(requestSnapshot, RateDimensionKey.NCD));
    }

    public RateTableCriteria {
        if (age < 0 || age > 120) {
            throw invalid("年龄必须在 0 到 120 之间");
        }
        gender = normalizeGender(gender);
        if (paymentTermYears <= 0) {
            throw invalid("缴费期必须大于 0");
        }
        if (coverageTermYears <= 0) {
            throw invalid("保障期必须大于 0");
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
        sumInsured = requireNonNegative(sumInsured, "保额");
        deductible = requireNonNegative(deductible, "免赔额");
        payrollAmount = requireNonNegative(payrollAmount, "工资总额");
        propertyValue = requireNonNegative(propertyValue, "财产价值");
        if (employeeCount != null && employeeCount < 0) {
            throw invalid("员工人数不能为负数");
        }
    }

    /** 金额维度入参不得为负——负保额/负免赔额会静默命中某些区间行。 */
    private static BigDecimal requireNonNegative(BigDecimal amount, String dimensionName) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid(dimensionName + "不能为负数");
        }
        return amount;
    }

    /** 按维度键取快照中的文本值。 */
    private static String snapshotText(Map<String, Object> snapshot, RateDimensionKey dimension) {
        Object value = raw(snapshot, dimension);
        return value == null ? null : normalizeText(value.toString());
    }

    /**
     * 按维度键取快照中的金额值。
     * <p>
     * 快照值来自 JSON 反序列化，可能是 {@link Number}（数字字面量）或 {@link String}
     * （带引号的数字）。前者直接 {@code toString()} 交给 {@link BigDecimal}，后者先 trim。
     * 无法解析时抛业务异常而非静默置 null——静默置 null 会让「保额填错了」表现为「按通配行计价」，
     * 出单金额错误却无任何告警。
     * </p>
     */
    private static BigDecimal snapshotAmount(Map<String, Object> snapshot, RateDimensionKey dimension) {
        Object value = raw(snapshot, dimension);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            throw invalid(dimension.getName() + "取值无法解析为金额：" + text);
        }
    }

    /**
     * 按维度键取快照中的整数值。
     * <p>
     * 与 {@link #snapshotAmount} 同族：快照值来自 JSON 反序列化，可能是 {@link Number}
     * 或 {@link String}。无法解析时抛业务异常而非静默置 null——静默置 null 会让「员工人数填错了」
     * 表现为「按通配行计价」，出单金额错误却无任何告警。
     * </p>
     */
    private static Integer snapshotInt(Map<String, Object> snapshot, RateDimensionKey dimension) {
        Object value = raw(snapshot, dimension);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException exception) {
            throw invalid(dimension.getName() + "取值无法解析为整数：" + text);
        }
    }

    private static Object raw(Map<String, Object> snapshot, RateDimensionKey dimension) {
        return snapshot == null ? null : snapshot.get(dimension.getCode());
    }

    private static String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) {
            throw invalid("性别不能为空");
        }
        String normalized = gender.trim().toUpperCase(Locale.ROOT);
        if ("MALE".equals(normalized)) {
            normalized = "M";
        } else if ("FEMALE".equals(normalized)) {
            normalized = "F";
        }
        if (!"M".equals(normalized) && !"F".equals(normalized)) {
            throw invalid("性别仅支持 M 或 F");
        }
        return normalized;
    }

    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.PRICING_INPUT_INVALID, detail);
    }
}
