package com.titanium.product.valueobject.rate;

import java.util.Locale;
import java.util.Map;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;

/**
 * 首期费率表匹配条件。
 *
 * @param age 被保人周岁年龄
 * @param gender 性别，M 或 F
 * @param paymentTermYears 缴费期年数
 * @param coverageTermYears 保障期年数
 * @param occupationClass 职业类别（如意外险1-6类），null 表示未提供
 * @param region 地区代码，null 表示未提供
 * @param vehicleType 车型代码，null 表示未提供
 */
public record RateTableCriteria(
        int age,
        String gender,
        int paymentTermYears,
        int coverageTermYears,
        String occupationClass,
        String region,
        String vehicleType) {

    /** 兼容构造器：未提供新维度（职业/地区/车型）时按缺省处理。 */
    public RateTableCriteria(int age, String gender, int paymentTermYears, int coverageTermYears) {
        this(age, gender, paymentTermYears, coverageTermYears, null, null, null);
    }

    /** 快照键名约定：试算请求 featureSnapshot/requestSnapshot 中携带的可选维度键。 */
    public static final String SNAPSHOT_KEY_OCCUPATION_CLASS = "occupationClass";
    public static final String SNAPSHOT_KEY_REGION = "region";
    public static final String SNAPSHOT_KEY_VEHICLE_TYPE = "vehicleType";

    /**
     * 从基础四要素 + 请求快照构造匹配条件，新维度按约定键名从快照提取。
     */
    public static RateTableCriteria fromSnapshot(
            int age,
            String gender,
            int paymentTermYears,
            int coverageTermYears,
            Map<String, Object> requestSnapshot) {
        return new RateTableCriteria(age, gender, paymentTermYears, coverageTermYears,
                snapshotText(requestSnapshot, SNAPSHOT_KEY_OCCUPATION_CLASS),
                snapshotText(requestSnapshot, SNAPSHOT_KEY_REGION),
                snapshotText(requestSnapshot, SNAPSHOT_KEY_VEHICLE_TYPE));
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
    }

    private static String snapshotText(Map<String, Object> snapshot, String key) {
        if (snapshot == null) {
            return null;
        }
        Object value = snapshot.get(key);
        return value == null ? null : normalizeText(value.toString());
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
