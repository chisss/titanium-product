package com.titanium.product.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;

import lombok.Data;

/**
 * 创建产品请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上，承载完整产品配置。由 {@code ProductWebMapper} 翻译为领域命令
 * {@code CreateProductCommand}。web 层可依赖 domain 命令与 metadata 枚举。
 * </p>
 */
@Data
public class CreateProductDTO {

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
    /** 产品形态（INDIVIDUAL/GROUP） */
    private ProductEnum.ProductForm form;
    /** 险种类型 */
    private InsuranceProductType insuranceType;
    /** 产品类别（MAIN/RIDER） */
    private ProductEnum.ProductCategory category;
    /** 销售开始时间 */
    private LocalDateTime saleStartTime;
    /** 销售截止时间 */
    private LocalDateTime saleEndTime;

    /** 投保条件 */
    private InsureConditionDTO insureCondition;
    /** 保障期间配置 */
    private Object coveragePeriod;
    /** 缴费方式配置 */
    private Object paymentConfig;
    /** 定价基础规则 */
    private PricingBasicRuleDTO pricingBasicRule;

    /** 绑定的条款ID列表 */
    private List<String> clauseIds;
    /** 条款版本映射 */
    private Map<String, String> clauseVersionMap;
    /** 主条款ID */
    private String mainClauseId;

    /** 销售渠道配置 */
    private List<Object> salesChannels;
    /** 附加险产品ID列表 */
    private List<String> attachProductIds;

    /** 出单流程配置 */
    private Object issuanceProcessConfig;
    /** 保单形态配置 */
    private Object policyFormConfig;
    /** 核保配置 */
    private Object underwritingConfig;

    /** 定价模式编码（RATE_TABLE 费率表查询 / ACTUARIAL_FORMULA 精算公式），billing 出单按此分派保费计算路径 */
    private String pricingMode;

    /** 创建人 */
    private String createdBy;
}
