package com.titanium.product.query.result;

import java.time.LocalDateTime;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.ActuarialBasis;
import com.titanium.product.valueobject.AuditInfo;
import com.titanium.product.valueobject.CoveragePeriodConfig;
import com.titanium.product.valueobject.InsureCondition;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.PaymentConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.UnderwritingConfig;

import lombok.Getter;
import lombok.Setter;

/**
 * 产品查询结果
 * 用于封装产品查询结果，包含完整的产品配置信息
 */
@Getter
@Setter
public class ProductQueryResult {

    // ====== 基础标识 ======
    private String productId;
    private String productCode;
    private String productName;
    private String productDesc;

    // ====== 分类 ======
    private ProductEnum.ProductForm form;
    private InsuranceProductType insuranceType;
    private ProductEnum.ProductCategory category;

    // ====== 版本与状态 ======
    private String version;
    private ProductEnum.ProductStatus status;
    private String originalProductId;

    // ====== 时间 ======
    private LocalDateTime effectiveTime;
    private LocalDateTime invalidTime;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;

    // ====== 配置（JSON字符串或直接值对象，根据场景选择） ======
    private InsureCondition insureCondition;
    private CoveragePeriodConfig coveragePeriod;
    private PaymentConfig paymentConfig;
    private PricingBasicRule pricingBasicRule;
    private IssuanceProcessConfig issuanceProcessConfig;
    private PolicyFormConfig policyFormConfig;
    private UnderwritingConfig underwritingConfig;

    // ====== 寿险双模式定价配置（PROD-3读侧） ======
    /** 定价模式（RATE_TABLE/ACTUARIAL_FORMULA） */
    private PricingMode pricingMode;
    /** 费率表引用（pricingMode=RATE_TABLE 时有值） */
    private RateTableRef rateTableRef;
    /** 精算基础参数（pricingMode=ACTUARIAL_FORMULA 时有值） */
    private ActuarialBasis actuarialBasis;

    // ====== 审核 ======
    private AuditInfo auditInfo;

    // ====== 审计 ======
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String tenantId;
}
