package com.titanium.product.infrastructure.pricing.adapter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.exception.PremiumCalculationConcurrentConflictException;
import com.titanium.product.infrastructure.pricing.entity.CalculationLineEntity;
import com.titanium.product.infrastructure.pricing.entity.CalculationLineId;
import com.titanium.product.infrastructure.pricing.entity.CalculationTotalEntity;
import com.titanium.product.infrastructure.pricing.entity.PremiumCalculationEntity;
import com.titanium.product.infrastructure.pricing.repository.CalculationLineJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.CalculationTotalJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PremiumCalculationJpaRepository;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationTotals;
import com.titanium.product.valueobject.pricing.CommissionLineEvidence;
import com.titanium.product.valueobject.pricing.DynamicFactorEvidence;
import com.titanium.product.valueobject.pricing.PremiumAdjustment;
import com.titanium.product.valueobject.pricing.PremiumCalculationEvidence;
import com.titanium.product.valueobject.pricing.TaxLineEvidence;

import lombok.RequiredArgsConstructor;

/**
 * Product 确认计算 JPA 适配器。
 */
@Repository
@RequiredArgsConstructor
public class JpaPremiumCalculationRepository implements PremiumCalculationRepository {

    private static final TypeReference<List<PremiumAdjustment>> ADJUSTMENT_LIST_TYPE =
            new TypeReference<>() { };
    private static final TypeReference<Map<String, Object>> SNAPSHOT_MAP_TYPE =
            new TypeReference<>() { };
    private static final TypeReference<List<DynamicFactorEvidence>> DYNAMIC_FACTOR_EVIDENCE_LIST_TYPE =
            new TypeReference<>() { };

