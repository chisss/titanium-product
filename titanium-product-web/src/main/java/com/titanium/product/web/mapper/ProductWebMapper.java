package com.titanium.product.web.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.titanium.common.util.SnowflakeIdGenerator;
import com.titanium.metadata.enums.insurance.InsuranceMandateType;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.api.request.product.AuditProductRequest;
import com.titanium.product.api.request.product.CreateProductRequest;
import com.titanium.product.api.request.product.UpdateCoverageSumInsuredConfigRequest;
import com.titanium.product.api.request.product.UpdateProductClauseRelRequest;
import com.titanium.product.api.response.clause.ProductClauseResponse;
import com.titanium.product.api.response.config.AdditionalStepResponse;
import com.titanium.product.api.response.config.AuditInfoResponse;
import com.titanium.product.api.response.config.CoveragePeriodConfigResponse;
import com.titanium.product.api.response.config.CoverageSumInsuredConfigResponse;
import com.titanium.product.api.response.config.CoverageSumInsuredTierResponse;
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
import com.titanium.product.command.ReviseProductCommand;
import com.titanium.product.command.UpdateCoverageSumInsuredConfigCommand;
import com.titanium.product.command.UpdateProductClauseRelCommand;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.query.result.CoverageSumInsuredConfigQueryResult;
import com.titanium.product.query.result.CoverageSumInsuredTierResult;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.valueobject.AdditionalStep;
import com.titanium.product.valueobject.AuditInfo;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.valueobject.config.CoveragePeriodConfig;
import com.titanium.product.valueobject.config.CoverageSumInsuredConfig;
import com.titanium.product.valueobject.config.DocumentConfig;
import com.titanium.product.valueobject.config.InsureCondition;
import com.titanium.product.valueobject.config.IssuanceProcessConfig;
import com.titanium.product.valueobject.config.PaymentConfig;
import com.titanium.product.valueobject.config.PolicyFormConfig;
import com.titanium.product.valueobject.config.SumInsuredTier;
import com.titanium.product.valueobject.config.UnderwritingConfig;
import com.titanium.product.web.dto.product.AuditProductDTO;
import com.titanium.product.web.dto.product.ClauseRelDTO;
import com.titanium.product.web.dto.product.ConfigureLifeProductDTO;
import com.titanium.product.web.dto.product.CreateProductDTO;
import com.titanium.product.web.dto.product.ProductVersionVO;
import com.titanium.product.web.dto.product.ReviseProductDTO;
import com.titanium.product.web.dto.product.UpdateCoverageSumInsuredConfigDTO;
import com.titanium.product.web.dto.product.UpdateProductClauseRelDTO;

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
     * HTTP DTO → 修订产品命令（Controller 用）
     * <p>
     * 修订以新版本 DRAFT 创建独立聚合：{@code newProductId} 缺省时由雪花算法生成；
     * 条款关联经 {@link #toClauseRels} 由三元组派生聚合内相对标识；定价模式编码还原为枚举。
     * 目标聚合 {@code productId} 为被修订的 EFFECTIVE 产品（修订不改写其事件流）。
     * </p>
     *
     * <p>🔴 {@code tenantId} 取自请求头（R8-01）：应用层要用它去条款域核查本次携带的条款绑定，
     * 该调用发生在命令到达聚合之前。此前未透传，使修订路径的条款门禁无法成立。</p>
     *
     * @param request 修订产品请求
     * @param productId 被修订的当前生效产品ID
     * @param tenantId 租户ID（请求头 X-Tenant-ID）
     * @return 修订产品命令
     */
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "newProductId",
            expression = "java(request.getNewProductId() != null ? request.getNewProductId() : SnowflakeIdGenerator.generate())")
    @Mapping(target = "newClauseRels", expression = "java(toClauseRels(request.getNewClauseRels()))")
    @Mapping(target = "newPricingMode", source = "request.newPricingMode", qualifiedByName = "pricingModeFromCode")
    ReviseProductCommand toCommand(ReviseProductDTO request, String productId, String tenantId);

    /**
     * 条款关联入参 → 领域值对象列表（修订命令用）
     * <p>
     * 每个三元组经 {@link ProductClauseRel} 业务便捷构造器派生相对标识（条款ID+版本）
     * 与绑定时间（now），保证聚合内标识完整，不依赖调用方手工拼装。
     * </p>
     *
     * @param rels 条款关联入参（可空）
     * @return 产品-条款关联值对象列表
     */
    default List<ProductClauseRel> toClauseRels(List<ClauseRelDTO> rels) {
        if (rels == null) {
            return null;
        }
        return rels.stream()
                .map(rel -> new ProductClauseRel(rel.clauseId(), rel.clauseVersion(),
                        Boolean.TRUE.equals(rel.isMainClause())))
                .toList();
    }

    /**
     * 条款关联入参（api 契约）→ 条款关联入参（web 形态）——**跨契约形态的唯一桥接点**。
     * <p>
     * 🔴 为何需要它：api 层自包含，其 {@code ClauseRel} 与 web 层 {@link ClauseRelDTO} 是两套独立契约
     * （项目 CLAUDE.md §3.4.11 ①），但二者落到领域后是<b>同一个</b> {@link ProductClauseRel}。
     * 本方法由 MapStruct 按同名分量生成，把 api 形态搬到 web 形态后复用 {@link #toClauseRels}，
     * 避免「三元组 → 领域值对象」这段派生逻辑写第二遍（同一个判断在两处各写一份 ⇒ 必漂移）。
     * </p>
     *
     * @param rels api 契约条款关联（可空）
     * @return web 形态条款关联（可空）
     */
    List<ClauseRelDTO> toClauseRelDTOs(List<UpdateProductClauseRelRequest.ClauseRel> rels);

    /**
     * HTTP DTO → 整体替换条款关联命令（Controller 用）
     *
     * <p>🔴 与 api 侧 {@link #toCommand(UpdateProductClauseRelRequest, String, String)} <b>平行</b>：
     * 两条入口各自翻译、产出<b>同一个</b> {@link UpdateProductClauseRelCommand}，再汇入同一个
     * application 薄门面 {@code ProductCommandAppService#updateProductClauseRel}，两侧互不依赖、不得互调。</p>
     *
     * @param request   整体替换请求（HTTP DTO）
     * @param productId 产品ID（路径变量）
     * @param tenantId  租户ID（请求头 X-Tenant-ID）
     * @return 整体替换条款关联命令
     */
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "newClauseRels", expression = "java(toClauseRels(request.getClauseRels()))")
    UpdateProductClauseRelCommand toCommand(UpdateProductClauseRelDTO request, String productId, String tenantId);

    /**
     * 远程 Request → 整体替换条款关联命令（Provider 用）
     *
     * <p>条款关联先经 {@link #toClauseRelDTOs} 从 api 契约形态搬到 web 形态，再走同一
     * {@link #toClauseRels} 派生值对象——两条入口共享同一段领域派生逻辑。</p>
     *
     * @param request   整体替换请求（api 契约）
     * @param productId 产品ID（路径变量）
     * @param tenantId  租户ID（请求头，由 Provider 从请求上下文取）
     * @return 整体替换条款关联命令
     */
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "newClauseRels",
            expression = "java(toClauseRels(toClauseRelDTOs(request.getClauseRels())))")
    UpdateProductClauseRelCommand toCommand(UpdateProductClauseRelRequest request, String productId,
                                            String tenantId);

    /**
     * HTTP DTO → 整体替换保额档次配置命令（Controller 用）
     *
     * <p>🔴 与 api 侧 {@link #toCommand(UpdateCoverageSumInsuredConfigRequest, String, String)} 是
     * MapStruct 的同名重载，靠<b>入参类型</b>区分（与本类既有的
     * {@code toCommand(UpdateProductClauseRelDTO/Request, ...)} 同法）。两条入口各自翻译出<b>同一个</b>
     * {@link UpdateCoverageSumInsuredConfigCommand}，汇入同一个 application 薄门面
     * {@code ProductCommandAppService#updateCoverageSumInsuredConfig}，互不依赖、不得互调。</p>
     *
     * <p>档次清单与档位由 MapStruct 逐层构造领域值对象（{@code List} 映射由 MapStruct 生成，
     * 自带 null 检查），七条不变量的校验在值对象紧凑构造器内完成——映射器只搬结构、不判业务。</p>
     *
     * @param request   整体替换请求（HTTP DTO）
     * @param productId 产品ID（路径变量）
     * @param tenantId  租户ID（请求头 X-Tenant-ID）
     * @return 整体替换保额档次配置命令
     */
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "tenantId", source = "tenantId")
    UpdateCoverageSumInsuredConfigCommand toCommand(UpdateCoverageSumInsuredConfigDTO request, String productId,
                                                    String tenantId);

    /**
     * 远程 Request → 整体替换保额档次配置命令（Provider 用）
     *
     * <p>与上者是同名重载（api 层自包含，其内嵌 record 与 web 层是两套独立契约，§3.4.11 ①），
     * 落到领域后产出<b>同一个</b>命令。此处不需要再加一层「api 形态 → web 形态」的桥接：
     * 与条款关联不同，本转换是<b>纯结构映射</b>，没有需要共享的手写派生逻辑。</p>
     *
     * @param request   整体替换请求（api 契约）
     * @param productId 产品ID（路径变量）
     * @param tenantId  租户ID（请求头，由 Provider 从请求上下文取）
     * @return 整体替换保额档次配置命令
     */
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "tenantId", source = "tenantId")
    UpdateCoverageSumInsuredConfigCommand toCommand(UpdateCoverageSumInsuredConfigRequest request, String productId,
                                                    String tenantId);

    /**
     * web 形态档次配置项 → 领域值对象（MapStruct 构造，七条不变量在紧凑构造器内裁决）
     *
     * @param item web 形态档次配置项
     * @return 领域值对象
     */
    CoverageSumInsuredConfig toCoverageSumInsuredConfig(UpdateCoverageSumInsuredConfigDTO.CoverageSumInsuredConfigItem item);

    /**
     * api 契约档次配置项 → 领域值对象（同名重载，靠入参类型区分）
     *
     * @param item api 契约档次配置项
     * @return 领域值对象
     */
    CoverageSumInsuredConfig toCoverageSumInsuredConfig(UpdateCoverageSumInsuredConfigRequest.CoverageSumInsuredConfigItem item);

    /**
     * web 形态档位 → 领域值对象
     *
     * @param item web 形态档位
     * @return 领域值对象
     */
    SumInsuredTier toSumInsuredTier(UpdateCoverageSumInsuredConfigDTO.SumInsuredTierItem item);

    /**
     * api 契约档位 → 领域值对象（同名重载，靠入参类型区分）
     *
     * @param item api 契约档位
     * @return 领域值对象
     */
    SumInsuredTier toSumInsuredTier(UpdateCoverageSumInsuredConfigRequest.SumInsuredTierItem item);

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
    @Mapping(target = "insuranceMandateType", source = "dto.insuranceMandateType",
            qualifiedByName = "insuranceMandateTypeFromCode")
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

    /**
     * 读模型结果 → 产品版本条目（版本历史列表用）。
     * <p>
     * 字段全部同名声明式映射；{@code status} 为领域枚举，MapStruct 按 {@code name()} 转为字符串，
     * 与 {@link #toProductResponse} 输出的 {@code ProductResponse.status} **同一取值口径**
     * （前端两处用同一套状态文案映射，不得一处给枚举名、一处给 code）。
     * </p>
     *
     * @param result 产品查询结果
     * @return 版本条目（不含配置块，配置对比由调用方逐版本取详情）
     */
    ProductVersionVO toProductVersionVO(ProductQueryResult result);

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

    /**
     * 责任级保额档次读模型结果 → 对外响应（Controller 与 Provider <b>共用</b>）
     * <p>
     * 同名字段自动映射（coverageCode/minSumInsured/maxSumInsured/allowCustomAmount/defaultTierCode），
     * 档位清单经 {@link #toSumInsuredTierResponse} 逐项映射，供承保前置链路（投保单录入）取
     * 「每个责任可填什么保额」。
     * </p>
     * <p>
     * 🔴 <b>此处只搬结构，不重算「选档 / 自由填」</b>：该裁决的唯一实现是写侧值对象
     * {@code CoverageSumInsuredConfig#resolveSelection}。读侧复制一份必然漂移，出现「产品侧算命中档位、
     * 投保侧算自由填」的两侧结论不一致。</p>
     *
     * @param result 档次配置查询结果
     * @return 档次配置响应
     */
    CoverageSumInsuredConfigResponse toCoverageSumInsuredResponse(CoverageSumInsuredConfigQueryResult result);

    /**
     * 保额档位读模型结果 → 对外响应
     *
     * @param result 档位查询结果
     * @return 档位响应
     */
    CoverageSumInsuredTierResponse toSumInsuredTierResponse(CoverageSumInsuredTierResult result);

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

    /** 险别 code → 险别枚举（null 安全，G11/g11-04；仅 api 契约的 String 形态走此转换，DTO 侧为枚举直传）。 */
    @Named("insuranceMandateTypeFromCode")
    default InsuranceMandateType insuranceMandateTypeFromCode(String code) {
        return code != null ? InsuranceMandateType.fromCode(code) : null;
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
