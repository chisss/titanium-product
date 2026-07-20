package com.titanium.product.web.dto;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.metadata.enums.insurance.InsuranceProductType;

import lombok.Data;

/**
 * 配置寿险产品规格请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上，承载寿险产品专属规格：险种三级分类、可投保年龄范围、保额范围、缴费期选项、保障期选项。
 * 由 {@code ProductWebMapper} 翻译为领域值对象 {@code LifeProductSpec}，再经应用层门面派发
 * {@code ConfigureLifeProductCommand}。
 * </p>
 */
@Data
public class ConfigureLifeProductDTO {

    /** 险种三级分类（定期寿/终身寿/两全/年金） */
    private InsuranceProductType productType;

    /** 可投保年龄范围 */
    private AgeRangeRequest entryAgeRange;

    /** 保额范围 */
    private SumInsuredRangeRequest sumInsuredRange;

    /** 缴费期选项列表 */
    private List<PremiumTermOptionRequest> premiumTermOptions;

    /** 保障期选项列表 */
    private List<CoverageTermOptionRequest> coverageTermOptions;

    /**
     * 可投保年龄范围请求。
     */
    @Data
    public static class AgeRangeRequest {
        /** 最小投保年龄（含） */
        private int minAge;
        /** 最大投保年龄（含） */
        private int maxAge;
    }

    /**
     * 保额范围请求。
     */
    @Data
    public static class SumInsuredRangeRequest {
        /** 最低基本保额（含） */
        private BigDecimal minSumInsured;
        /** 最高基本保额（含） */
        private BigDecimal maxSumInsured;
    }

    /**
     * 缴费期选项请求。
     */
    @Data
    public static class PremiumTermOptionRequest {
        /** 缴费年数（0 表示趸缴） */
        private int years;
        /** 缴至年龄（与 years 二选一，null 表示按年数缴费） */
        private Integer toAge;
        /** 选项描述（如"趸缴""20年缴""缴至60岁"） */
        private String description;
    }

    /**
     * 保障期选项请求。
     */
    @Data
    public static class CoverageTermOptionRequest {
        /** 保障年数（0 表示终身） */
        private int years;
        /** 保至年龄（与 years 二选一，null 表示按年数保障） */
        private Integer toAge;
        /** 是否终身保障 */
        private boolean wholeLife;
        /** 选项描述（如"保20年""保至70岁""终身"） */
        private String description;
    }
}
