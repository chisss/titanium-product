package com.titanium.product.application.query.maintenance;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.common.enums.ProductMaintenanceOfferingFailureReason;
import com.titanium.product.exception.ProductMaintenanceOfferingException;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;
import com.titanium.product.maintenance.repository.ProductMaintenanceOfferingRepository;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.PricingPlanRepository;

import lombok.RequiredArgsConstructor;

/** Product 保全 Offering 查询门面。 */
@Service
@RequiredArgsConstructor
public class ProductMaintenanceOfferingQueryService {

    private final ProductQueryService productQueryService;
    private final PricingPlanRepository pricingPlanRepository;
    private final ProductMaintenanceOfferingRepository offeringRepository;

    /** 查询指定 Offering。 */
    @Transactional(readOnly = true)
    public ProductMaintenanceOffering get(String tenantId, String productId, String offeringId) {
        return offeringRepository.findById(tenantId, productId, offeringId)
                .orElseThrow(() -> failure(
                        ProductMaintenanceOfferingFailureReason.NOT_FOUND,
                        "Product保全Offering不存在"));
    }

    /** 按完整业务上下文解析唯一已发布 Offering。 */
    @Transactional(readOnly = true)
    public ProductMaintenanceOffering resolve(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            String policyStatus,
            String source,
            LocalDateTime businessTime) {
        validateProductVersion(tenantId, productId, productVersion);
        validatePlanVersion(tenantId, productId, productVersion, planVersion, true);
        ProductMaintenanceOffering offering = offeringRepository.findEffective(
                        tenantId, productId, productVersion, planVersion, businessTime)
                .orElseThrow(() -> failure(
                        ProductMaintenanceOfferingFailureReason.NOT_FOUND,
                        "当前产品、计划和业务时点不存在已发布Offering"));
        if (!offering.appliesTo(policyStatus, source, businessTime)) {
            throw failure(ProductMaintenanceOfferingFailureReason.NOT_APPLICABLE,
                    "当前保单状态或受理渠道不适用该Offering");
        }
        return offering;
    }

    private void validateProductVersion(String tenantId, String productId, String productVersion) {
        ProductQueryResult product = productQueryService.findProductById(productId, tenantId);
        if (product == null) {
            throw failure(ProductMaintenanceOfferingFailureReason.NOT_FOUND, "Product不存在");
        }
        if (!productVersion.equals(product.getVersion())) {
            throw failure(ProductMaintenanceOfferingFailureReason.VERSION_MISMATCH,
                    "Product版本与Offering请求不一致");
        }
    }

    private void validatePlanVersion(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            boolean requirePublished) {
        PricingPlanDefinition plan = pricingPlanRepository.findByVersion(tenantId, productId, planVersion)
                .orElseThrow(() -> failure(
                        ProductMaintenanceOfferingFailureReason.VERSION_MISMATCH,
                        "PricingPlan版本不存在"));
        if (!productVersion.equals(plan.productVersion())
                || requirePublished && plan.status() != PricingPlanStatus.PUBLISHED) {
            throw failure(ProductMaintenanceOfferingFailureReason.VERSION_MISMATCH,
                    "PricingPlan产品版本不匹配或尚未发布");
        }
    }

    private ProductMaintenanceOfferingException failure(
            ProductMaintenanceOfferingFailureReason reason, String message) {
        return new ProductMaintenanceOfferingException(reason, message);
    }
}
