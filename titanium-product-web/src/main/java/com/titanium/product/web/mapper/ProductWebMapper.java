package com.titanium.product.web.mapper;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.api.request.AuditProductRequest;
import com.titanium.product.api.request.CreateProductRequest;
import com.titanium.product.api.response.AdditionalStepResponse;
import com.titanium.product.api.response.AuditInfoResponse;
import com.titanium.product.api.response.CoveragePeriodConfigResponse;
import com.titanium.product.api.response.DocumentConfigResponse;
import com.titanium.product.api.response.DocumentTemplateResponse;
import com.titanium.product.api.response.InsureConditionResponse;
import com.titanium.product.api.response.IssuanceProcessConfigResponse;
import com.titanium.product.api.response.PaymentConfigResponse;
import com.titanium.product.api.response.PolicyFormConfigResponse;
import com.titanium.product.api.response.PricingBasicRuleResponse;
import com.titanium.product.api.response.ProductResponse;
import com.titanium.product.api.response.RequiredMaterialResponse;
import com.titanium.product.api.response.UnderwritingConfigResponse;
import com.titanium.product.command.AuditProductCommand;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.command.RejectProductAuditCommand;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.valueobject.AdditionalStep;
import com.titanium.product.valueobject.AuditInfo;
import com.titanium.product.valueobject.CoveragePeriodConfig;
import com.titanium.product.valueobject.DocumentConfig;
import com.titanium.product.valueobject.InsureCondition;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.valueobject.PaymentConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.valueobject.UnderwritingConfig;
import com.titanium.product.web.dto.AuditProductDTO;
import com.titanium.product.web.dto.ConfigureLifeProductDTO;
import com.titanium.product.web.dto.CreateProductDTO;
import com.titanium.product.web.dto.InsureConditionDTO;
import com.titanium.product.web.dto.PricingBasicRuleDTO;

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
    default CreateProductCommand toCommand(CreateProductDTO request, String tenantId) {
        if (request == null) {
            return null;
        }
        return new CreateProductCommand(
                request.getProductId() != null ? request.getProductId() : UUID.randomUUID().toString(),
                request.getTemplateId(),
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
                request.getCoveragePeriod(), // 保障期间：DTO 直承载领域值对象（web 可依赖 domain VO）
                request.getPaymentConfig(), // 缴费方式：同上
                toPricingBasicRule(request.getPricingBasicRule()),
                request.getClauseIds(),
                request.getClauseVersionMap(),
                request.getMainClauseId(),
                request.getSalesChannels(), // 销售渠道配置
                request.getAttachProductIds(),
                request.getIssuanceProcessConfig(), // 出单流程配置
                request.getPolicyFormConfig(), // 保单形态配置
                request.getUnderwritingConfig(), // 核保配置
                tenantId,
                request.getPricingMode() != null ? PricingMode.fromCode(request.getPricingMode()) : null,
                request.getRateTableRef(), // 费率表引用（RATE_TABLE 模式）
                request.getActuarialBasis(), // 精算基础参数（ACTUARIAL_FORMULA 模式）
                request.getDocumentConfig(), // 文档配置（所需投保材料 + 生成文档模板）
                request.getCreatedBy()); // 创建人：前端随请求体传入登录用户显示名
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
    default CreateProductCommand toCommand(CreateProductRequest dto, String tenantId) {
        if (dto == null) {
            return null;
        }
        return new CreateProductCommand(
                dto.getProductId() != null ? dto.getProductId() : UUID.randomUUID().toString(),
                dto.getTemplateId(),
                dto.getProductCode(),
                dto.getProductName(),
                dto.getProductDesc(),
                dto.getForm() != null ? ProductEnum.ProductForm.fromCode(dto.getForm()) : null,
                dto.getInsuranceType() != null ? InsuranceProductType.fromCode(dto.getInsuranceType()) : null,
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
                tenantId,
                dto.getPricingMode() != null ? PricingMode.fromCode(dto.getPricingMode()) : null,
                null, // rateTableRef：后续通过 JSON 转换
                null, // actuarialBasis：后续通过 JSON 转换
                null, // documentConfig：远程契约暂不承载文档配置
                null); // createdBy：远程契约暂不承载创建人
    }

    /**
     * HTTP Request → 审核通过命令（Controller 用）
     */
    default AuditProductCommand toAuditCommand(String productId, AuditProductDTO request) {
        return new AuditProductCommand(productId, request.getAuditorId(), request.getAuditorName(),
                request.getAuditOpinion(), ProductEnum.AuditResult.PASS);
    }

    /**
     * 远程 DTO → 审核通过命令（Provider 用）
     */
    default AuditProductCommand toAuditCommand(String productId, AuditProductRequest dto) {
        return new AuditProductCommand(productId, dto.getAuditorId(), dto.getAuditorName(),
                dto.getAuditOpinion(), ProductEnum.AuditResult.PASS);
    }

    /**
     * HTTP Request → 驳回审核命令（Controller 用）
     */
    default RejectProductAuditCommand toRejectCommand(String productId, AuditProductDTO request) {
        return new RejectProductAuditCommand(productId, request.getAuditorId(), request.getAuditorName(),
                request.getAuditOpinion());
    }

    /**
     * 远程 DTO → 驳回审核命令（Provider 用）
     */
    default RejectProductAuditCommand toRejectCommand(String productId, AuditProductRequest dto) {
        return new RejectProductAuditCommand(productId, dto.getAuditorId(), dto.getAuditorName(),
                dto.getAuditOpinion());
    }

    // ==================== 读：QueryResult → 对外 DTO ====================

    /**
     * 读模型结果 → 产品 DTO（Controller/Provider 用）
     */
    /**
     * 读模型结果 → 产品响应（Controller/Provider 用）。
     * <p>
     * 声明式映射：标量字段同名自动映射；6 个配置值对象经声明式子映射器转为对应 {@code *Response}
     * 强类型（替代原 {@code Object} 松类型）；{@code pricingBasicRule} 因需合并 result 顶层的
     * 定价模式/精算基础/费率表引用，复用富化装配的 {@link #toPricingRuleResponse}。
     * </p>
     */
    @Mapping(target = "pricingBasicRule", expression = "java(toPricingRuleResponse(result))")
    ProductResponse toProductResponse(ProductQueryResult result);

    /** 投保条件值对象 → 响应（同名字段声明式映射） */
    InsureConditionResponse toInsureConditionResponse(InsureCondition vo);

    /** 保障期间配置值对象 → 响应 */
    CoveragePeriodConfigResponse toCoveragePeriodConfigResponse(CoveragePeriodConfig vo);

    /** 缴费方式配置值对象 → 响应 */
    PaymentConfigResponse toPaymentConfigResponse(PaymentConfig vo);

    /** 出单流程配置值对象 → 响应（内嵌附加步骤列表由 MapStruct 逐项映射） */
    IssuanceProcessConfigResponse toIssuanceProcessConfigResponse(IssuanceProcessConfig vo);

    /** 附加业务步骤值对象 → 响应 */
    AdditionalStepResponse toAdditionalStepResponse(AdditionalStep vo);

    /** 保单形态配置值对象 → 响应 */
    PolicyFormConfigResponse toPolicyFormConfigResponse(PolicyFormConfig vo);

    /** 核保配置值对象 → 响应 */
    UnderwritingConfigResponse toUnderwritingConfigResponse(UnderwritingConfig vo);

    /** 文档配置值对象 → 响应（内嵌所需材料/文档模板列表由 MapStruct 逐项映射） */
    DocumentConfigResponse toDocumentConfigResponse(DocumentConfig vo);

    /** 所需投保材料值对象 → 响应 */
    RequiredMaterialResponse toRequiredMaterialResponse(DocumentConfig.RequiredMaterial vo);

    /** 生成文档模板值对象 → 响应 */
    DocumentTemplateResponse toDocumentTemplateResponse(DocumentConfig.DocumentTemplate vo);

    /** 审核信息值对象 → 响应 */
    AuditInfoResponse toAuditInfoResponse(AuditInfo vo);

    /**
     * 产品查询结果 → 定价基础规则 DTO（Provider 定价规则查询用）
     * <p>
     * 含寿险双模式定价扩展字段（PROD-3读侧）：pricingMode/rateTableRef/actuarialBasis
     * 位于 ProductQueryResult 顶层，与 PricingBasicRule 并列，此处合并到同一 DTO 返回给 billing。
     * </p>
     */
    default PricingBasicRuleResponse toPricingRuleResponse(ProductQueryResult result) {
        if (result == null) {
            return null;
        }
        PricingBasicRuleResponse dto = new PricingBasicRuleResponse();
        // 基础定价规则字段
        PricingBasicRule rule = result.getPricingBasicRule();
        if (rule != null) {
            dto.setPricingType(rule.pricingType());
            dto.setBaseRate(rule.baseRate());
            dto.setRateFormula(rule.rateFormula());
        }
        // 定价模式（RATE_TABLE / ACTUARIAL_FORMULA）
        dto.setPricingMode(result.getPricingMode() != null ? result.getPricingMode().getCode() : null);
        // 精算基础参数（pricingMode=ACTUARIAL_FORMULA 时有值）
        if (result.getActuarialBasis() != null) {
            dto.setPredefinedInterestRate(result.getActuarialBasis().predefinedInterestRate());
            dto.setMortalityTableRef(result.getActuarialBasis().mortalityTableRef());
            dto.setExpenseLoadingRate(result.getActuarialBasis().expenseLoadingRate());
        }
        // 费率表引用（pricingMode=RATE_TABLE 时有值）
        if (result.getRateTableRef() != null) {
            dto.setClauseId(result.getRateTableRef().clauseId());
            dto.setTableCode(result.getRateTableRef().tableCode());
            dto.setTableVersion(result.getRateTableRef().version());
        }
        return dto;
    }

    // ==================== 值对象组装（default 辅助） ====================

    /**
     * 寿险产品规格请求 → 寿险产品规格值对象（Controller 用）。
     * <p>
     * 将 HTTP 请求承载的年龄/保额/缴费期/保障期结构翻译为领域值对象 {@code LifeProductSpec}，
     * 区间与选项的合法性由值对象紧凑构造器兜底校验。
     * </p>
     *
     * @param req 配置寿险产品请求
     * @return 寿险产品规格值对象
     */
    default LifeProductSpec toLifeProductSpec(ConfigureLifeProductDTO req) {
        if (req == null) {
            return null;
        }
        LifeProductSpec.AgeRange entryAgeRange = req.getEntryAgeRange() != null
                ? new LifeProductSpec.AgeRange(req.getEntryAgeRange().getMinAge(), req.getEntryAgeRange().getMaxAge())
                : null;
        LifeProductSpec.SumInsuredRange sumInsuredRange = req.getSumInsuredRange() != null
                ? new LifeProductSpec.SumInsuredRange(req.getSumInsuredRange().getMinSumInsured(),
                        req.getSumInsuredRange().getMaxSumInsured())
                : null;
        List<LifeProductSpec.PremiumTermOption> premiumTermOptions = req.getPremiumTermOptions() == null ? List.of()
                : req.getPremiumTermOptions().stream()
                        .map(opt -> new LifeProductSpec.PremiumTermOption(opt.getYears(), opt.getToAge(),
                                opt.getDescription()))
                        .toList();
        List<LifeProductSpec.CoverageTermOption> coverageTermOptions = req.getCoverageTermOptions() == null ? List.of()
                : req.getCoverageTermOptions().stream()
                        .map(opt -> new LifeProductSpec.CoverageTermOption(opt.getYears(), opt.getToAge(),
                                opt.isWholeLife(), opt.getDescription()))
                        .toList();
        return new LifeProductSpec(req.getProductType(), entryAgeRange, sumInsuredRange, premiumTermOptions,
                coverageTermOptions);
    }

    /**
     * 投保条件请求 → 投保条件值对象。
     */
    default InsureCondition toInsureCondition(InsureConditionDTO req) {
        if (req == null) {
            return null;
        }
        return new InsureCondition(
                req.getMinAge(), req.getMaxAge(),
                req.getForbiddenOccupations(), req.getAllowedOccupations(),
                req.getMinGroupSize(), req.getMaxGroupSize(),
                req.getHealthNotice(),
                req.getMinInsuredAmount(), req.getMaxInsuredAmount(),
                req.getForbiddenRegions(), req.getAllowedRegions(),
                req.getMaxInsuredCount(), req.getWaitingPeriodDays(), req.getHesitationPeriodDays());
    }

    /**
     * 投保条件远程入参 → 投保条件值对象。
     */
    default InsureCondition toInsureCondition(CreateProductRequest.InsureConditionInput input) {
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
    default PricingBasicRule toPricingBasicRule(PricingBasicRuleDTO req) {
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
    default PricingBasicRule toPricingBasicRule(CreateProductRequest.PricingRuleInput input) {
        if (input == null) {
            return null;
        }
        return new PricingBasicRule(
                input.getPricingType() != null ? ProductEnum.PricingType.fromCode(input.getPricingType()) : null,
                input.getBaseRate(), null, input.getRateFormula(), null);
    }

    /**
     * 产品条款关联读模型结果 → 对外响应（Provider 用）
     * <p>
     * 同名字段自动映射（clauseId/clauseVersion/mainClause/bindTime），供 policy 域出单时
     * 据条款ID与版本装配保单条款快照。
     * </p>
     *
     * @param result 产品条款关联查询结果
     * @return 产品条款关联响应
     */
    com.titanium.product.api.response.ProductClauseResponse toProductClauseResponse(
            com.titanium.product.query.result.ProductClauseQueryResult result);
}