    private final PremiumCalculationJpaRepository jpaRepository;
    private final CalculationTotalJpaRepository calculationTotalJpaRepository;
    private final CalculationLineJpaRepository calculationLineJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<PremiumCalculation> findById(String tenantId, String calculationId) {
        return jpaRepository.findByCalculationIdAndTenantId(calculationId, tenantId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PremiumCalculation> findByIdempotencyKey(
            String tenantId, String calculationRequestId, PricingCalculationPurpose purpose) {
        return jpaRepository.findByTenantIdAndCalculationRequestIdAndPurpose(
                tenantId, calculationRequestId, purpose).map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(PremiumCalculation calculation) {
        try {
            jpaRepository.saveAndFlush(toEntity(calculation));
            calculationTotalJpaRepository.save(toEntity(calculation.getCalculationId(), calculation.getCalculationTotals()));
            calculationLineJpaRepository.saveAll(calculation.getCalculationLines().stream()
                    .map(line -> toEntity(calculation.getCalculationId(), line))
                    .toList());
        } catch (DataIntegrityViolationException exception) {
            throw new PremiumCalculationConcurrentConflictException(exception);
        }
    }

    private PremiumCalculationEntity toEntity(PremiumCalculation calculation) {
        PremiumCalculationEntity entity = new PremiumCalculationEntity();
        entity.setCalculationId(calculation.getCalculationId());
        entity.setCalculationRequestId(calculation.getCalculationRequestId());
        entity.setBizNo(calculation.getBizNo());
        entity.setPurpose(calculation.getPurpose());
        entity.setStatus(calculation.getStatus());
        entity.setTenantId(calculation.getTenantId());
        entity.setProductId(calculation.getProductId());
        entity.setProductVersion(calculation.getEvidence().productVersion());
        entity.setBusinessTime(calculation.getBusinessTime());
        entity.setCurrency(calculation.getCurrency());
        entity.setStandardPremium(calculation.getStandardPremium());
        entity.setTotalPremium(calculation.getTotalPremium());
        entity.setInstallmentAmount(calculation.getInstallmentAmount());
        entity.setPeriods(calculation.getPeriods());
        entity.setAdjustmentsJson(JSON.toJSONString(calculation.getAdjustments()));
        entity.setRequestSnapshotJson(JSON.toJSONString(calculation.getRequestSnapshot()));
        entity.setPricingPlanVersion(calculation.getEvidence().pricingPlanVersion());
        entity.setPricingPlanContentHash(calculation.getEvidence().pricingPlanContentHash());
        entity.setRateTableCode(calculation.getEvidence().rateTableCode());
        entity.setRateTableVersion(calculation.getEvidence().rateTableVersion());
        entity.setRateTableContentHash(calculation.getEvidence().rateTableContentHash());
        entity.setFeatureSnapshotId(calculation.getEvidence().featureSnapshotId());
        entity.setDynamicFactorEvidenceJson(JSON.toJSONString(calculation.getEvidence().dynamicFactorEvidence()));
        entity.setRuleArtifactCode(calculation.getEvidence().ruleArtifactCode());
        entity.setRuleArtifactVersion(calculation.getEvidence().ruleArtifactVersion());
        entity.setRuleArtifactHash(calculation.getEvidence().ruleArtifactHash());
        entity.setCalculationModelCode(calculation.getEvidence().calculationModelCode());
        entity.setCalculationModelVersion(calculation.getEvidence().calculationModelVersion());
        entity.setCalculationModelHash(calculation.getEvidence().calculationModelHash());
        entity.setRoundingScale(calculation.getEvidence().roundingScale());
        entity.setRoundingMode(calculation.getEvidence().roundingMode());
        entity.setRequestHash(calculation.getRequestHash());
        entity.setInputHash(calculation.getInputHash());
        entity.setResultHash(calculation.getResultHash());
        entity.setCreateTime(calculation.getCreatedAt());
        return entity;
    }

    private PremiumCalculation toDomain(PremiumCalculationEntity entity) {
        PremiumCalculationEvidence evidence = new PremiumCalculationEvidence(
                entity.getProductVersion(), entity.getPricingPlanVersion(), entity.getPricingPlanContentHash(),
                entity.getRateTableCode(), entity.getRateTableVersion(), entity.getRateTableContentHash(),
                entity.getFeatureSnapshotId(), entity.getRuleArtifactCode(), entity.getRuleArtifactVersion(),
                entity.getRuleArtifactHash(), entity.getRoundingScale(), entity.getRoundingMode(),
                entity.getCalculationModelCode(), entity.getCalculationModelVersion(), entity.getCalculationModelHash(),
                parseDynamicFactorEvidence(entity.getDynamicFactorEvidenceJson()));
        CalculationTotals totals = calculationTotalJpaRepository.findById(entity.getCalculationId())
                .map(this::toDomain)
                .orElseGet(() -> CalculationTotals.customerPremium(entity.getTotalPremium()));
        List<CalculationLine> lines = calculationLineJpaRepository
                .findByIdCalculationIdOrderByIdLineIdAsc(entity.getCalculationId()).stream()
                .map(this::toDomain)
                .toList();
        return PremiumCalculation.restore(
                entity.getCalculationId(), entity.getCalculationRequestId(), entity.getBizNo(), entity.getPurpose(),
                entity.getStatus(), entity.getTenantId(), entity.getProductId(), entity.getBusinessTime(),
                entity.getCurrency(), entity.getStandardPremium(), entity.getTotalPremium(),
                entity.getInstallmentAmount(), entity.getPeriods(),
                JSON.parseObject(entity.getAdjustmentsJson(), ADJUSTMENT_LIST_TYPE), totals, lines, evidence,
                JSON.parseObject(entity.getRequestSnapshotJson(), SNAPSHOT_MAP_TYPE), entity.getRequestHash(),
                entity.getInputHash(), entity.getResultHash(), entity.getCreateTime());
    }

    private List<DynamicFactorEvidence> parseDynamicFactorEvidence(String value) {
        return value == null || value.isBlank()
                ? List.of()
                : JSON.parseObject(value, DYNAMIC_FACTOR_EVIDENCE_LIST_TYPE);
    }

    private CalculationTotalEntity toEntity(String calculationId, CalculationTotals totals) {
        CalculationTotalEntity entity = new CalculationTotalEntity();
        entity.setCalculationId(calculationId);
        entity.setPremiumSubtotal(totals.premiumSubtotal());
        entity.setTaxAndLevyTotal(totals.taxAndLevyTotal());
        entity.setCustomerPayable(totals.customerPayable());
        entity.setInternalCostTotal(totals.internalCostTotal());
        return entity;
    }

    private CalculationLineEntity toEntity(String calculationId, CalculationLine line) {
        CalculationLineEntity entity = new CalculationLineEntity();
        entity.setId(new CalculationLineId(calculationId, line.lineId()));
        entity.setComponentCode(line.componentCode());
        entity.setComponentVersion(line.componentVersion());
        entity.setCategory(line.category());
        entity.setAmountChannel(line.amountChannel());
        entity.setDirection(line.direction());
        entity.setPayerType(line.payerType());
        entity.setAccountingClass(line.accountingClass());
        entity.setCurrency(line.currency());
        entity.setBaseAmount(line.baseAmount());
        entity.setRate(line.rate());
        entity.setCalculatedAmount(line.calculatedAmount());
        entity.setNodeCode(line.nodeCode());
        entity.setCustomerVisible(line.customerVisible());
        entity.setAffectsCustomerPayable(line.affectsCustomerPayable());
        if (line.taxEvidence() != null) {
            entity.setJurisdictionCode(line.taxEvidence().jurisdictionCode());
            entity.setRegulatoryReferenceId(line.taxEvidence().regulatoryReferenceId());
            entity.setTaxPriceMode(line.taxEvidence().priceMode());
            entity.setTaxPolicyHash(line.taxEvidence().policyHash());
            entity.setTaxExempt(line.taxEvidence().exempt());
        }
        if (line.commissionEvidence() != null) {
            entity.setCommissionChannelId(line.commissionEvidence().channelId());
            entity.setCommissionSchemeCode(line.commissionEvidence().schemeCode());
            entity.setCommissionSchemeVersion(line.commissionEvidence().schemeVersion());
            entity.setCommissionSchemeHash(line.commissionEvidence().schemeHash());
            entity.setCommissionBeneficiaryType(line.commissionEvidence().beneficiaryType());
            entity.setCommissionBeneficiaryId(line.commissionEvidence().beneficiaryId());
            entity.setCommissionSplitRate(line.commissionEvidence().splitRate());
            entity.setCommissionGrossAmount(line.commissionEvidence().grossCommission());
            entity.setCommissionInstallmentCount(line.commissionEvidence().installmentCount());
            entity.setCommissionClawbackMonths(line.commissionEvidence().clawbackMonths());
        }
        entity.setDescription(line.description());
        return entity;
    }

    private CalculationTotals toDomain(CalculationTotalEntity entity) {
        return new CalculationTotals(
                entity.getPremiumSubtotal(), entity.getTaxAndLevyTotal(), entity.getCustomerPayable(),
                entity.getInternalCostTotal());
    }

    private CalculationLine toDomain(CalculationLineEntity entity) {
        TaxLineEvidence taxEvidence = entity.getTaxPriceMode() == null ? null : new TaxLineEvidence(
                entity.getJurisdictionCode(), entity.getRegulatoryReferenceId(), entity.getTaxPriceMode(),
                entity.getTaxPolicyHash(), Boolean.TRUE.equals(entity.getTaxExempt()));
        CommissionLineEvidence commissionEvidence = entity.getCommissionSchemeCode() == null ? null
                : new CommissionLineEvidence(
                        entity.getCommissionChannelId(), entity.getCommissionSchemeCode(),
                        entity.getCommissionSchemeVersion(), entity.getCommissionSchemeHash(),
                        entity.getCommissionBeneficiaryType(), entity.getCommissionBeneficiaryId(),
                        entity.getCommissionSplitRate(), entity.getCommissionGrossAmount(),
                        entity.getCommissionInstallmentCount(), entity.getCommissionClawbackMonths());
        return new CalculationLine(
                entity.getId().getLineId(), entity.getComponentCode(), entity.getComponentVersion(),
                entity.getCategory(), entity.getAmountChannel(), entity.getDirection(), entity.getPayerType(),
                entity.getAccountingClass(), entity.getCurrency(), entity.getBaseAmount(), entity.getRate(),
                entity.getCalculatedAmount(), entity.getNodeCode(), entity.isCustomerVisible(),
                entity.getDescription(), entity.isAffectsCustomerPayable(), taxEvidence, commissionEvidence);
    }
}
