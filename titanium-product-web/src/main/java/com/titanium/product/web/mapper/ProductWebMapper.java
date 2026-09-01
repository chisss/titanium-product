package com.titanium.product.web.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.titanium.common.util.SnowflakeIdGenerator;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.api.request.product.AuditProductRequest;
import com.titanium.product.api.request.product.CreateProductRequest;
import com.titanium.product.api.response.clause.ProductClauseResponse;
import com.titanium.product.api.response.config.AdditionalStepResponse;
import com.titanium.product.api.response.config.AuditInfoResponse;
import com.titanium.product.api.response.config.CoveragePeriodConfigResponse;
import com.titanium.product.api.response.config.DocumentConfigResponse;
import com.titanium.product.api.response.config.InsureConditionResponse;
import com.titanium.product.api.response.config.IssuanceProcessConfigResponse;
import com.titanium.product.api.response.config.PaymentConfigResponse;
import com.titanium.product.api.response.config.PolicyFormConfigResponse;
import com.titanium.product.api.response.config.UnderwritingConfigResponse;
import com.titanium.product.api.response.document.DocumentTemplateResponse;
import com.titanium.product.api.response.document.RequiredMaterialResponse;
import com.titanium.product.api.response.pricing.PricingBasicRuleResponse;
import com.titanium.product.api.response.product.ProductResponse;
import com.titanium.product.command.AuditProductCommand;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.command.RejectProductAuditCommand;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.valueobject.AdditionalStep;
import com.titanium.product.valueobject.AuditInfo;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.valueobject.config.CoveragePeriodConfig;
import com.titanium.product.valueobject.config.DocumentConfig;
import com.titanium.product.valueobject.config.InsureCondition;
import com.titanium.product.valueobject.config.IssuanceProcessConfig;
import com.titanium.product.valueobject.config.PaymentConfig;
import com.titanium.product.valueobject.config.PolicyFormConfig;
import com.titanium.product.valueobject.config.UnderwritingConfig;
import com.titanium.product.web.dto.AuditProductDTO;
import com.titanium.product.web.dto.ConfigureLifeProductDTO;
import com.titanium.product.web.dto.CreateProductDTO;

/**
 * 产品 Web 层对象映射器（MapStruct，声明式）
 * <p>
 * 边界输入 → CQRS 命令/查询的翻译枢纽：HTTP {@code DTO} → 领域命令（Controller 用）、
 * 远程 {@code Request} → 领域命令（Provider 用）、读模型结果 → 对外响应（Controller/Provider 用）。
 * application 门面入参即领域命令，本映射器在 web 层完成 DTO/Request → Command 的结构翻译，
 * 领域枚举从 String 表示由各枚举 {@code fromCode} 还原；命令的业务完整性由聚合根保证，
 * DTO/Request 未承载的复杂配置置空由聚合根兜底。productId 缺省时由 {@link SnowflakeIdGenerator} 生成。
 * </p>
 */
@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE,
        imports = { SnowflakeIdGenerator.class, ProductEnum.class })
public interface ProductWebMapper {

    // ==================== 写：Request/DTO → 领域命令 ====================

