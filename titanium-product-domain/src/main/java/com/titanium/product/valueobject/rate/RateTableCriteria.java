package com.titanium.product.valueobject.rate;

import java.util.Locale;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;

/**
 * 首期费率表匹配条件。
 *
 * @param age 被保人周岁年龄
 * @param gender 性别，M 或 F
 * @param paymentTermYears 缴费期年数
 * @param coverageTermYears 保障期年数
 */
public record RateTableCriteria(int age, String gender, int paymentTermYears, int coverageTermYears) {

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

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.PRICING_INPUT_INVALID, detail);
    }
}
