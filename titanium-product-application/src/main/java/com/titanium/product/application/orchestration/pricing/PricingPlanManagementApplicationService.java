package com.titanium.product.application.orchestration.pricing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.CalculationModelDefinition;
import com.titanium.product.aggregate.DynamicFactorDefinition;
import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.aggregate.TaxPolicyDefinition;
import com.titanium.product.application.command.pricing.CreatePricingPlanDraftCommand;
import com.titanium.product.application.command.pricing.PricingTestCaseDraft;
import com.titanium.product.application.command.pricing.ReplacePricingTestCasesCommand;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.port.CommissionResolutionPort;
import com.titanium.product.port.PricingPlanRepository;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.CalculationModelRepository;
import com.titanium.product.repository.DynamicFactorRepository;
import com.titanium.product.repository.TaxPolicyRepository;
import com.titanium.product.valueobject.pricing.CommissionSchemeRef;
import com.titanium.product.valueobject.pricing.CommissionSchemeValidationRequest;
import com.titanium.product.valueobject.pricing.DynamicFactorRef;
import com.titanium.product.valueobject.pricing.PricingFeatureContract;
import com.titanium.product.valueobject.pricing.PricingPlanValidationResult;
import com.titanium.product.valueobject.pricing.PricingTestCase;
import com.titanium.product.valueobject.pricing.TaxPolicyRef;

import lombok.RequiredArgsConstructor;

/**
 * Product 定价方案后台管理编排。
 */
@Service
@RequiredArgsConstructor
public class PricingPlanManagementApplicationService {

    private final ProductQueryService productQueryService;
    private final PricingPlanRepository pricingPlanRepository;
    private final CalculationModelRepository calculationModelRepository;
    private final TaxPolicyRepository taxPolicyRepository;
    private final DynamicFactorRepository dynamicFactorRepository;
    private final PricingPlanTestRunner pricingPlanTestRunner;
    private final CommissionResolutionPort commissionResolutionPort;

    @Transactional
    public String createDraft(CreatePricingPlanDraftCommand command) {
        validateProduct(command.productId(), command.productVersion(), command.tenantId());
        if (pricingPlanRepository.existsByBusinessKey(
                command.tenantId(), command.productId(), command.planVersion())) {
            throw new BusinessException(ProductErrorCode.PRICING_PLAN_ALREADY_EXISTS);
        }
        validateCalculationModel(command);
        validateTaxPolicies(
                command.tenantId(), command.productId(), command.effectiveFrom(), command.taxPolicyRefs());
        validateCommissionSchemes(
                command.tenantId(), command.productId(), command.currency(), command.effectiveFrom(),
                command.commissionSchemeRefs());
        validateDynamicFactors(
                command.tenantId(), command.productId(), command.effectiveFrom(), command.dynamicFactorRefs(),
                command.featureContract());
        String planId = UUID.randomUUID().toString();
        PricingPlanDefinition plan = PricingPlanDefinition.createDraft(
                planId, command.productId(), command.productVersion(), command.planVersion(), command.mode(),
                command.currency(), command.effectiveFrom(), command.effectiveTo(), command.rateTableRef(),
                command.featureContract(), command.artifactRef(), command.calculationModelRef(),
                command.taxPolicyRefs(), command.commissionSchemeRefs(), command.dynamicFactorRefs(),
                command.roundingRule(), command.tenantId());
        pricingPlanRepository.save(plan);
        return planId;
    }

    @Transactional
    public void replaceTestCases(ReplacePricingTestCasesCommand command) {
        PricingPlanDefinition plan = requirePlan(command.tenantId(), command.productId(), command.planId());
        plan.replaceTestCases(command.testCases().stream().map(this::toTestCase).toList());
        pricingPlanRepository.save(plan);
    }

    @Transactional
    public String approve(String tenantId, String productId, String planId) {
        PricingPlanDefinition plan = requirePlan(tenantId, productId, planId);
        validateTaxPolicies(tenantId, productId, plan.effectiveFrom(), plan.taxPolicyRefs());
        validateCommissionSchemes(
                tenantId, productId, plan.currency(), plan.effectiveFrom(), plan.commissionSchemeRefs());
        validateDynamicFactors(
                tenantId, productId, plan.effectiveFrom(), plan.dynamicFactorRefs(), plan.featureContract());
        String contentHash = plan.approve();
        pricingPlanRepository.save(plan);
        return contentHash;
    }

    @Transactional(readOnly = true)
    public PricingPlanValidationResult runTests(String tenantId, String productId, String planId) {
        PricingPlanDefinition plan = requirePlan(tenantId, productId, planId);
        if (plan.status() != PricingPlanStatus.APPROVED && plan.status() != PricingPlanStatus.PUBLISHED) {
            throw new BusinessException(ProductErrorCode.PRICING_PLAN_STATUS_INVALID);
        }
        return pricingPlanTestRunner.run(plan);
    }

