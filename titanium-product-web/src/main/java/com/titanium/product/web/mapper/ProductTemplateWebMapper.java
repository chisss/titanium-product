package com.titanium.product.web.mapper;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;

import com.titanium.product.api.dto.ProductTemplateDTO;
import com.titanium.product.command.CreateProductTemplateCommand;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.valueobject.ClaimConfig;
import com.titanium.product.valueobject.MaintenanceConfig;
import com.titanium.product.valueobject.PolicyStage;
import com.titanium.product.valueobject.UnderwritingConfig;
import com.titanium.product.web.request.CreateProductTemplateRequest;

/**
 * 产品模板 Web 层对象映射器（MapStruct）
 * <p>
 * 边界输入 → CQRS 命令/查询的翻译枢纽：HTTP {@code Request} → 领域命令
 * {@code CreateProductTemplateCommand}（Controller 用）、读模型结果 → 对外 {@code DTO}（Controller/Provider 用）。
 * 创建命令的构造从 application 下沉本层，application 门面入参即领域命令。请求可映射的部分
 * （核保/理赔/保全配置）按 record 组件组装，其余复杂配置（出单流程、保单形态、定价规则）请求暂未承载，置空由聚合根兜底。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface ProductTemplateWebMapper {

    // ==================== 写：Request → 领域命令 ====================

    /**
     * HTTP Request → 创建产品模板命令（Controller 用）
     *
     * @param request 创建产品模板请求
     * @param tenantId 租户ID（请求头）
     * @return 创建产品模板命令
     */
    default CreateProductTemplateCommand toCommand(CreateProductTemplateRequest request, String tenantId) {
        if (request == null) {
            return null;
        }
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
                null);
    }

    // ==================== 读：QueryResult → 对外 DTO ====================

    /**
     * 读模型结果 → 产品模板 DTO（Controller/Provider 用）
     */
    ProductTemplateDTO toDTO(ProductTemplateQueryResult result);

    /**
     * 出单阶段值对象 → DTO（供上面列表映射复用）。
     */
    ProductTemplateDTO.PolicyStageDTO toDTO(PolicyStage stage);
}
