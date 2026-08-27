package com.titanium.product.infrastructure.pricing.adapter;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.infrastructure.pricing.entity.PricingPlanCommissionSchemeRefEntity;
import com.titanium.product.infrastructure.pricing.entity.PricingPlanDynamicFactorRefEntity;
import com.titanium.product.infrastructure.pricing.entity.PricingPlanEntity;
import com.titanium.product.infrastructure.pricing.entity.PricingPlanTaxPolicyRefEntity;
import com.titanium.product.infrastructure.pricing.entity.PricingTestCaseEntity;
import com.titanium.product.infrastructure.pricing.repository.PricingPlanCommissionSchemeRefJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PricingPlanDynamicFactorRefJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PricingPlanJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PricingPlanTaxPolicyRefJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PricingTestCaseJpaRepository;
import com.titanium.product.port.PricingPlanRepository;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.pricing.CalculationModelRef;
import com.titanium.product.valueobject.pricing.CommissionSchemeRef;
import com.titanium.product.valueobject.pricing.DynamicFactorRef;
import com.titanium.product.valueobject.pricing.PricingFeatureContract;
import com.titanium.product.valueobject.pricing.PricingFeatureRequirement;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.pricing.PricingRuleArtifactRef;
import com.titanium.product.valueobject.pricing.PricingTestCase;
import com.titanium.product.valueobject.pricing.TaxPolicyRef;

import lombok.RequiredArgsConstructor;

/**
 * PricingPlan JPA 适配器。
 */
@Repository
@RequiredArgsConstructor
public class JpaPricingPlanRepository implements PricingPlanRepository {

    private static final TypeReference<List<PricingFeatureRequirement>> REQUIREMENT_LIST_TYPE =
            new TypeReference<>() { };
    private static final TypeReference<Map<String, Object>> SNAPSHOT_MAP_TYPE =
            new TypeReference<>() { };

