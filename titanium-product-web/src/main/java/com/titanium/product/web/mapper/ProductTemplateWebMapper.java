package com.titanium.product.web.mapper;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.api.response.product.ProductTemplateResponse;
import com.titanium.product.command.CreateProductTemplateCommand;
import com.titanium.product.command.UpdateProductTemplateCommand;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.valueobject.PolicyStage;
import com.titanium.product.valueobject.config.ClaimConfig;
import com.titanium.product.valueobject.config.IssuanceProcessConfig;
import com.titanium.product.valueobject.config.MaintenanceConfig;
import com.titanium.product.valueobject.config.UnderwritingConfig;
import com.titanium.product.web.dto.CreateProductTemplateDTO;
import com.titanium.product.web.dto.UpdateProductTemplateDTO;

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
    default CreateProductTemplateCommand toCommand(CreateProductTemplateDTO request, String tenantId) {
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

        // 出单流程配置：聚合根强制非空。请求未显式指定出单模式时默认三步出单（意向单→投保单→核保→保单），
        // 由出单模式派生标准步骤链，避免因 issuanceProcessConfig 为 null 触发聚合根校验失败。
        IssuanceProcessConfig issuanceProcessConfig = buildIssuanceProcessConfig(request.getIssuanceMode());

        return new CreateProductTemplateCommand(
                templateId,
                request.getTemplateCode(),
                request.getTemplateName(),
                request.getInsuranceType(),
                null,
                issuanceProcessConfig,
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

    /**
     * HTTP Request → 更新产品模板命令（Controller 用）
     * <p>
     * DTO 字段已直接承载领域值对象，此处仅补齐聚合标识 {@code templateId} 与租户，其余透传。
     * 值对象合法性由其紧凑构造器保证，命令的业务前置校验（模板非删除态）由聚合根兜底。
     * </p>
     *
     * @param templateId 模板ID（路径变量）
     * @param request 更新产品模板请求
     * @param tenantId 租户ID（请求头）
     * @return 更新产品模板命令
     */
    default UpdateProductTemplateCommand toUpdateCommand(String templateId, UpdateProductTemplateDTO request,
                                                         String tenantId) {
        if (request == null) {
            return null;
        }
        return new UpdateProductTemplateCommand(
                templateId,
                request.getTemplateName(),
                request.getIssuanceMode(),
                request.getPolicyStages(),
                request.getUnderwritingConfig(),
                request.getPolicyStructure(),
                request.getMaintenanceConfig(),
                request.getClaimConfig(),
                request.getBillingConfig(),
                request.getReinsuranceConfig(),
                request.getDividendConfig(),
                tenantId);
    }

    /**
     * 由出单模式派生出单流程配置：模式缺省时按三步出单兜底，并据模式生成标准步骤链。
     * <p>
     * 聚合根 {@code ProductTemplate} 要求 {@code issuanceProcessConfig} 非空，此处保证请求未承载出单流程细节时
     * 仍能产出合法配置（出单有效期默认 30 天，附加步骤留空）。
     * </p>
     *
     * @param issuanceMode 出单模式（可空，空则默认 THREE_STEP）
     * @return 出单流程配置值对象
     */
    default IssuanceProcessConfig buildIssuanceProcessConfig(ProductEnum.IssuanceMode issuanceMode) {
        ProductEnum.IssuanceMode mode = issuanceMode != null ? issuanceMode : ProductEnum.IssuanceMode.THREE_STEP;
        List<ProductEnum.IssuanceStep> steps = switch (mode) {
            case ONE_STEP -> List.of(ProductEnum.IssuanceStep.POLICY_ISSUE);
            case TWO_STEP -> List.of(ProductEnum.IssuanceStep.APPLICATION_SUBMIT, ProductEnum.IssuanceStep.POLICY_ISSUE);
            default -> List.of(ProductEnum.IssuanceStep.PROPOSAL_CREATE, ProductEnum.IssuanceStep.APPLICATION_SUBMIT,
                    ProductEnum.IssuanceStep.UNDERWRITING, ProductEnum.IssuanceStep.POLICY_ISSUE);
        };
        boolean proposalRequired = mode == ProductEnum.IssuanceMode.THREE_STEP
                || mode == ProductEnum.IssuanceMode.CUSTOM;
        boolean underwritingSkippable = mode == ProductEnum.IssuanceMode.ONE_STEP;
        return new IssuanceProcessConfig(mode, steps, proposalRequired, underwritingSkippable, false, 30, List.of());
    }

    // ==================== 读：QueryResult → 对外 DTO ====================

    /**
     * 读模型结果 → 产品模板 DTO（Controller/Provider 用）
     * <p>
     * {@code issuanceMode} 读模型存枚举名字符串，经 {@link #toIssuanceMode(String)} 空安全转枚举，
     * 遇历史脏数据（非法值）降级为 null 而非抛异常。
     * </p>
     */
    @Mapping(target = "issuanceMode", source = "issuanceMode", qualifiedByName = "toIssuanceMode")
    ProductTemplateResponse toResponse(ProductTemplateQueryResult result);

    /**
     * 出单阶段值对象 → DTO（供上面列表映射复用）。
     */
    ProductTemplateResponse.PolicyStageDTO toResponse(PolicyStage stage);

    /**
     * 出单模式枚举名字符串 → 枚举（空安全 + 容错）。
     * <p>
     * 读模型 {@code issuance_mode} 列存枚举名（如 {@code TWO_STEP}）。空值或历史脏数据（曾误存整段
     * IssuanceProcessConfig JSON）时返回 null，避免 {@code Enum.valueOf} 抛
     * {@code IllegalArgumentException} 导致查询 500。
     * </p>
     */
    @Named("toIssuanceMode")
    default ProductEnum.IssuanceMode toIssuanceMode(String issuanceMode) {
        if (issuanceMode == null || issuanceMode.isBlank()) {
            return null;
        }
        try {
            return ProductEnum.IssuanceMode.valueOf(issuanceMode);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