    @Transactional
    public PricingPlanValidationResult publish(String tenantId, String productId, String planId) {
        PricingPlanDefinition plan = requirePlan(tenantId, productId, planId);
        if (pricingPlanRepository.existsPublishedOverlap(
                tenantId, productId, planId, plan.currency(), plan.effectiveFrom(), plan.effectiveTo())) {
            throw new BusinessException(ProductErrorCode.PRICING_PLAN_EFFECTIVE_PERIOD_CONFLICT);
        }
        validateCommissionSchemes(
                tenantId, productId, plan.currency(), plan.effectiveFrom(), plan.commissionSchemeRefs());
        validateDynamicFactors(
                tenantId, productId, plan.effectiveFrom(), plan.dynamicFactorRefs(), plan.featureContract());
        PricingPlanValidationResult validation = pricingPlanTestRunner.run(plan);
        plan.publish(validation);
        pricingPlanRepository.save(plan);
        return validation;
    }

    @Transactional
    public void retire(String tenantId, String productId, String planId) {
        PricingPlanDefinition plan = requirePlan(tenantId, productId, planId);
        plan.retire();
        pricingPlanRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public PricingPlanDefinition get(String tenantId, String productId, String planId) {
        return requirePlan(tenantId, productId, planId);
    }

    @Transactional(readOnly = true)
    public List<PricingPlanDefinition> list(
            String tenantId, String productId, PricingPlanStatus status) {
        validateProduct(productId, null, tenantId);
        return pricingPlanRepository.findAll(tenantId, productId, status);
    }

    private PricingPlanDefinition requirePlan(String tenantId, String productId, String planId) {
        return pricingPlanRepository.findById(tenantId, productId, planId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_PLAN_NOT_FOUND));
    }

    private void validateCalculationModel(CreatePricingPlanDraftCommand command) {
        if (command.calculationModelRef() == null) {
            return;
        }
        CalculationModelDefinition model = calculationModelRepository.findPublished(
                        command.tenantId(), command.productId(), command.calculationModelRef().modelCode(),
                        command.calculationModelRef().modelVersion(), command.effectiveFrom())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_MODEL_NOT_FOUND));
        if (!model.getContentHash().equals(command.calculationModelRef().contentHash())
                || !model.getCurrency().equalsIgnoreCase(command.currency())) {
            throw new BusinessException(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
        }
    }

    private void validateTaxPolicies(
            String tenantId,
            String productId,
            LocalDateTime effectiveFrom,
            List<TaxPolicyRef> references) {
        for (TaxPolicyRef reference : references == null ? List.<TaxPolicyRef>of() : references) {
            TaxPolicyDefinition policy = taxPolicyRepository.findPublished(
                            tenantId, productId, reference.policyCode(), reference.policyVersion(), effectiveFrom)
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND));
            if (!reference.contentHash().equals(policy.getContentHash())) {
                throw new BusinessException(
                        "税费策略版本证据与已发布内容不一致: " + reference.policyCode(),
                        ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
            }
        }
    }

    private void validateCommissionSchemes(
            String tenantId,
            String productId,
            String currency,
            LocalDateTime businessTime,
            List<CommissionSchemeRef> references) {
        for (CommissionSchemeRef reference : references == null ? List.<CommissionSchemeRef>of() : references) {
            commissionResolutionPort.validate(new CommissionSchemeValidationRequest(
                    tenantId, productId, currency, businessTime, reference));
        }
    }

    private void validateDynamicFactors(
            String tenantId,
            String productId,
            LocalDateTime businessTime,
            List<DynamicFactorRef> references,
            PricingFeatureContract featureContract) {
        for (DynamicFactorRef reference : references == null ? List.<DynamicFactorRef>of() : references) {
            DynamicFactorDefinition factor = dynamicFactorRepository.findPublished(
                            tenantId, productId, reference.factorCode(), reference.factorVersion(), businessTime)
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND));
            if (!reference.contentHash().equals(factor.getContentHash())) {
                throw new BusinessException(
                        "动态因子版本证据与已发布内容不一致: " + reference.factorCode(),
                        ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
            }
            boolean contractMatches = featureContract != null && featureContract.requirements().stream()
                    .anyMatch(requirement -> requirement.featureCode().equalsIgnoreCase(factor.getFeatureCode())
                            && factor.getFeatureDefinitionVersion().equals(requirement.definitionVersion()));
            if (!contractMatches) {
                throw new BusinessException(
                        "动态因子未绑定精确 Feature Center 特征版本: " + reference.factorCode(),
                        ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
            }
        }
    }

    private void validateProduct(String productId, String productVersion, String tenantId) {
        ProductQueryResult product = productQueryService.findProductById(productId, tenantId);
        if (product == null) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_EXIST);
        }
        if (productVersion != null && !productVersion.equals(product.getVersion())) {
            throw new BusinessException(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED);
        }
    }

    private PricingTestCase toTestCase(PricingTestCaseDraft draft) {
        return new PricingTestCase(
                UUID.randomUUID().toString(), draft.caseCode(), draft.description(), draft.businessTime(),
                draft.sumInsured(), draft.age(), draft.gender(), draft.paymentTermYears(),
                draft.coverageTermYears(), draft.paymentPeriods(), draft.requestSnapshot(),
                draft.expectedPremium(), draft.tolerance());
    }
}
