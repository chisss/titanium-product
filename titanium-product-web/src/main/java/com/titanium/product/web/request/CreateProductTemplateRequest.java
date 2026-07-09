package com.titanium.product.web.request;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 创建产品模板请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上，由 {@code ProductTemplateWebMapper} 翻译为领域命令
 * {@code CreateProductTemplateCommand}。
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建产品模板请求")
public class CreateProductTemplateRequest {

    @Schema(description = "模板编码", required = true)
    private String templateCode;

    @Schema(description = "模板名称", required = true)
    private String templateName;

    @Schema(description = "险种大类")
    private ProductEnum.ProductCategory insuranceCategory;

    @Schema(description = "险种类型编码", required = true)
    private InsuranceType insuranceType;

    @Schema(description = "关联产品ID")
    private String productId;

    @Schema(description = "出单模式", required = true)
    private ProductEnum.IssuanceMode issuanceMode;

    @Schema(description = "出单阶段定义")
    private List<PolicyStageRequest> policyStages;

    @Schema(description = "核保配置")
    private UnderwritingConfigRequest underwritingConfig;

    @Schema(description = "保单结构配置")
    private PolicyStructureConfigRequest policyStructure;

    @Schema(description = "保全配置")
    private MaintenanceConfigRequest maintenanceConfig;

    @Schema(description = "理赔配置")
    private ClaimConfigRequest claimConfig;

    @Schema(description = "缴费配置")
    private BillingConfigRequest billingConfig;

    @Schema(description = "再保险配置")
    private ReinsuranceConfigRequest reinsuranceConfig;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PolicyStageRequest {
        private String stageCode;
        private String stageName;
        private List<String> requiredComponents;
        private String validationRuleSet;
        private String nextStageTransition;
        private boolean autoTransition;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UnderwritingConfigRequest {
        private boolean required;
        private String autoUnderwritingRuleSet;
        private String manualThresholdRuleSet;
        private BigDecimal maxAutoApproveAmount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PolicyStructureConfigRequest {
        private String subjectType;
        private String subjectFieldsSchema;
        private boolean allowMultipleSubjects;
        private List<String> partyRoles;
        private List<String> requiredPartyRoles;
        private String liabilityStructure;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MaintenanceConfigRequest {
        private List<String> allowedTypes;
        private int freeLookPeriodDays;
        private String surrenderRuleSet;
        private String endorsementRuleSet;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ClaimConfigRequest {
        private List<String> claimStages;
        private int reportDeadlineDays;
        private int waitingPeriodDays;
        private String claimRuleSet;
        private List<String> requiredDocuments;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class BillingConfigRequest {
        private List<String> allowedPaymentModes;
        private int gracePeriodDays;
        private int lapseAfterDays;
        private boolean autoDeductEnabled;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ReinsuranceConfigRequest {
        private boolean autoReinsurance;
        private BigDecimal retentionLimit;
        private String defaultContractCode;
    }
}
