package com.titanium.product.api.dto;

import java.util.List;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.valueobject.LiabilityStructure;
import com.titanium.product.domain.valueobject.SubjectType;

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
public class ProductTemplateDTO {

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
}
