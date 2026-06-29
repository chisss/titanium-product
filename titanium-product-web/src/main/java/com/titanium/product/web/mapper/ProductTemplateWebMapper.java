package com.titanium.product.web.mapper;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;

import com.titanium.product.api.dto.ProductTemplateDTO;
import com.titanium.product.api.request.CreateProductTemplateRequest;
import com.titanium.product.domain.command.CreateProductTemplateCommand;
import com.titanium.product.domain.valueobject.ClaimConfig;
import com.titanium.product.domain.valueobject.MaintenanceConfig;
import com.titanium.product.domain.valueobject.PolicyStage;
import com.titanium.product.domain.valueobject.UnderwritingConfig;
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
     *
     * <p>命令已枚举化重构，仅承载新版字段。请求中可直接映射的部分（核保/理赔/保全配置）按
     * record 真实组件构造，其余复杂配置（出单流程、保单形态、定价规则）请求暂未承载，置 null。
     */
    default CreateProductTemplateCommand toCreateCommand(CreateProductTemplateRequest request, String tenantId) {
        String templateId = "TPL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        UnderwritingConfig underwritingConfig = request.getUnderwritingConfig() != null
                ? new UnderwritingConfig(
                        null,
                        request.getUnderwritingConfig().getAutoUnderwritingRuleSet(),
                        request.getUnderwritingConfig().getMaxAutoApproveAmount(),
                        List.of(),
                        null,
                        false,
                        false)
                : null;

        ClaimConfig claimConfig = request.getClaimConfig() != null
                ? new ClaimConfig(
                        request.getClaimConfig().getClaimStages(),
                        request.getClaimConfig().getReportDeadlineDays(),
                        request.getClaimConfig().getWaitingPeriodDays(),
                        request.getClaimConfig().getClaimRuleSet(),
                        request.getClaimConfig().getRequiredDocuments())
                : null;

        MaintenanceConfig maintenanceConfig = request.getMaintenanceConfig() != null
                ? new MaintenanceConfig(
                        request.getMaintenanceConfig().getAllowedTypes(),
                        request.getMaintenanceConfig().getFreeLookPeriodDays(),
                        request.getMaintenanceConfig().getSurrenderRuleSet(),
                        request.getMaintenanceConfig().getEndorsementRuleSet())
                : null;

        return new CreateProductTemplateCommand(
                templateId,
                request.getTemplateCode(),
                request.getTemplateName(),
                request.getInsuranceType(),
                null,
                null,
                underwritingConfig,
                claimConfig,
                maintenanceConfig,
                null,
                null,
                List.of(),
                List.of(),
                tenantId,
                null
        );
    }
}
