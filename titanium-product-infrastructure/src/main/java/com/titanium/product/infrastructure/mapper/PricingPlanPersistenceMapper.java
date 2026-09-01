package com.titanium.product.infrastructure.mapper;

import java.math.RoundingMode;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.infrastructure.pricing.entity.pricing.PricingPlanCommissionSchemeRefDO;
import com.titanium.product.infrastructure.pricing.entity.pricing.PricingPlanDO;
import com.titanium.product.infrastructure.pricing.entity.pricing.PricingPlanDynamicFactorRefDO;
import com.titanium.product.infrastructure.pricing.entity.pricing.PricingPlanTaxPolicyRefDO;
import com.titanium.product.infrastructure.pricing.entity.pricing.PricingTestCaseDO;
import com.titanium.product.pricing.aggregate.PricingPlanDefinition;
import com.titanium.product.valueobject.pricing.commission.CommissionSchemeRef;
import com.titanium.product.valueobject.pricing.premium.TaxPolicyRef;
import com.titanium.product.valueobject.pricing.pricing.DynamicFactorRef;
import com.titanium.product.valueobject.pricing.pricing.PricingTestCase;

/**
 * 定价方案领域对象 → 持久化对象声明式映射。
 * 内嵌引用（费率表/特征契约/规则工件/计算模型）经嵌套路径平铺落库，
 * 特征需求快照与测试用例快照 JSON 序列化落库。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        imports = JSON.class)
public interface PricingPlanPersistenceMapper {

    /** 定价方案主表映射：嵌套引用平铺，roundingMode 枚举名落库，行数冗余列由领域对象计算。 */
    @Mapping(target = "rateTableCode", source = "rateTableRef.tableCode")
    @Mapping(target = "rateTableVersion", source = "rateTableRef.version")
    @Mapping(target = "rateDimensionKeysJson", source = "rateTableRef.dimensionKeys", qualifiedByName = "toJson")
    @Mapping(target = "featureContractId", source = "featureContract.contractId")
    @Mapping(target = "featureContractVersion", source = "featureContract.contractVersion")
    @Mapping(target = "featureRequirementsJson",
            expression = "java(plan.featureContract() == null ? \"[]\" : JSON.toJSONString(plan.featureContract().requirements()))")
    @Mapping(target = "artifactCode", source = "artifactRef.artifactCode")
    @Mapping(target = "artifactVersion", source = "artifactRef.artifactVersion")
    @Mapping(target = "inputSchemaVersion", source = "artifactRef.inputSchemaVersion")
    @Mapping(target = "artifactHash", source = "artifactRef.artifactHash")
    @Mapping(target = "calculationModelCode", source = "calculationModelRef.modelCode")
    @Mapping(target = "calculationModelVersion", source = "calculationModelRef.modelVersion")
    @Mapping(target = "calculationModelHash", source = "calculationModelRef.contentHash")
    @Mapping(target = "roundingScale", source = "roundingRule.scale")
    @Mapping(target = "roundingMode", source = "roundingRule.roundingMode", qualifiedByName = "roundingModeName")
    @Mapping(target = "testCaseCount", expression = "java(plan.testCases().size())")
    PricingPlanDO toDO(PricingPlanDefinition plan);

    /** 定价测试用例映射：planId/tenantId 沿用主表，请求快照 JSON 序列化。 */
    @Mapping(target = "planId", source = "plan.planId")
    @Mapping(target = "tenantId", source = "plan.tenantId")
    @Mapping(target = "requestSnapshotJson", source = "testCase.requestSnapshot", qualifiedByName = "toJson")
    PricingTestCaseDO toDO(PricingPlanDefinition plan, PricingTestCase testCase);

    /** 税务政策引用映射：refId 由调用方派生，policyHash 取引用内容哈希。 */
    @Mapping(target = "refId", source = "refId")
    @Mapping(target = "planId", source = "plan.planId")
    @Mapping(target = "policyCode", source = "ref.policyCode")
    @Mapping(target = "policyVersion", source = "ref.policyVersion")
    @Mapping(target = "policyHash", source = "ref.contentHash")
    @Mapping(target = "sortOrder", source = "sortOrder")
    PricingPlanTaxPolicyRefDO toDO(PricingPlanDefinition plan, TaxPolicyRef ref, String refId, int sortOrder);

    /** 佣金方案引用映射：refId 由调用方派生，schemeHash 取引用内容哈希。 */
    @Mapping(target = "refId", source = "refId")
    @Mapping(target = "planId", source = "plan.planId")
    @Mapping(target = "channelId", source = "ref.channelId")
    @Mapping(target = "schemeCode", source = "ref.schemeCode")
    @Mapping(target = "schemeVersion", source = "ref.schemeVersion")
    @Mapping(target = "schemeHash", source = "ref.contentHash")
    @Mapping(target = "sortOrder", source = "sortOrder")
    PricingPlanCommissionSchemeRefDO toDO(
            PricingPlanDefinition plan, CommissionSchemeRef ref, String refId, int sortOrder);

    /** 动态因子引用映射：refId 由调用方派生，factorHash 取引用内容哈希。 */
    @Mapping(target = "refId", source = "refId")
    @Mapping(target = "planId", source = "plan.planId")
    @Mapping(target = "factorCode", source = "ref.factorCode")
    @Mapping(target = "factorVersion", source = "ref.factorVersion")
    @Mapping(target = "factorHash", source = "ref.contentHash")
    @Mapping(target = "sortOrder", source = "sortOrder")
    PricingPlanDynamicFactorRefDO toDO(
            PricingPlanDefinition plan, DynamicFactorRef ref, String refId, int sortOrder);

    /** 任意对象 → JSON 字符串（null 安全，与适配器原语义一致）。 */
    @Named("toJson")
    default String toJson(Object value) {
        return value != null ? JSON.toJSONString(value) : null;
    }

    /** 舍入模式枚举 → 枚举名落库。 */
    @Named("roundingModeName")
    default String roundingModeName(RoundingMode roundingMode) {
        return roundingMode != null ? roundingMode.name() : null;
    }
}