    private final PricingPlanJpaRepository pricingPlanJpaRepository;
    private final PricingTestCaseJpaRepository pricingTestCaseJpaRepository;
    private final PricingPlanTaxPolicyRefJpaRepository taxPolicyRefJpaRepository;
    private final PricingPlanCommissionSchemeRefJpaRepository commissionSchemeRefJpaRepository;
    private final PricingPlanDynamicFactorRefJpaRepository dynamicFactorRefJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBusinessKey(String tenantId, String productId, String planVersion) {
        return pricingPlanJpaRepository.existsByTenantIdAndProductIdAndPlanVersion(
                tenantId, productId, planVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PricingPlanDefinition> findById(String tenantId, String productId, String planId) {
        return pricingPlanJpaRepository.findByPlanIdAndTenantIdAndProductId(planId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PricingPlanDefinition> findByVersion(
            String tenantId, String productId, String planVersion) {
        return pricingPlanJpaRepository.findByTenantIdAndProductIdAndPlanVersion(
                tenantId, productId, planVersion).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingPlanDefinition> findAll(String tenantId, String productId, PricingPlanStatus status) {
        List<PricingPlanEntity> plans = status == null
                ? pricingPlanJpaRepository.findByTenantIdAndProductIdOrderByCreateTimeDesc(tenantId, productId)
                : pricingPlanJpaRepository.findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
                        tenantId, productId, status);
        return plans.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PricingPlanDefinition> findEffective(
            String tenantId, String productId, String currency, LocalDateTime businessTime) {
        List<PricingPlanEntity> plans = pricingPlanJpaRepository.findEffectivePlans(
                tenantId, productId, currency, businessTime);
        if (plans.size() > 1) {
            throw new IllegalStateException("同一业务时点存在多个有效定价方案");
        }
        return plans.stream().findFirst().map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsPublishedOverlap(
            String tenantId,
            String productId,
            String excludedPlanId,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo) {
        return pricingPlanJpaRepository.countPublishedOverlaps(
                tenantId, productId, excludedPlanId, currency, effectiveFrom, effectiveTo) > 0;
    }

    @Override
    @Transactional
    public void save(PricingPlanDefinition pricingPlan) {
        pricingPlanJpaRepository.save(toEntity(pricingPlan));
        pricingTestCaseJpaRepository.deleteByPlanIdAndTenantId(pricingPlan.planId(), pricingPlan.tenantId());
        pricingTestCaseJpaRepository.flush();
        pricingTestCaseJpaRepository.saveAll(pricingPlan.testCases().stream()
                .map(testCase -> toEntity(pricingPlan, testCase))
                .toList());
        taxPolicyRefJpaRepository.deleteByPlanId(pricingPlan.planId());
        taxPolicyRefJpaRepository.flush();
        taxPolicyRefJpaRepository.saveAll(toTaxPolicyRefEntities(pricingPlan));
        commissionSchemeRefJpaRepository.deleteByPlanId(pricingPlan.planId());
        commissionSchemeRefJpaRepository.flush();
        commissionSchemeRefJpaRepository.saveAll(toCommissionSchemeRefEntities(pricingPlan));
        dynamicFactorRefJpaRepository.deleteByPlanId(pricingPlan.planId());
        dynamicFactorRefJpaRepository.flush();
        dynamicFactorRefJpaRepository.saveAll(toDynamicFactorRefEntities(pricingPlan));
    }

    private PricingPlanDefinition toDomain(PricingPlanEntity entity) {
        RateTableRef rateTableRef = entity.getRateTableCode() == null ? null : new RateTableRef(
                null, entity.getRateTableCode(), entity.getRateTableVersion(),
                parseStringList(entity.getRateDimensionKeysJson()));
        PricingFeatureContract featureContract = entity.getFeatureContractId() == null ? null
                : new PricingFeatureContract(
                        entity.getFeatureContractId(), entity.getFeatureContractVersion(),
                        JSON.parseObject(entity.getFeatureRequirementsJson(), REQUIREMENT_LIST_TYPE));
        PricingRuleArtifactRef artifactRef = entity.getArtifactCode() == null ? null
                : new PricingRuleArtifactRef(
                        entity.getArtifactCode(), entity.getArtifactVersion(), entity.getInputSchemaVersion(),
                        entity.getArtifactHash());
        CalculationModelRef calculationModelRef = entity.getCalculationModelCode() == null ? null
                : new CalculationModelRef(
                        entity.getCalculationModelCode(), entity.getCalculationModelVersion(),
                        entity.getCalculationModelHash());
        List<PricingTestCase> testCases = pricingTestCaseJpaRepository
                .findByPlanIdAndTenantIdOrderByCaseCodeAsc(entity.getPlanId(), entity.getTenantId())
                .stream()
                .map(this::toDomain)
                .toList();
        List<TaxPolicyRef> taxPolicyRefs = taxPolicyRefJpaRepository
                .findByPlanIdOrderBySortOrderAsc(entity.getPlanId()).stream()
                .map(ref -> new TaxPolicyRef(ref.getPolicyCode(), ref.getPolicyVersion(), ref.getPolicyHash()))
                .toList();
        List<CommissionSchemeRef> commissionSchemeRefs = commissionSchemeRefJpaRepository
                .findByPlanIdOrderBySortOrderAsc(entity.getPlanId()).stream()
                .map(ref -> new CommissionSchemeRef(
                        ref.getChannelId(), ref.getSchemeCode(), ref.getSchemeVersion(), ref.getSchemeHash()))
                .toList();
        List<DynamicFactorRef> dynamicFactorRefs = dynamicFactorRefJpaRepository
                .findByPlanIdOrderBySortOrderAsc(entity.getPlanId()).stream()
                .map(ref -> new DynamicFactorRef(
                        ref.getFactorCode(), ref.getFactorVersion(), ref.getFactorHash()))
                .toList();
        return PricingPlanDefinition.restore(
                entity.getPlanId(), entity.getProductId(), entity.getProductVersion(), entity.getPlanVersion(),
                entity.getPricingMode(), entity.getStatus(), entity.getCurrency(), entity.getEffectiveFrom(),
                entity.getEffectiveTo(), rateTableRef, featureContract, artifactRef, calculationModelRef,
                taxPolicyRefs, commissionSchemeRefs, dynamicFactorRefs,
                new PricingRoundingRule(entity.getRoundingScale(), RoundingMode.valueOf(entity.getRoundingMode())),
                entity.getTenantId(), testCases, entity.getContentHash());
    }

    private PricingTestCase toDomain(PricingTestCaseEntity entity) {
        return new PricingTestCase(
                entity.getCaseId(), entity.getCaseCode(), entity.getDescription(), entity.getBusinessTime(),
                entity.getSumInsured(), entity.getAge(), entity.getGender(), entity.getPaymentTermYears(),
                entity.getCoverageTermYears(), entity.getPaymentPeriods(),
                JSON.parseObject(entity.getRequestSnapshotJson(), SNAPSHOT_MAP_TYPE),
                entity.getExpectedPremium(), entity.getTolerance());
    }

    private PricingPlanEntity toEntity(PricingPlanDefinition plan) {
        PricingPlanEntity entity = new PricingPlanEntity();
        entity.setPlanId(plan.planId());
        entity.setProductId(plan.productId());
        entity.setProductVersion(plan.productVersion());
        entity.setPlanVersion(plan.planVersion());
        entity.setPricingMode(plan.mode());
        entity.setStatus(plan.status());
        entity.setCurrency(plan.currency());
        entity.setEffectiveFrom(plan.effectiveFrom());
        entity.setEffectiveTo(plan.effectiveTo());
        mapRateTable(plan, entity);
        mapFeatureContract(plan, entity);
        mapArtifact(plan, entity);
        mapCalculationModel(plan, entity);
        entity.setRoundingScale(plan.roundingRule().scale());
        entity.setRoundingMode(plan.roundingRule().roundingMode().name());
        entity.setContentHash(plan.contentHash());
        entity.setTestCaseCount(plan.testCases().size());
        entity.setTenantId(plan.tenantId());
        return entity;
    }

    private void mapRateTable(PricingPlanDefinition plan, PricingPlanEntity entity) {
        if (plan.rateTableRef() != null) {
            entity.setRateTableCode(plan.rateTableRef().tableCode());
            entity.setRateTableVersion(plan.rateTableRef().version());
            entity.setRateDimensionKeysJson(JSON.toJSONString(plan.rateTableRef().dimensionKeys()));
        }
    }

    private void mapFeatureContract(PricingPlanDefinition plan, PricingPlanEntity entity) {
        if (plan.featureContract() == null) {
            entity.setFeatureRequirementsJson("[]");
            return;
        }
        entity.setFeatureContractId(plan.featureContract().contractId());
        entity.setFeatureContractVersion(plan.featureContract().contractVersion());
        entity.setFeatureRequirementsJson(JSON.toJSONString(plan.featureContract().requirements()));
    }

    private void mapArtifact(PricingPlanDefinition plan, PricingPlanEntity entity) {
        if (plan.artifactRef() != null) {
            entity.setArtifactCode(plan.artifactRef().artifactCode());
            entity.setArtifactVersion(plan.artifactRef().artifactVersion());
            entity.setInputSchemaVersion(plan.artifactRef().inputSchemaVersion());
            entity.setArtifactHash(plan.artifactRef().artifactHash());
        }
    }

    private void mapCalculationModel(PricingPlanDefinition plan, PricingPlanEntity entity) {
        if (plan.calculationModelRef() != null) {
            entity.setCalculationModelCode(plan.calculationModelRef().modelCode());
            entity.setCalculationModelVersion(plan.calculationModelRef().modelVersion());
            entity.setCalculationModelHash(plan.calculationModelRef().contentHash());
        }
    }

    private PricingTestCaseEntity toEntity(PricingPlanDefinition plan, PricingTestCase testCase) {
        PricingTestCaseEntity entity = new PricingTestCaseEntity();
        entity.setCaseId(testCase.caseId());
        entity.setPlanId(plan.planId());
        entity.setCaseCode(testCase.caseCode());
        entity.setDescription(testCase.description());
        entity.setBusinessTime(testCase.businessTime());
        entity.setSumInsured(testCase.sumInsured());
        entity.setAge(testCase.age());
        entity.setGender(testCase.gender());
        entity.setPaymentTermYears(testCase.paymentTermYears());
        entity.setCoverageTermYears(testCase.coverageTermYears());
        entity.setPaymentPeriods(testCase.paymentPeriods());
        entity.setRequestSnapshotJson(JSON.toJSONString(testCase.requestSnapshot()));
        entity.setExpectedPremium(testCase.expectedPremium());
        entity.setTolerance(testCase.tolerance());
        entity.setTenantId(plan.tenantId());
        return entity;
    }

    private List<PricingPlanTaxPolicyRefEntity> toTaxPolicyRefEntities(PricingPlanDefinition plan) {
        return java.util.stream.IntStream.range(0, plan.taxPolicyRefs().size())
                .mapToObj(index -> {
                    TaxPolicyRef ref = plan.taxPolicyRefs().get(index);
                    PricingPlanTaxPolicyRefEntity entity = new PricingPlanTaxPolicyRefEntity();
                    String key = plan.planId() + ':' + ref.policyCode() + ':' + ref.policyVersion();
                    entity.setRefId(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString());
                    entity.setPlanId(plan.planId());
                    entity.setPolicyCode(ref.policyCode());
                    entity.setPolicyVersion(ref.policyVersion());
                    entity.setPolicyHash(ref.contentHash());
                    entity.setSortOrder(index);
                    return entity;
                })
                .toList();
    }

    private List<PricingPlanCommissionSchemeRefEntity> toCommissionSchemeRefEntities(
            PricingPlanDefinition plan) {
        return java.util.stream.IntStream.range(0, plan.commissionSchemeRefs().size())
                .mapToObj(index -> {
                    CommissionSchemeRef ref = plan.commissionSchemeRefs().get(index);
                    PricingPlanCommissionSchemeRefEntity entity = new PricingPlanCommissionSchemeRefEntity();
                    String key = plan.planId() + ':' + ref.channelId() + ':' + ref.schemeCode() + ':'
                            + ref.schemeVersion();
                    entity.setRefId(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString());
                    entity.setPlanId(plan.planId());
                    entity.setChannelId(ref.channelId());
                    entity.setSchemeCode(ref.schemeCode());
                    entity.setSchemeVersion(ref.schemeVersion());
                    entity.setSchemeHash(ref.contentHash());
                    entity.setSortOrder(index);
                    return entity;
                })
                .toList();
    }

    private List<PricingPlanDynamicFactorRefEntity> toDynamicFactorRefEntities(PricingPlanDefinition plan) {
        return java.util.stream.IntStream.range(0, plan.dynamicFactorRefs().size())
                .mapToObj(index -> {
                    DynamicFactorRef ref = plan.dynamicFactorRefs().get(index);
                    PricingPlanDynamicFactorRefEntity entity = new PricingPlanDynamicFactorRefEntity();
                    String key = plan.planId() + ':' + ref.factorCode() + ':' + ref.factorVersion();
                    entity.setRefId(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString());
                    entity.setPlanId(plan.planId());
                    entity.setFactorCode(ref.factorCode());
                    entity.setFactorVersion(ref.factorVersion());
                    entity.setFactorHash(ref.contentHash());
                    entity.setSortOrder(index);
                    return entity;
                })
                .toList();
    }

    private List<String> parseStringList(String json) {
        return json == null || json.isBlank() ? RateTableRef.DEFAULT_DIMENSIONS : JSON.parseArray(json, String.class);
    }
}
