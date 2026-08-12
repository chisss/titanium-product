package com.titanium.product.query.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.product.event.ProductCreatedEvent;
import com.titanium.product.event.ProductRevisedEvent;
import com.titanium.product.event.ProductTemplateCreatedEvent;
import com.titanium.product.query.view.ProductTemplateView;
import com.titanium.product.query.view.ProductView;

/**
 * 产品读模型投影映射器（MapStruct，事件 → 读模型字段拷贝）
 * <p>
 * 承担产品域三个"新建型"投影的事件 record → View 字段映射，取代投影处理器中逐字段 set。采用
 * {@link MappingTarget} 就地更新既有/新建 View 实例，保留投影的 upsert 语义；
 * {@link NullValuePropertyMappingStrategy#IGNORE} 确保事件缺省字段不覆盖 View 既有值。
 * </p>
 * <p>
 * <b>职责边界</b>：仅做纯字段/值对象结构翻译（复杂值对象经 {@code toJson} 整体序列化为 JSON 列）。
 * 以下三类含运行时副作用或"仅首次"语义的字段仍由投影处理器控制，此处对应目标字段 {@code ignore}：
 * <ul>
 *   <li>审计时间戳 createTime（仅首次）/ updateTime（每次 now）——运行时 now() 副作用；</li>
 *   <li>乐观锁 version（{@code BaseView} 的 {@code @Version}，与产品业务版本号 versionNo 同名不同义）——由 JPA 维护；</li>
 *   <li>修订/创建中需从原始记录继承或按业务规则固定的字段（如修订态 status 固定 DRAFT、productCode/tenantId 继承）。</li>
 * </ul>
 * </p>
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductViewMapper {

    /** 产品创建事件 → 产品读模型（就地 upsert；复杂值对象整体序列化为 JSON 列） */
    @Mapping(target = "versionNo", source = "version")
    @Mapping(target = "insureConditionJson", source = "insureCondition", qualifiedByName = "toJson")
    @Mapping(target = "coveragePeriodJson", source = "coveragePeriod", qualifiedByName = "toJson")
    @Mapping(target = "paymentConfigJson", source = "paymentConfig", qualifiedByName = "toJson")
    @Mapping(target = "pricingBasicRuleJson", source = "pricingBasicRule", qualifiedByName = "toJson")
    @Mapping(target = "issuanceProcessConfigJson", source = "issuanceProcessConfig", qualifiedByName = "toJson")
    @Mapping(target = "policyFormConfigJson", source = "policyFormConfig", qualifiedByName = "toJson")
    @Mapping(target = "underwritingConfigJson", source = "underwritingConfig", qualifiedByName = "toJson")
    @Mapping(target = "documentConfigJson", source = "documentConfig", qualifiedByName = "toJson")
    @Mapping(target = "pricingMode", source = "pricingMode", qualifiedByName = "pricingModeCode")
    @Mapping(target = "rateTableRefJson", source = "rateTableRef", qualifiedByName = "toJson")
    @Mapping(target = "actuarialBasisJson", source = "actuarialBasis", qualifiedByName = "toJson")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyCreated(@MappingTarget ProductView view, ProductCreatedEvent event);

    /**
     * 产品修订事件 → 产品读模型（就地 upsert；修订会生成新产品ID）。
     * <p>
     * 修订事件字段以 {@code newXxx} 命名，逐字段映射到读模型；{@code productCode}/{@code tenantId} 现由事件携带，
     * 与读模型同名字段自动映射（不再从原始记录继承）；修订态 status 固定 DRAFT 属运行时业务规则，由投影处理器处理。
     * </p>
     */
    @Mapping(target = "productId", source = "newProductId")
    @Mapping(target = "versionNo", source = "newVersion")
    @Mapping(target = "productName", source = "newProductName")
    @Mapping(target = "productDesc", source = "newProductDesc")
    @Mapping(target = "form", source = "newForm")
    @Mapping(target = "insuranceType", source = "newInsuranceType")
    @Mapping(target = "category", source = "newCategory")
    @Mapping(target = "insureConditionJson", source = "newInsureCondition", qualifiedByName = "toJson")
    @Mapping(target = "coveragePeriodJson", source = "newCoveragePeriod", qualifiedByName = "toJson")
    @Mapping(target = "paymentConfigJson", source = "newPaymentConfig", qualifiedByName = "toJson")
    @Mapping(target = "pricingBasicRuleJson", source = "newPricingBasicRule", qualifiedByName = "toJson")
    @Mapping(target = "issuanceProcessConfigJson", source = "newIssuanceProcessConfig", qualifiedByName = "toJson")
    @Mapping(target = "policyFormConfigJson", source = "newPolicyFormConfig", qualifiedByName = "toJson")
    @Mapping(target = "underwritingConfigJson", source = "newUnderwritingConfig", qualifiedByName = "toJson")
    @Mapping(target = "pricingMode", source = "newPricingMode", qualifiedByName = "pricingModeCode")
    @Mapping(target = "rateTableRefJson", source = "newRateTableRef", qualifiedByName = "toJson")
    @Mapping(target = "actuarialBasisJson", source = "newActuarialBasis", qualifiedByName = "toJson")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyRevised(@MappingTarget ProductView view, ProductRevisedEvent event);

    /**
     * 产品模板创建事件 → 模板读模型（就地 upsert；复杂值对象整体序列化为 JSON 列）。
     * <p>
     * issuanceMode 含"值对象序列化否则回退 DEFAULT"的默认值语义，由投影处理器处理，此处 {@code ignore}。
     * </p>
     */
    @Mapping(target = "underwritingConfigJson", source = "underwritingConfig", qualifiedByName = "toJson")
    @Mapping(target = "maintenanceConfigJson", source = "maintenanceConfig", qualifiedByName = "toJson")
    @Mapping(target = "claimConfigJson", source = "claimsConfig", qualifiedByName = "toJson")
    // policyStructureJson 是保单结构配置(PolicyStructureConfig)列，仅由 UpdateProductTemplateCommand 写入；
    // 创建事件不承载 policyStructureConfig（原误将 policyFormConfig 序列化入此列，与读侧 PolicyStructureConfig 反序列化类型冲突），故创建期忽略
    @Mapping(target = "policyStructureJson", ignore = true)
    @Mapping(target = "issuanceMode", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyTemplateCreated(@MappingTarget ProductTemplateView view, ProductTemplateCreatedEvent event);

    /** 复杂值对象 → JSON 字符串（null 安全，与投影处理器原 toJson 语义一致） */
    @Named("toJson")
    default String toJson(Object value) {
        return value != null ? JSON.toJSONString(value) : null;
    }

    /** 定价模式枚举 → code 字符串（读模型标量列存 code，null 安全） */
    @Named("pricingModeCode")
    default String pricingModeCode(PricingMode pricingMode) {
        return pricingMode != null ? pricingMode.getCode() : null;
    }
}
