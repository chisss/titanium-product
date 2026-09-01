package com.titanium.product.api.response.product;
import java.time.LocalDateTime;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.api.response.config.AuditInfoResponse;
import com.titanium.product.api.response.config.CoveragePeriodConfigResponse;
import com.titanium.product.api.response.config.DocumentConfigResponse;
import com.titanium.product.api.response.config.InsureConditionResponse;
import com.titanium.product.api.response.config.IssuanceProcessConfigResponse;
import com.titanium.product.api.response.config.PaymentConfigResponse;
import com.titanium.product.api.response.config.PolicyFormConfigResponse;
import com.titanium.product.api.response.config.UnderwritingConfigResponse;
import com.titanium.product.api.response.pricing.PricingBasicRuleResponse;

import lombok.Data;

/**
 * 产品DTO
 * 用于产品数据的传输，包含完整的产品配置信息
 */
@Data
public class ProductResponse {
    private String productId;
    private String productNo;
    private String productCode;
    private String productName;
    private String productDesc;
    private ProductEnum.ProductForm form;
    private InsuranceProductType insuranceType;
    private ProductEnum.ProductCategory category;
    private String version;
    private ProductEnum.ProductStatus status;
    private String originalProductId;
    /** 绑定的产品模板ID（模板行为配置的定位键） */
    private String templateId;
    private LocalDateTime effectiveTime;
    private LocalDateTime invalidTime;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;

    /** 投保条件 */
    private InsureConditionResponse insureCondition;
    /** 保障期间配置 */
    private CoveragePeriodConfigResponse coveragePeriod;
    /** 缴费方式配置 */
    private PaymentConfigResponse paymentConfig;
    /** 定价基础规则 */
    private PricingBasicRuleResponse pricingBasicRule;
    /** 出单流程配置 */
    private IssuanceProcessConfigResponse issuanceProcessConfig;
    /** 保单形态配置 */
    private PolicyFormConfigResponse policyFormConfig;
    /** 核保配置 */
    private UnderwritingConfigResponse underwritingConfig;
    /** 文档配置（所需投保材料清单 + 生成文档模板清单） */
    private DocumentConfigResponse documentConfig;
    /** 审核信息 */
    private AuditInfoResponse auditInfo;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
