package com.titanium.product.infrastructure.pricing.adapter.premium;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.exception.PremiumCalculationConcurrentConflictException;
import com.titanium.product.infrastructure.mapper.PremiumCalculationPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.calculation.CalculationLineDO;
import com.titanium.product.infrastructure.pricing.entity.calculation.CalculationTotalDO;
import com.titanium.product.infrastructure.pricing.entity.premium.PremiumCalculationDO;
import com.titanium.product.infrastructure.pricing.repository.calculation.CalculationLineJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.calculation.CalculationTotalJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.premium.PremiumCalculationJpaRepository;
import com.titanium.product.pricing.aggregate.PremiumCalculation;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.valueobject.pricing.calculation.CalculationLine;
import com.titanium.product.valueobject.pricing.calculation.CalculationTotals;
import com.titanium.product.valueobject.pricing.commission.CommissionLineEvidence;
import com.titanium.product.valueobject.pricing.premium.PremiumAdjustment;
import com.titanium.product.valueobject.pricing.premium.PremiumCalculationEvidence;
import com.titanium.product.valueobject.pricing.premium.TaxLineEvidence;
import com.titanium.product.valueobject.pricing.pricing.DynamicFactorEvidence;

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
    private final PremiumCalculationPersistenceMapper persistenceMapper;

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
            jpaRepository.saveAndFlush(persistenceMapper.toDO(calculation));
            calculationTotalJpaRepository.save(
                    persistenceMapper.toDO(calculation.getCalculationId(), calculation.getCalculationTotals()));
            calculationLineJpaRepository.saveAll(calculation.getCalculationLines().stream()
                    .map(line -> persistenceMapper.toDO(calculation.getCalculationId(), line))
                    .toList());
        } catch (DataIntegrityViolationException exception) {
            throw new PremiumCalculationConcurrentConflictException(exception);
        }
    }

    private PremiumCalculation toDomain(PremiumCalculationDO dataObject) {
        PremiumCalculationEvidence evidence = new PremiumCalculationEvidence(
                dataObject.getProductVersion(), dataObject.getPricingPlanVersion(),
                dataObject.getPricingPlanContentHash(), dataObject.getRateTableCode(),
                dataObject.getRateTableVersion(), dataObject.getRateTableContentHash(),
                dataObject.getFeatureSnapshotId(), dataObject.getRuleArtifactCode(),
                dataObject.getRuleArtifactVersion(), dataObject.getRuleArtifactHash(),
                dataObject.getRoundingScale(), dataObject.getRoundingMode(),
                dataObject.getCalculationModelCode(), dataObject.getCalculationModelVersion(),
                dataObject.getCalculationModelHash(),
                parseDynamicFactorEvidence(dataObject.getDynamicFactorEvidenceJson()));
        CalculationTotals totals = calculationTotalJpaRepository.findById(dataObject.getCalculationId())
                .map(this::toDomain)
                .orElseGet(() -> CalculationTotals.customerPremium(dataObject.getTotalPremium()));
        List<CalculationLine> lines = calculationLineJpaRepository
                .findByIdCalculationIdOrderByIdLineIdAsc(dataObject.getCalculationId()).stream()
                .map(this::toDomain)
                .toList();
        return PremiumCalculation.restore(
                dataObject.getCalculationId(), dataObject.getCalculationRequestId(), dataObject.getBizNo(),
                dataObject.getPurpose(), dataObject.getStatus(), dataObject.getTenantId(),
                dataObject.getProductId(), dataObject.getBusinessTime(), dataObject.getCurrency(),
                dataObject.getStandardPremium(), dataObject.getTotalPremium(),
                dataObject.getInstallmentAmount(), dataObject.getPeriods(),
                JSON.parseObject(dataObject.getAdjustmentsJson(), ADJUSTMENT_LIST_TYPE), totals, lines, evidence,
                JSON.parseObject(dataObject.getRequestSnapshotJson(), SNAPSHOT_MAP_TYPE),
                dataObject.getRequestHash(), dataObject.getInputHash(), dataObject.getResultHash(),
                dataObject.getCreateTime());
    }

    private List<DynamicFactorEvidence> parseDynamicFactorEvidence(String value) {
        return value == null || value.isBlank()
                ? List.of()
                : JSON.parseObject(value, DYNAMIC_FACTOR_EVIDENCE_LIST_TYPE);
    }

    private CalculationTotals toDomain(CalculationTotalDO dataObject) {
        return new CalculationTotals(
                dataObject.getPremiumSubtotal(), dataObject.getTaxAndLevyTotal(), dataObject.getCustomerPayable(),
                dataObject.getInternalCostTotal());
    }

    private CalculationLine toDomain(CalculationLineDO dataObject) {
        TaxLineEvidence taxEvidence = dataObject.getTaxPriceMode() == null ? null : new TaxLineEvidence(
                dataObject.getJurisdictionCode(), dataObject.getRegulatoryReferenceId(),
                dataObject.getTaxPriceMode(), dataObject.getTaxPolicyHash(),
                Boolean.TRUE.equals(dataObject.getTaxExempt()));
        CommissionLineEvidence commissionEvidence = dataObject.getCommissionSchemeCode() == null ? null
                : new CommissionLineEvidence(
                        dataObject.getCommissionChannelId(), dataObject.getCommissionSchemeCode(),
                        dataObject.getCommissionSchemeVersion(), dataObject.getCommissionSchemeHash(),
                        dataObject.getCommissionBeneficiaryType(), dataObject.getCommissionBeneficiaryId(),
                        dataObject.getCommissionSplitRate(), dataObject.getCommissionGrossAmount(),
                        dataObject.getCommissionInstallmentCount(), dataObject.getCommissionClawbackMonths());
        return new CalculationLine(
                dataObject.getId().getLineId(), dataObject.getComponentCode(), dataObject.getComponentVersion(),
                dataObject.getCategory(), dataObject.getAmountChannel(), dataObject.getDirection(),
                dataObject.getPayerType(), dataObject.getAccountingClass(), dataObject.getCurrency(),
                dataObject.getBaseAmount(), dataObject.getRate(), dataObject.getCalculatedAmount(),
                dataObject.getNodeCode(), dataObject.isCustomerVisible(), dataObject.getDescription(),
                dataObject.isAffectsCustomerPayable(), taxEvidence, commissionEvidence);
    }
}
