package com.titanium.product.api.request.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 创建产品远程契约 DTO
 * <p>
 * 面向其它微服务的远程创建入参。api 层为对外自包含契约，领域枚举一律以 {@code String} 承载
 * （产品形态/险种类型/产品类别），由 web/provider 的 {@code ProductWebMapper} 翻译为领域命令。
 * 复杂因子（定价因子/险种专属配置）远程契约暂不承载，缺省由聚合根兜底。
 * </p>
 */
@Data
public class CreateProductRequest {

    /** 产品ID（可选，不传则自动生成） */
    private String productId;
    /** 所属产品模板ID（必填，单向引用 ProductTemplate） */
    private String templateId;
    /** 产品代码 */
    private String productCode;
    /** 产品名称 */
    private String productName;
    /** 产品描述 */
    private String productDesc;
    /** 产品形态编码（INDIVIDUAL/GROUP） */
    private String form;
    /** 险种类型编码 */
    private String insuranceType;
    /** 产品类别编码（MAIN/RIDER） */
    private String category;
    /** 销售开始时间 */
    private LocalDateTime saleStartTime;
    /** 销售截止时间 */
    private LocalDateTime saleEndTime;

    /** 投保条件 */
    private InsureConditionInput insureCondition;
    /** 定价基础规则 */
    private PricingRuleInput pricingBasicRule;

    /** 绑定的条款ID列表 */
    private List<String> clauseIds;
    /** 条款版本映射 */
    private Map<String, String> clauseVersionMap;
    /** 主条款ID */
    private String mainClauseId;
    /** 附加险产品ID列表 */
    private List<String> attachProductIds;

    /** 定价模式编码（RATE_TABLE 费率表查询 / ACTUARIAL_FORMULA 精算公式），billing 出单按此分派保费计算路径 */
    private String pricingMode;

    /**
     * 投保条件远程入参（自包含，无领域依赖）
     */
    @Data
    public static class InsureConditionInput {
        /** 最小年龄 */
        private Integer minAge;
        /** 最大年龄 */
        private Integer maxAge;
        /** 禁止职业列表 */
        private List<String> forbiddenOccupations;
        /** 最小团体人数 */
        private Integer minGroupSize;
        /** 最大团体人数 */
        private Integer maxGroupSize;
        /** 健康告知要求 */
        private String healthNotice;
    }

    /**
     * 定价基础规则远程入参（定价类型以 String 承载）
     */
    @Data
    public static class PricingRuleInput {
        /** 定价类型编码（FIXED/AGE/OCCUPATION/COMBINED） */
        private String pricingType;
        /** 基础费率 */
        private BigDecimal baseRate;
        /** 费率计算公式 */
        private String rateFormula;
    }
}
