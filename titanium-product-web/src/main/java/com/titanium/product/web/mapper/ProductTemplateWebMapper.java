package com.titanium.product.web.mapper;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.api.dto.ProductTemplateDTO;
import com.titanium.product.api.request.CreateProductTemplateRequest;
import com.titanium.product.domain.command.CreateProductTemplateCommand;
import com.titanium.product.domain.valueobject.*;
import com.titanium.product.query.entity.ProductTemplateQueryResult;

/**
 * 产品模板 Web 层 Mapper
 */
@Mapper(componentModel = "spring")
public interface ProductTemplateWebMapper {

    /**
     * QueryResult -> DTO
     */
    ProductTemplateDTO toDTO(ProductTemplateQueryResult result);

    ProductTemplateDTO.PolicyStageDTO toDTO(PolicyStage stage);

    /**
     * 将请求转换为创建命令
     */
    default CreateProductTemplateCommand toCreateCommand(CreateProductTemplateRequest request, String tenantId) {
        String templateId = "TPL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new CreateProductTemplateCommand(
                templateId,
                request.getTemplateCode(),
                request.getTemplateName(),
                request.getInsuranceCategory(),
                InsuranceType.fromCode(request.getInsuranceType()),
                request.getProductId(),
                IssuanceMode.fromCode(request.getIssuanceMode()),
                request.getPolicyStages() != null
                        ? request.getPolicyStages().stream().map(s -> new PolicyStage(
                                s.getStageCode(), s.getStageName(), s.getRequiredComponents(),
                                s.getValidationRuleSet(), s.getNextStageTransition(), s.isAutoTransition()
                        )).toList()
                        : List.of(),
                request.getUnderwritingConfig() != null
                        ? new UnderwritingConfig(
                                request.getUnderwritingConfig().isRequired(),
                                request.getUnderwritingConfig().getAutoUnderwritingRuleSet(),
                                request.getUnderwritingConfig().getManualThresholdRuleSet(),
                                request.getUnderwritingConfig().getMaxAutoApproveAmount())
                        : new UnderwritingConfig(false, null, null, null),
                request.getPolicyStructure() != null
                        ? new PolicyStructureConfig(
                                SubjectType.fromCode(request.getPolicyStructure().getSubjectType()),
                                request.getPolicyStructure().getSubjectFieldsSchema(),
                                request.getPolicyStructure().isAllowMultipleSubjects(),
                                request.getPolicyStructure().getPartyRoles(),
                                request.getPolicyStructure().getRequiredPartyRoles(),
                                LiabilityStructure.fromCode(request.getPolicyStructure().getLiabilityStructure()))
                        : null,
                request.getMaintenanceConfig() != null
                        ? new MaintenanceConfig(
                                request.getMaintenanceConfig().getAllowedTypes(),
                                request.getMaintenanceConfig().getFreeLookPeriodDays(),
                                request.getMaintenanceConfig().getSurrenderRuleSet(),
                                request.getMaintenanceConfig().getEndorsementRuleSet())
                        : null,
                request.getClaimConfig() != null
                        ? new ClaimConfig(
                                request.getClaimConfig().getClaimStages(),
                                request.getClaimConfig().getReportDeadlineDays(),
                                request.getClaimConfig().getWaitingPeriodDays(),
                                request.getClaimConfig().getClaimRuleSet(),
                                request.getClaimConfig().getRequiredDocuments())
                        : null,
                request.getBillingConfig() != null
                        ? new BillingConfig(
                                request.getBillingConfig().getAllowedPaymentModes(),
                                request.getBillingConfig().getGracePeriodDays(),
                                request.getBillingConfig().getLapseAfterDays(),
                                request.getBillingConfig().isAutoDeductEnabled())
                        : null,
                request.getReinsuranceConfig() != null
                        ? new ReinsuranceConfig(
                                request.getReinsuranceConfig().isAutoReinsurance(),
                                request.getReinsuranceConfig().getRetentionLimit(),
                                request.getReinsuranceConfig().getDefaultContractCode())
                        : null,
                tenantId
        );
    }
}
