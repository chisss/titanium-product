package com.titanium.product.web.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.api.dto.AuditProductDTO;
import com.titanium.product.api.dto.CreateProductDTO;
import com.titanium.product.api.dto.PricingBasicRuleDTO;
import com.titanium.product.api.dto.ProductDTO;
import com.titanium.product.command.AuditProductCommand;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.command.RejectProductAuditCommand;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.valueobject.InsureCondition;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.web.request.AuditProductRequest;
import com.titanium.product.web.request.CreateProductRequest;
import com.titanium.product.web.request.InsureConditionRequest;
import com.titanium.product.web.request.PricingBasicRuleRequest;

/**
 * 产品 Web 层对象映射器（MapStruct）
 * <p>
 * 边界输入 → CQRS 命令/查询的翻译枢纽：HTTP {@code Request} → 领域命令（Controller 用）、
 * 远程 {@code DTO} → 领域命令（Provider 用）、读模型结果 → 对外 {@code DTO}（Controller/Provider 用）。
 * application 门面入参即领域命令，本映射器在 web 层完成 Request/DTO → Command 的结构翻译，
 * 领域枚举从 DTO 的 String 表示由 {@code fromCode} 还原。命令的业务完整性由聚合根保证，
 * Request/DTO 未承载的复杂配置置空由聚合根兜底。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface ProductWebMapper {

    // ==================== 写：Request/DTO → 领域命令 ====================

    /**
     * HTTP Request → 创建产品命令（Controller 用）
     *
     * @param request 创建产品请求
     * @param tenantId 租户ID（请求头）
     * @return 创建产品命令
     */
    default CreateProductCommand toCommand(CreateProductRequest request, String tenantId) {
        if (request == null) {
            return null;
        }
        return new CreateProductCommand(
                request.getProductId() != null ? request.getProductId() : UUID.randomUUID().toString(),
                request.getProductCode(),
                request.getProductName(),
                request.getProductDesc(),
                request.getForm(),
                request.getInsuranceType(),
                request.getCategory(),
                null, // effectiveTime：审核通过时确定
                request.getSaleStartTime(),
                request.getSaleEndTime(),
                toInsureCondition(request.getInsureCondition()),
                null, // coveragePeriod：后续通过 JSON 转换
                null, // paymentConfig：后续通过 JSON 转换
                toPricingBasicRule(request.getPricingBasicRule()),
                request.getClauseIds(),
                request.getClauseVersionMap(),
                request.getMainClauseId(),
                null, // salesChannels：后续通过 JSON 转换
                request.getAttachProductIds(),
                null, // issuanceProcessConfig：后续通过 JSON 转换
                null, // policyFormConfig：后续通过 JSON 转换
                null, // underwritingConfig：后续通过 JSON 转换
                tenantId);
    }

    /**
     * 远程 DTO → 创建产品命令（Provider 用）
     * <p>
     * DTO 承载的领域枚举以 String 表示，此处经各枚举 {@code fromCode} 还原为领域枚举。
     * </p>
     *
     * @param dto 创建产品 DTO
     * @param tenantId 租户ID（请求头）
     * @return 创建产品命令
     */
    default CreateProductCommand toCommand(CreateProductDTO dto, String tenantId) {
        if (dto == null) {
            return null;
        }
        return new CreateProductCommand(
                dto.getProductId() != null ? dto.getProductId() : UUID.randomUUID().toString(),
                dto.getProductCode(),
                dto.getProductName(),
                dto.getProductDesc(),
                dto.getForm() != null ? ProductEnum.ProductForm.fromCode(dto.getForm()) : null,
                dto.getInsuranceType() != null ? InsuranceType.fromCode(dto.getInsuranceType()) : null,
                dto.getCategory() != null ? ProductEnum.ProductCategory.fromCode(dto.getCategory()) : null,
                null,
                dto.getSaleStartTime(),
                dto.getSaleEndTime(),
                toInsureCondition(dto.getInsureCondition()),
                null,
                null,
                toPricingBasicRule(dto.getPricingBasicRule()),
                dto.getClauseIds(),
                dto.getClauseVersionMap(),
                dto.getMainClauseId(),
                null,
                dto.getAttachProductIds(),
                null,
                null,
                null,
                tenantId);
    }

    /**
     * HTTP Request → 审核通过命令（Controller 用）
     */
    default AuditProductCommand toAuditCommand(String productId, AuditProductRequest request) {
        return new AuditProductCommand(productId, request.getAuditorId(), request.getAuditorName(),
                request.getAuditOpinion(), ProductEnum.AuditResult.PASS);
    }

    /**
     * 远程 DTO → 审核通过命令（Provider 用）
     */
    default AuditProductCommand toAuditCommand(String productId, AuditProductDTO dto) {
        return new AuditProductCommand(productId, dto.getAuditorId(), dto.getAuditorName(),
                dto.getAuditOpinion(), ProductEnum.AuditResult.PASS);
    }

    /**
     * HTTP Request → 驳回审核命令（Controller 用）
     */
    default RejectProductAuditCommand toRejectCommand(String productId, AuditProductRequest request) {
        return new RejectProductAuditCommand(productId, request.getAuditorId(), request.getAuditorName(),
                request.getAuditOpinion());
    }

    /**
     * 远程 DTO → 驳回审核命令（Provider 用）
     */
    default RejectProductAuditCommand toRejectCommand(String productId, AuditProductDTO dto) {
        return new RejectProductAuditCommand(productId, dto.getAuditorId(), dto.getAuditorName(),
                dto.getAuditOpinion());
    }

    // ==================== 读：QueryResult → 对外 DTO ====================

    /**
     * 读模型结果 → 产品 DTO（Controller/Provider 用）
     */
    default ProductDTO toProductDTO(ProductQueryResult result) {
        if (result == null) {
            return null;
        }
        ProductDTO dto = new ProductDTO();
        dto.setProductId(result.getProductId());
        dto.setProductCode(result.getProductCode());
        dto.setProductName(result.getProductName());
        dto.setProductDesc(result.getProductDesc());
        dto.setForm(result.getForm());
        dto.setInsuranceType(result.getInsuranceType());
        dto.setCategory(result.getCategory());
        dto.setVersion(result.getVersion());
        dto.setStatus(result.getStatus());
        dto.setOriginalProductId(result.getOriginalProductId());
        dto.setEffectiveTime(result.getEffectiveTime());
        dto.setInvalidTime(result.getInvalidTime());
        dto.setSaleStartTime(result.getSaleStartTime());
        dto.setSaleEndTime(result.getSaleEndTime());
        dto.setInsureCondition(result.getInsureCondition());
        dto.setCoveragePeriod(result.getCoveragePeriod());
        dto.setPaymentConfig(result.getPaymentConfig());
        dto.setPricingBasicRule(result.getPricingBasicRule());
        dto.setIssuanceProcessConfig(result.getIssuanceProcessConfig());
        dto.setPolicyFormConfig(result.getPolicyFormConfig());
        dto.setUnderwritingConfig(result.getUnderwritingConfig());
        dto.setAuditInfo(result.getAuditInfo());
        dto.setCreatedAt(result.getCreatedAt());
        dto.setCreatedBy(result.getCreatedBy());
        dto.setUpdatedAt(result.getUpdatedAt());
        dto.setUpdatedBy(result.getUpdatedBy());
        return dto;
    }

    /**
     * 定价基础规则值对象 → 对外 DTO（Provider 定价规则查询用）
     */
    default PricingBasicRuleDTO toPricingRuleDTO(PricingBasicRule rule) {
        if (rule == null) {
            return null;
        }
        PricingBasicRuleDTO dto = new PricingBasicRuleDTO();
        dto.setPricingType(rule.pricingType());
        dto.setBaseRate(rule.baseRate());
        dto.setRateFormula(rule.rateFormula());
        return dto;
    }

    // ==================== 值对象组装（default 辅助） ====================

    /**
     * 投保条件请求 → 投保条件值对象。
     */
    default InsureCondition toInsureCondition(InsureConditionRequest req) {
        if (req == null) {
            return null;
        }
        return new InsureCondition(
                req.getMinAge(), req.getMaxAge(),
                req.getForbiddenOccupations(), null,
                req.getMinGroupSize(), req.getMaxGroupSize(),
                req.getHealthNotice(),
                null, null, null, null, null, null, null);
    }

    /**
     * 投保条件远程入参 → 投保条件值对象。
     */
    default InsureCondition toInsureCondition(CreateProductDTO.InsureConditionInput input) {
        if (input == null) {
            return null;
        }
        return new InsureCondition(
                input.getMinAge(), input.getMaxAge(),
                input.getForbiddenOccupations(), null,
                input.getMinGroupSize(), input.getMaxGroupSize(),
                input.getHealthNotice(),
                null, null, null, null, null, null, null);
    }

    /**
     * 定价基础规则请求 → 定价基础规则值对象。
     */
    default PricingBasicRule toPricingBasicRule(PricingBasicRuleRequest req) {
        if (req == null) {
            return null;
        }
        return new PricingBasicRule(
                req.getPricingType(), req.getBaseRate(),
                req.getFactors(), req.getRateFormula(),
                req.getTypeSpecificConfig());
    }

    /**
     * 定价基础规则远程入参 → 定价基础规则值对象（定价类型 String → 领域枚举）。
     */
    default PricingBasicRule toPricingBasicRule(CreateProductDTO.PricingRuleInput input) {
        if (input == null) {
            return null;
        }
        return new PricingBasicRule(
                input.getPricingType() != null ? ProductEnum.PricingType.fromCode(input.getPricingType()) : null,
                input.getBaseRate(), null, input.getRateFormula(), null);
    }
}
