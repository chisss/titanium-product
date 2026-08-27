package com.titanium.product.valueobject;

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
        BigDecimal maximumPremium) {

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
    }

    private static String normalizeGender(String gender) {
        return gender == null || gender.isBlank() ? null : gender.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 判断当前费率行是否匹配输入条件。
     */
    public boolean matches(RateTableCriteria criteria) {
        boolean ageMatched = (ageFrom == null || criteria.age() >= ageFrom)
                && (ageToExclusive == null || criteria.age() < ageToExclusive);
        boolean genderMatched = gender == null || "ALL".equalsIgnoreCase(gender)
                || gender.equalsIgnoreCase(criteria.gender());
        boolean paymentMatched = paymentTermYears == null
                || paymentTermYears == criteria.paymentTermYears();
        boolean coverageMatched = coverageTermYears == null
                || coverageTermYears == criteria.coverageTermYears();
        return ageMatched && genderMatched && paymentMatched && coverageMatched;
    }

    /**
     * 返回只包含匹配维度的稳定摘要，用于数据库唯一约束和迁移校验。
     */
    public String dimensionHash() {
        String dimensions = String.join("|",
                nullable(ageFrom), nullable(ageToExclusive), nullable(gender),
                nullable(paymentTermYears), nullable(coverageTermYears));
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
