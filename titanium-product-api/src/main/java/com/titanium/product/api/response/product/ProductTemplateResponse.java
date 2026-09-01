package com.titanium.product.api.response.product;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.insurance.SubjectType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.common.enums.LiabilityStructure;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 产品模板 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "产品模板数据传输对象")
public class ProductTemplateResponse {

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "险种大类: MAIN/RIDER")
    private ProductEnum.ProductCategory insuranceCategory;

    @Schema(description = "险种类型")
    private InsuranceType insuranceType;

    @Schema(description = "关联产品ID")
    private String productId;

    @Schema(description = "出单模式: ONE_STEP/TWO_STEP/THREE_STEP/CUSTOM")
    private ProductEnum.IssuanceMode issuanceMode;

    @Schema(description = "出单阶段定义")
    private List<PolicyStageDTO> policyStages;

    @Schema(description = "核保配置")
    private UnderwritingConfigDTO underwritingConfig;

    @Schema(description = "保单结构配置")
    private PolicyStructureConfigDTO policyStructure;

    @Schema(description = "保全配置")
    private MaintenanceConfigDTO maintenanceConfig;

    @Schema(description = "理赔配置")
    private ClaimConfigDTO claimConfig;

    @Schema(description = "缴费配置")
    private BillingConfigDTO billingConfig;

    @Schema(description = "再保险配置")
    private ReinsuranceConfigDTO reinsuranceConfig;

    @Schema(description = "分红配置（分红险专属：红利分配方式 + 三档演示利率）")
    private DividendConfigDTO dividendConfig;

    @Schema(description = "寿险产品规格（寿险专属：投保年龄/保额范围/缴费期/保障期选项）")
    private LifeProductSpecDTO lifeProductSpec;

    @Schema(description = "模板状态: ACTIVE/INACTIVE")
    private CommonStatus status;

    @Schema(description = "租户ID")
    private String tenantId;

    // ========== 嵌套 DTO ==========

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "出单阶段DTO")
    public static class PolicyStageDTO {
        @Schema(description = "阶段编码")
        private String stageCode;
        @Schema(description = "阶段名称")
        private String stageName;
        @Schema(description = "必需数据组件")
        private List<String> requiredComponents;
        @Schema(description = "校验规则集编码")
        private String validationRuleSet;
        @Schema(description = "下一阶段触发条件")
        private String nextStageTransition;
        @Schema(description = "是否自动流转")
        private boolean autoTransition;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "核保配置DTO")
    public static class UnderwritingConfigDTO {
        @Schema(description = "是否需要核保")
        private boolean required;
        @Schema(description = "自动核保规则集编码")
        private String autoUnderwritingRuleSet;
        @Schema(description = "转人工规则集编码")
        private String manualThresholdRuleSet;
        @Schema(description = "自动核保最高保额")
        private java.math.BigDecimal maxAutoApproveAmount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "保单结构配置DTO")
    public static class PolicyStructureConfigDTO {
        @Schema(description = "标的类型")
        private SubjectType subjectType;
        @Schema(description = "标的必填字段Schema")
        private String subjectFieldsSchema;
        @Schema(description = "是否允许多标的")
        private boolean allowMultipleSubjects;
        @Schema(description = "参与方角色列表")
        private List<String> partyRoles;
        @Schema(description = "必需参与方角色")
        private List<String> requiredPartyRoles;
        @Schema(description = "责任结构类型")
        private LiabilityStructure liabilityStructure;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "保全配置DTO")
    public static class MaintenanceConfigDTO {
        @Schema(description = "允许的保全类型")
        private List<String> allowedTypes;
        @Schema(description = "犹豫期天数")
        private int freeLookPeriodDays;
        @Schema(description = "退保规则集编码")
        private String surrenderRuleSet;
        @Schema(description = "批改规则集编码")
        private String endorsementRuleSet;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "理赔配置DTO")
    public static class ClaimConfigDTO {
        @Schema(description = "理赔阶段列表")
        private List<String> claimStages;
        @Schema(description = "报案时效天数")
        private int reportDeadlineDays;
        @Schema(description = "等待期天数")
        private int waitingPeriodDays;
        @Schema(description = "理赔审核规则集编码")
        private String claimRuleSet;
        @Schema(description = "理赔所需材料")
        private List<String> requiredDocuments;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "缴费配置DTO")
    public static class BillingConfigDTO {
        @Schema(description = "允许的缴费方式")
        private List<String> allowedPaymentModes;
        @Schema(description = "宽限期天数")
        private int gracePeriodDays;
        @Schema(description = "失效天数")
        private int lapseAfterDays;
        @Schema(description = "是否支持自动扣款")
        private boolean autoDeductEnabled;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "再保险配置DTO")
    public static class ReinsuranceConfigDTO {
        @Schema(description = "是否自动分保")
        private boolean autoReinsurance;
        @Schema(description = "自留保额上限")
        private java.math.BigDecimal retentionLimit;
        @Schema(description = "默认再保合约编码")
        private String defaultContractCode;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "分红配置DTO（分红险专属）")
    public static class DividendConfigDTO {
        @Schema(description = "红利分配方式: CASH/ACCUMULATE/PAID_UP_ADDITION/OFFSET_PREMIUM")
        private ProductEnum.DividendDistribution distribution;
        @Schema(description = "低档演示利率（如 0.015 表示 1.5%）")
        private BigDecimal lowDemoRate;
        @Schema(description = "中档演示利率")
        private BigDecimal midDemoRate;
        @Schema(description = "高档演示利率")
        private BigDecimal highDemoRate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "寿险产品规格DTO（寿险专属）")
    public static class LifeProductSpecDTO {
        @Schema(description = "险种三级分类: 定期寿/终身寿/两全/年金")
        private InsuranceProductType productType;
        @Schema(description = "可投保年龄范围")
        private AgeRangeDTO entryAgeRange;
        @Schema(description = "保额范围")
        private SumInsuredRangeDTO sumInsuredRange;
        @Schema(description = "缴费期选项列表")
        private List<PremiumTermOptionDTO> premiumTermOptions;
        @Schema(description = "保障期选项列表")
        private List<CoverageTermOptionDTO> coverageTermOptions;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "可投保年龄范围DTO")
    public static class AgeRangeDTO {
        @Schema(description = "最小投保年龄（含）")
        private int minAge;
        @Schema(description = "最大投保年龄（含）")
        private int maxAge;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "保额范围DTO")
    public static class SumInsuredRangeDTO {
        @Schema(description = "最低基本保额（含）")
        private BigDecimal minSumInsured;
        @Schema(description = "最高基本保额（含）")
        private BigDecimal maxSumInsured;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "缴费期选项DTO")
    public static class PremiumTermOptionDTO {
        @Schema(description = "缴费年数（0 表示趸缴）")
        private int years;
        @Schema(description = "缴至年龄（与 years 二选一，null 表示按年数缴费）")
        private Integer toAge;
        @Schema(description = "选项描述")
        private String description;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "保障期选项DTO")
    public static class CoverageTermOptionDTO {
        @Schema(description = "保障年数（0 表示终身）")
        private int years;
        @Schema(description = "保至年龄（与 years 二选一，null 表示按年数保障）")
        private Integer toAge;
        @Schema(description = "是否终身保障")
        private boolean wholeLife;
        @Schema(description = "选项描述")
        private String description;
    }
}
