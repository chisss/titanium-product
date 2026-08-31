package com.titanium.product.infrastructure.pricing.adapter;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.infrastructure.mapper.PricingPlanPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.PricingPlanCommissionSchemeRefDO;
import com.titanium.product.infrastructure.pricing.entity.PricingPlanDO;
import com.titanium.product.infrastructure.pricing.entity.PricingPlanDynamicFactorRefDO;
import com.titanium.product.infrastructure.pricing.entity.PricingPlanTaxPolicyRefDO;
import com.titanium.product.infrastructure.pricing.entity.PricingTestCaseDO;
import com.titanium.product.infrastructure.pricing.repository.PricingPlanCommissionSchemeRefJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PricingPlanDynamicFactorRefJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PricingPlanJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PricingPlanTaxPolicyRefJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PricingTestCaseJpaRepository;
import com.titanium.product.repository.PricingPlanRepository;
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
    private final PricingPlanPersistenceMapper persistenceMapper;

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
        List<PricingPlanDO> plans = status == null
                ? pricingPlanJpaRepository.findByTenantIdAndProductIdOrderByCreateTimeDesc(tenantId, productId)
                : pricingPlanJpaRepository.findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
                        tenantId, productId, status);
        return plans.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PricingPlanDefinition> findEffective(
            String tenantId, String productId, String currency, LocalDateTime businessTime) {
        List<PricingPlanDO> plans = pricingPlanJpaRepository.findEffectivePlans(
                tenantId, productId, currency, businessTime);
        if (plans.size() > 1) {
            throw new PricingDomainException(ProductErrorCode.PRICING_PLAN_EFFECTIVE_PERIOD_CONFLICT,
                    "同一业务时点存在多个有效定价方案");
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
        pricingPlanJpaRepository.save(persistenceMapper.toDO(pricingPlan));
        pricingTestCaseJpaRepository.deleteByPlanIdAndTenantId(pricingPlan.planId(), pricingPlan.tenantId());
        pricingTestCaseJpaRepository.flush();
        pricingTestCaseJpaRepository.saveAll(pricingPlan.testCases().stream()
                .map(testCase -> persistenceMapper.toDO(pricingPlan, testCase))
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

    private PricingPlanDefinition toDomain(PricingPlanDO dataObject) {
        RateTableRef rateTableRef = dataObject.getRateTableCode() == null ? null : new RateTableRef(
                null, dataObject.getRateTableCode(), dataObject.getRateTableVersion(),
                parseStringList(dataObject.getRateDimensionKeysJson()));
        PricingFeatureContract featureContract = dataObject.getFeatureContractId() == null ? null
                : new PricingFeatureContract(
                        dataObject.getFeatureContractId(), dataObject.getFeatureContractVersion(),
                        JSON.parseObject(dataObject.getFeatureRequirementsJson(), REQUIREMENT_LIST_TYPE));
        PricingRuleArtifactRef artifactRef = dataObject.getArtifactCode() == null ? null
                : new PricingRuleArtifactRef(
                        dataObject.getArtifactCode(), dataObject.getArtifactVersion(),
                        dataObject.getInputSchemaVersion(), dataObject.getArtifactHash());
        CalculationModelRef calculationModelRef = dataObject.getCalculationModelCode() == null ? null
                : new CalculationModelRef(
                        dataObject.getCalculationModelCode(), dataObject.getCalculationModelVersion(),
                        dataObject.getCalculationModelHash());
        List<PricingTestCase> testCases = pricingTestCaseJpaRepository
                .findByPlanIdAndTenantIdOrderByCaseCodeAsc(dataObject.getPlanId(), dataObject.getTenantId())
                .stream()
                .map(this::toDomain)
                .toList();
        List<TaxPolicyRef> taxPolicyRefs = taxPolicyRefJpaRepository
                .findByPlanIdOrderBySortOrderAsc(dataObject.getPlanId()).stream()
                .map(ref -> new TaxPolicyRef(ref.getPolicyCode(), ref.getPolicyVersion(), ref.getPolicyHash()))
                .toList();
        List<CommissionSchemeRef> commissionSchemeRefs = commissionSchemeRefJpaRepository
                .findByPlanIdOrderBySortOrderAsc(dataObject.getPlanId()).stream()
                .map(ref -> new CommissionSchemeRef(
                        ref.getChannelId(), ref.getSchemeCode(), ref.getSchemeVersion(), ref.getSchemeHash()))
                .toList();
        List<DynamicFactorRef> dynamicFactorRefs = dynamicFactorRefJpaRepository
                .findByPlanIdOrderBySortOrderAsc(dataObject.getPlanId()).stream()
                .map(ref -> new DynamicFactorRef(
                        ref.getFactorCode(), ref.getFactorVersion(), ref.getFactorHash()))
                .toList();
        return PricingPlanDefinition.restore(
                dataObject.getPlanId(), dataObject.getProductId(), dataObject.getProductVersion(),
                dataObject.getPlanVersion(), dataObject.getPricingMode(), dataObject.getStatus(),
                dataObject.getCurrency(), dataObject.getEffectiveFrom(), dataObject.getEffectiveTo(),
                rateTableRef, featureContract, artifactRef, calculationModelRef, taxPolicyRefs,
                commissionSchemeRefs, dynamicFactorRefs,
                new PricingRoundingRule(dataObject.getRoundingScale(),
                        RoundingMode.valueOf(dataObject.getRoundingMode())),
                dataObject.getTenantId(), testCases, dataObject.getContentHash());
    }

    private PricingTestCase toDomain(PricingTestCaseDO dataObject) {
        return new PricingTestCase(
                dataObject.getCaseId(), dataObject.getCaseCode(), dataObject.getDescription(),
                dataObject.getBusinessTime(), dataObject.getSumInsured(), dataObject.getAge(),
                dataObject.getGender(), dataObject.getPaymentTermYears(), dataObject.getCoverageTermYears(),
                dataObject.getPaymentPeriods(),
                JSON.parseObject(dataObject.getRequestSnapshotJson(), SNAPSHOT_MAP_TYPE),
                dataObject.getExpectedPremium(), dataObject.getTolerance());
    }

    private List<PricingPlanTaxPolicyRefDO> toTaxPolicyRefEntities(PricingPlanDefinition plan) {
        return IntStream.range(0, plan.taxPolicyRefs().size())
                .mapToObj(index -> {
                    TaxPolicyRef ref = plan.taxPolicyRefs().get(index);
                    String key = plan.planId() + ':' + ref.policyCode() + ':' + ref.policyVersion();
                    String refId = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
                    return persistenceMapper.toDO(plan, ref, refId, index);
                })
                .toList();
    }

    private List<PricingPlanCommissionSchemeRefDO> toCommissionSchemeRefEntities(
            PricingPlanDefinition plan) {
        return IntStream.range(0, plan.commissionSchemeRefs().size())
                .mapToObj(index -> {
                    CommissionSchemeRef ref = plan.commissionSchemeRefs().get(index);
                    String key = plan.planId() + ':' + ref.channelId() + ':' + ref.schemeCode() + ':'
                            + ref.schemeVersion();
                    String refId = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
                    return persistenceMapper.toDO(plan, ref, refId, index);
                })
                .toList();
    }

    private List<PricingPlanDynamicFactorRefDO> toDynamicFactorRefEntities(PricingPlanDefinition plan) {
        return IntStream.range(0, plan.dynamicFactorRefs().size())
                .mapToObj(index -> {
                    DynamicFactorRef ref = plan.dynamicFactorRefs().get(index);
                    String key = plan.planId() + ':' + ref.factorCode() + ':' + ref.factorVersion();
                    String refId = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
                    return persistenceMapper.toDO(plan, ref, refId, index);
                })
                .toList();
    }

    private List<String> parseStringList(String json) {
        return json == null || json.isBlank() ? RateTableRef.DEFAULT_DIMENSIONS : JSON.parseArray(json, String.class);
    }
}
