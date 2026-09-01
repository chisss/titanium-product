package com.titanium.product.api.response.pricing;

import java.math.BigDecimal;

import com.titanium.metadata.enums.product.ProductEnum;

import lombok.Data;

/**
 * 定价基础规则DTO
 * 用于表示产品的定价基础规则，含寿险双模式定价配置（PROD-3读侧）
 */
@Data
public class PricingBasicRuleResponse {
    /**
     * 定价类型（FIXED-固定费率/STEP-阶梯费率/FACTOR-因子定价）
     */
    private ProductEnum.PricingType pricingType;

    /**
     * 基础费率（如车险基础费率 0.02），供保费计算使用
     */
    private BigDecimal baseRate;

    /**
     * 费率计算公式（如 保费=保额×基础费率×∏定价因子）
     */
    private String rateFormula;

    /**
     * 定价系数（JSON格式）
     */
    private String pricingFactors;

    /**
     * 最低保费
     */
    private Double minPremium;

    /**
     * 最高保费
     */
    private Double maxPremium;

    /**
     * 费率表ID（历史字段，向后兼容）
     */
    private String rateTableId;

    // ===== 寿险双模式定价扩展（PROD-3读侧） =====

    /**
     * 定价模式 code（RATE_TABLE/ACTUARIAL_FORMULA），billing 据此分派计算路径
     */
    private String pricingMode;

    // --- 精算公式模式参数（pricingMode=ACTUARIAL_FORMULA 时有值） ---

    /** 预定利率（如 0.03 表示 3%） */
    private BigDecimal predefinedInterestRate;

    /** 预定死亡率表引用编码（如 "CL2010-2013"） */
    private String mortalityTableRef;

    /** 附加费用率（如 0.20 表示 20%，净保费×(1+rate)=毛保费） */
    private BigDecimal expenseLoadingRate;

    // --- 费率表查询模式参数（pricingMode=RATE_TABLE 时有值） ---

    /** 费率表所属条款ID */
    private String clauseId;

    /** 费率表编码 */
    private String tableCode;

    /** 费率表版本 */
    private String tableVersion;
}