    /**
     * HTTP DTO → 创建产品命令（Controller 用，同名字段自动映射，差异字段见下）
     *
     * @param request 创建产品请求
     * @param tenantId 租户ID（请求头）
     * @return 创建产品命令
     */
    @Mapping(target = "productId",
            expression = "java(request.getProductId() != null ? request.getProductId() : SnowflakeIdGenerator.generate())")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "pricingMode", source = "request.pricingMode", qualifiedByName = "pricingModeFromCode")
    CreateProductCommand toCommand(CreateProductDTO request, String tenantId);

    /**
     * 远程 Request → 创建产品命令（Provider 用，形态/险种/类别 String → 领域枚举）
     *
     * @param dto 创建产品请求
     * @param tenantId 租户ID（请求头）
     * @return 创建产品命令
     */
    @Mapping(target = "productId",
            expression = "java(dto.getProductId() != null ? dto.getProductId() : SnowflakeIdGenerator.generate())")
    @Mapping(target = "form", source = "dto.form", qualifiedByName = "productFormFromCode")
    @Mapping(target = "insuranceType", source = "dto.insuranceType", qualifiedByName = "insuranceTypeFromCode")
    @Mapping(target = "category", source = "dto.category", qualifiedByName = "productCategoryFromCode")
    @Mapping(target = "pricingBasicRule.pricingType", source = "dto.pricingBasicRule.pricingType",
            qualifiedByName = "pricingTypeFromCode")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "pricingMode", source = "dto.pricingMode", qualifiedByName = "pricingModeFromCode")
    CreateProductCommand toCommand(CreateProductRequest dto, String tenantId);

    /**
     * HTTP DTO → 审核通过命令（Controller 用，审核结论固定 PASS）
     */
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "auditResult", expression = "java(ProductEnum.AuditResult.PASS)")
    AuditProductCommand toAuditCommand(String productId, AuditProductDTO request);

    /**
     * 远程 DTO → 审核通过命令（Provider 用，审核结论固定 PASS）
     */
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "auditResult", expression = "java(ProductEnum.AuditResult.PASS)")
    AuditProductCommand toAuditCommand(String productId, AuditProductRequest dto);

    /**
     * HTTP DTO → 驳回审核命令（Controller 用）
     */
    @Mapping(target = "productId", source = "productId")
    RejectProductAuditCommand toRejectCommand(String productId, AuditProductDTO request);

    /**
     * 远程 DTO → 驳回审核命令（Provider 用）
     */
    @Mapping(target = "productId", source = "productId")
    RejectProductAuditCommand toRejectCommand(String productId, AuditProductRequest dto);

    // ==================== 读：QueryResult → 对外响应 ====================

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
     * 产品查询结果 → 定价基础规则响应（Provider 定价规则查询用）
     * <p>
     * 含寿险双模式定价扩展字段（PROD-3读侧）：pricingMode/rateTableRef/actuarialBasis
     * 位于 ProductQueryResult 顶层，与 PricingBasicRule 并列，此处合并到同一响应返回给 billing。
     * </p>
     */
    @Mapping(target = "pricingType", source = "pricingBasicRule.pricingType")
    @Mapping(target = "baseRate", source = "pricingBasicRule.baseRate")
    @Mapping(target = "rateFormula", source = "pricingBasicRule.rateFormula")
    @Mapping(target = "pricingMode", source = "pricingMode", qualifiedByName = "pricingModeCode")
    @Mapping(target = "predefinedInterestRate", source = "actuarialBasis.predefinedInterestRate")
    @Mapping(target = "mortalityTableRef", source = "actuarialBasis.mortalityTableRef")
    @Mapping(target = "expenseLoadingRate", source = "actuarialBasis.expenseLoadingRate")
    @Mapping(target = "clauseId", source = "rateTableRef.clauseId")
    @Mapping(target = "tableCode", source = "rateTableRef.tableCode")
    @Mapping(target = "tableVersion", source = "rateTableRef.version")
    PricingBasicRuleResponse toPricingRuleResponse(ProductQueryResult result);

    /**
     * 寿险产品规格请求 → 寿险产品规格值对象（Controller 用）。
     * <p>
     * 将 HTTP 请求承载的年龄/保额/缴费期/保障期结构翻译为领域值对象 {@code LifeProductSpec}，
     * 区间与选项的合法性由值对象紧凑构造器兜底校验；缴费期/保障期选项缺省时由紧凑构造器
     * 落空 {@link List}（null → {@code List.of()}）。
     * </p>
     */
    LifeProductSpec toLifeProductSpec(ConfigureLifeProductDTO req);

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
    ProductClauseResponse toProductClauseResponse(ProductClauseQueryResult result);

    // ==================== 枚举 String → 领域枚举转换 ====================

    /** 定价模式 code → 定价模式枚举（null 安全）。 */
    @Named("pricingModeFromCode")
    default PricingMode pricingModeFromCode(String code) {
        return code != null ? PricingMode.fromCode(code) : null;
    }

    /** 定价模式枚举 → code（null 安全）。 */
    @Named("pricingModeCode")
    default String pricingModeCode(PricingMode pricingMode) {
        return pricingMode != null ? pricingMode.getCode() : null;
    }

    /** 产品形态 code → 产品形态枚举（null 安全）。 */
    @Named("productFormFromCode")
    default ProductEnum.ProductForm productFormFromCode(String code) {
        return code != null ? ProductEnum.ProductForm.fromCode(code) : null;
    }

    /** 险种 code → 险种枚举（null 安全）。 */
    @Named("insuranceTypeFromCode")
    default InsuranceProductType insuranceTypeFromCode(String code) {
        return code != null ? InsuranceProductType.fromCode(code) : null;
    }

    /** 产品类别 code → 产品类别枚举（null 安全）。 */
    @Named("productCategoryFromCode")
    default ProductEnum.ProductCategory productCategoryFromCode(String code) {
        return code != null ? ProductEnum.ProductCategory.fromCode(code) : null;
    }

    /** 定价类型 code → 定价类型枚举（null 安全）。 */
    @Named("pricingTypeFromCode")
    default ProductEnum.PricingType pricingTypeFromCode(String code) {
        return code != null ? ProductEnum.PricingType.fromCode(code) : null;
    }
}
