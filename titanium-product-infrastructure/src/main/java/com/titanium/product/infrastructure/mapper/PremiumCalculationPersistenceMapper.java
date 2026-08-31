package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.infrastructure.pricing.entity.CalculationLineDO;
import com.titanium.product.infrastructure.pricing.entity.CalculationLineId;
import com.titanium.product.infrastructure.pricing.entity.CalculationTotalDO;
import com.titanium.product.infrastructure.pricing.entity.PremiumCalculationDO;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationTotals;

/**
 * 确认计算领域对象 → 持久化对象声明式映射。
 * 证据快照字段平铺落库、列表型快照 JSON 序列化落库；
 * 明细行复合主键 {@link CalculationLineId} 由计算ID与行标识构成。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        imports = CalculationLineId.class)
public interface PremiumCalculationPersistenceMapper {

    /** 确认计算主表映射：证据快照字段经嵌套路径平铺，快照列表 JSON 序列化。 */
    @Mapping(target = "productVersion", source = "evidence.productVersion")
    @Mapping(target = "pricingPlanVersion", source = "evidence.pricingPlanVersion")
    @Mapping(target = "pricingPlanContentHash", source = "evidence.pricingPlanContentHash")
    @Mapping(target = "rateTableCode", source = "evidence.rateTableCode")
    @Mapping(target = "rateTableVersion", source = "evidence.rateTableVersion")
    @Mapping(target = "rateTableContentHash", source = "evidence.rateTableContentHash")
    @Mapping(target = "featureSnapshotId", source = "evidence.featureSnapshotId")
    @Mapping(target = "dynamicFactorEvidenceJson", source = "evidence.dynamicFactorEvidence", qualifiedByName = "toJson")
    @Mapping(target = "ruleArtifactCode", source = "evidence.ruleArtifactCode")
    @Mapping(target = "ruleArtifactVersion", source = "evidence.ruleArtifactVersion")
    @Mapping(target = "ruleArtifactHash", source = "evidence.ruleArtifactHash")
    @Mapping(target = "calculationModelCode", source = "evidence.calculationModelCode")
    @Mapping(target = "calculationModelVersion", source = "evidence.calculationModelVersion")
    @Mapping(target = "calculationModelHash", source = "evidence.calculationModelHash")
    @Mapping(target = "roundingScale", source = "evidence.roundingScale")
    @Mapping(target = "roundingMode", source = "evidence.roundingMode")
    @Mapping(target = "adjustmentsJson", source = "adjustments", qualifiedByName = "toJson")
    @Mapping(target = "requestSnapshotJson", source = "requestSnapshot", qualifiedByName = "toJson")
    @Mapping(target = "createTime", source = "createdAt")
    PremiumCalculationDO toDO(PremiumCalculation calculation);

    /** 合计行映射：calculationId 由调用方传入。 */
    @Mapping(target = "calculationId", source = "calculationId")
    CalculationTotalDO toDO(String calculationId, CalculationTotals totals);

    /** 明细行映射：复合主键由 calculationId 与行标识构成，税务/佣金证据按需平铺。 */
    @Mapping(target = "id",
            expression = "java(new CalculationLineId(calculationId, line.lineId()))")
    @Mapping(target = "jurisdictionCode", source = "line.taxEvidence.jurisdictionCode")
    @Mapping(target = "regulatoryReferenceId", source = "line.taxEvidence.regulatoryReferenceId")
    @Mapping(target = "taxPriceMode", source = "line.taxEvidence.priceMode")
    @Mapping(target = "taxPolicyHash", source = "line.taxEvidence.policyHash")
    @Mapping(target = "taxExempt", source = "line.taxEvidence.exempt")
    @Mapping(target = "commissionChannelId", source = "line.commissionEvidence.channelId")
    @Mapping(target = "commissionSchemeCode", source = "line.commissionEvidence.schemeCode")
    @Mapping(target = "commissionSchemeVersion", source = "line.commissionEvidence.schemeVersion")
    @Mapping(target = "commissionSchemeHash", source = "line.commissionEvidence.schemeHash")
    @Mapping(target = "commissionBeneficiaryType", source = "line.commissionEvidence.beneficiaryType")
    @Mapping(target = "commissionBeneficiaryId", source = "line.commissionEvidence.beneficiaryId")
    @Mapping(target = "commissionSplitRate", source = "line.commissionEvidence.splitRate")
    @Mapping(target = "commissionGrossAmount", source = "line.commissionEvidence.grossCommission")
    @Mapping(target = "commissionInstallmentCount", source = "line.commissionEvidence.installmentCount")
    @Mapping(target = "commissionClawbackMonths", source = "line.commissionEvidence.clawbackMonths")
    CalculationLineDO toDO(String calculationId, CalculationLine line);

    /** 任意对象 → JSON 字符串。 */
    @Named("toJson")
    default String toJson(Object value) {
        return JSON.toJSONString(value);
    }
}
