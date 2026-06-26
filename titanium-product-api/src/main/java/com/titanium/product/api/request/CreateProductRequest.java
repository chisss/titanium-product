package com.titanium.product.api.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;

import lombok.Data;

/**
 * 创建产品请求
 * 用于创建产品的请求参数，包含完整的产品配置
 */
@Data
public class CreateProductRequest {
    /** 产品ID（可选，不传则自动生成） */
    private String productId;
    /** 产品代码 */
    private String productCode;
    /** 产品名称 */
    private String productName;
    /** 产品描述 */
    private String productDesc;
    /** 产品形态（INDIVIDUAL/GROUP） */
    private ProductEnum.ProductForm form;
    /** 险种类型 */
    private InsuranceType insuranceType;
    /** 产品类别（MAIN/RIDER） */
    private ProductEnum.ProductCategory category;
    /** 销售开始时间 */
    private LocalDateTime saleStartTime;
    /** 销售截止时间 */
    private LocalDateTime saleEndTime;

    /** 投保条件 */
    private InsureConditionRequest insureCondition;
    /** 保障期间配置 */
    private Object coveragePeriod;
    /** 缴费方式配置 */
    private Object paymentConfig;
    /** 定价基础规则 */
    private PricingBasicRuleRequest pricingBasicRule;

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

    /** 创建人 */
    private String createdBy;
}
