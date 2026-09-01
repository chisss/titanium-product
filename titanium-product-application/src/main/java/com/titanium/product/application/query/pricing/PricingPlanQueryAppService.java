package com.titanium.product.application.query.pricing;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.application.service.pricing.PricingPlanTestRunner;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.pricing.aggregate.PricingPlanDefinition;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.PricingPlanRepository;
import com.titanium.product.valueobject.pricing.pricing.PricingPlanValidationResult;

import lombok.RequiredArgsConstructor;

/**
 * 定价方案查询入口。
 */
@Service
@RequiredArgsConstructor
public class PricingPlanQueryAppService {

    private final ProductQueryService productQueryService;
    private final PricingPlanRepository pricingPlanRepository;
    private final PricingPlanTestRunner pricingPlanTestRunner;

    @Transactional(readOnly = true)
    public PricingPlanDefinition get(String tenantId, String productId, String planId) {
        return pricingPlanRepository.findById(tenantId, productId, planId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_PLAN_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<PricingPlanDefinition> list(
            String tenantId, String productId, PricingPlanStatus status) {
        validateProduct(productId, null, tenantId);
        return pricingPlanRepository.findAll(tenantId, productId, status);
    }

    @Transactional(readOnly = true)
    public PricingPlanValidationResult runTests(String tenantId, String productId, String planId) {
        PricingPlanDefinition plan = pricingPlanRepository.findById(tenantId, productId, planId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_PLAN_NOT_FOUND));
        if (plan.status() != PricingPlanStatus.APPROVED && plan.status() != PricingPlanStatus.PUBLISHED) {
            throw new BusinessException(ProductErrorCode.PRICING_PLAN_STATUS_INVALID);
        }
        return pricingPlanTestRunner.run(plan);
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
}
