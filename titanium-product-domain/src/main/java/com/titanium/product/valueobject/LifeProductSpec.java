package com.titanium.product.valueobject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.titanium.metadata.enums.insurance.InsuranceProductType;

/**
 * 寿险产品规格值对象（寿险产品专属规格建模）
 * <p>
 * 定义寿险产品（定期寿/终身寿/两全/年金）的核保与投保规格边界：可投保年龄范围、保额范围、
 * 缴费期与保障期选项。作为 {@code ProductTemplate} 的行为配置字段，经
 * {@code ConfigureLifeProductCommand} 写入，供投保时校验被保人年龄/保额是否在产品允许区间、
 * 供计费时确定缴费期约束。替代原先将寿险规格泛化塞入通用 {@code InsureCondition}/{@code PaymentConfig}
 * 的做法，让寿险规格成为一等建模对象。
 * </p>
 *
 * @param productType 险种三级分类（定期寿/终身寿/两全/年金）
 * @param entryAgeRange 可投保年龄范围（被保人投保时年龄边界）
 * @param sumInsuredRange 保额范围（基本保额上下限）
 * @param premiumTermOptions 缴费期选项列表（如趸缴/5/10/20 年缴、缴至某年龄）
 * @param coverageTermOptions 保障期选项列表（如保 10/20/30 年、保至某年龄、终身）
 */
public record LifeProductSpec(InsuranceProductType productType, AgeRange entryAgeRange,
                              SumInsuredRange sumInsuredRange, List<PremiumTermOption> premiumTermOptions,
                              List<CoverageTermOption> coverageTermOptions)
        implements
            Serializable {

    public LifeProductSpec {
        premiumTermOptions = premiumTermOptions == null ? List.of() : List.copyOf(premiumTermOptions);
        coverageTermOptions = coverageTermOptions == null ? List.of() : List.copyOf(coverageTermOptions);
    }

    /**
     * 校验被保人投保年龄是否在允许范围内。
     *
     * @param age 被保人投保年龄
     * @return 在范围内返回 {@code true}
     */
    public boolean isEntryAgeAllowed(int age) {
        return entryAgeRange != null && entryAgeRange.contains(age);
    }

    /**
     * 校验申请保额是否在允许范围内。
     *
     * @param sumInsured 申请基本保额
     * @return 在范围内返回 {@code true}
     */
    public boolean isSumInsuredAllowed(BigDecimal sumInsured) {
        return sumInsuredRange != null && sumInsuredRange.contains(sumInsured);
    }

    /**
     * 校验缴费期是否为产品允许的选项之一。
     *
     * @param years 缴费年数（趸缴以 0 表示）
     * @return 允许返回 {@code true}
     */
    public boolean isPremiumTermAllowed(int years) {
        return premiumTermOptions.stream().anyMatch(opt -> opt.years() == years);
    }

    /**
     * 可投保年龄范围。
     *
     * @param minAge 最小投保年龄（含）
     * @param maxAge 最大投保年龄（含）
     */
    public record AgeRange(int minAge, int maxAge) implements Serializable {
        public AgeRange {
            if (minAge < 0 || maxAge < minAge) {
                throw new IllegalArgumentException("投保年龄范围非法: [" + minAge + "," + maxAge + "]");
            }
        }

        /**
         * 年龄是否落在范围内（闭区间）。
         *
         * @param age 年龄
         * @return 在范围内返回 {@code true}
         */
        public boolean contains(int age) {
            return age >= minAge && age <= maxAge;
        }
    }

    /**
     * 保额范围。
     *
     * @param minSumInsured 最低基本保额（含）
     * @param maxSumInsured 最高基本保额（含）
     */
    public record SumInsuredRange(BigDecimal minSumInsured, BigDecimal maxSumInsured) implements Serializable {
        public SumInsuredRange {
            if (minSumInsured == null || maxSumInsured == null
                    || minSumInsured.compareTo(BigDecimal.ZERO) < 0
                    || maxSumInsured.compareTo(minSumInsured) < 0) {
                throw new IllegalArgumentException("保额范围非法");
            }
        }

        /**
         * 保额是否落在范围内（闭区间）。
         *
         * @param sumInsured 保额
         * @return 在范围内返回 {@code true}
         */
        public boolean contains(BigDecimal sumInsured) {
            return sumInsured != null && sumInsured.compareTo(minSumInsured) >= 0
                    && sumInsured.compareTo(maxSumInsured) <= 0;
        }
    }

    /**
     * 缴费期选项。
     *
     * @param years 缴费年数（0 表示趸缴）
     * @param toAge 缴至年龄（与 years 二选一，null 表示按年数缴费）
     * @param description 选项描述（如"趸缴""20年缴""缴至60岁"）
     */
    public record PremiumTermOption(int years, Integer toAge, String description) implements Serializable {
        /**
         * 是否趸缴（一次性缴清）。
         *
         * @return 趸缴返回 {@code true}
         */
        public boolean isSinglePayment() {
            return years == 0 && toAge == null;
        }
    }

    /**
     * 保障期选项。
     *
     * @param years 保障年数（0 表示终身）
     * @param toAge 保至年龄（与 years 二选一，null 表示按年数保障）
     * @param wholeLife 是否终身保障
     * @param description 选项描述（如"保20年""保至70岁""终身"）
     */
    public record CoverageTermOption(int years, Integer toAge, boolean wholeLife, String description)
            implements Serializable {
    }

    /**
     * 便捷构造：仅指定核心边界，选项列表可后续补充。
     *
     * @param productType 险种三级分类
     * @param minAge 最小投保年龄
     * @param maxAge 最大投保年龄
     * @param minSumInsured 最低保额
     * @param maxSumInsured 最高保额
     * @return 寿险产品规格
     */
    public static LifeProductSpec of(InsuranceProductType productType, int minAge, int maxAge,
                                     BigDecimal minSumInsured, BigDecimal maxSumInsured) {
        return new LifeProductSpec(productType, new AgeRange(minAge, maxAge),
                new SumInsuredRange(minSumInsured, maxSumInsured), new ArrayList<>(), new ArrayList<>());
    }
}
