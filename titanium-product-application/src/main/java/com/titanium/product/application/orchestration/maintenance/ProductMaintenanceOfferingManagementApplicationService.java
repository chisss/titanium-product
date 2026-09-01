package com.titanium.product.application.orchestration.maintenance;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.command.maintenance.CreateProductMaintenanceOfferingCommand;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.common.enums.ProductMaintenanceOfferingFailureReason;
import com.titanium.product.exception.ProductMaintenanceOfferingException;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;
import com.titanium.product.maintenance.repository.ProductMaintenanceOfferingRepository;
import com.titanium.product.pricing.aggregate.PricingPlanDefinition;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.PricingPlanRepository;

import lombok.RequiredArgsConstructor;

/** Product 保全 Offering 管理与权威解析编排。 */
@Service
@RequiredArgsConstructor
public class ProductMaintenanceOfferingManagementApplicationService {

    private final ProductQueryService productQueryService;
    private final PricingPlanRepository pricingPlanRepository;
    private final ProductMaintenanceOfferingRepository offeringRepository;

    /** 创建经产品和计划版本校验的 Offering 草稿。 */
    @Transactional
    public String createDraft(CreateProductMaintenanceOfferingCommand command) {
        validateProductVersion(command.tenantId(), command.productId(), command.productVersion());
        validatePlanVersion(
                command.tenantId(), command.productId(), command.productVersion(), command.planVersion(), false);
        if (offeringRepository.existsByBusinessKey(
                command.tenantId(), command.productId(), command.productVersion(),
                command.planVersion(), command.offeringVersion())) {
            throw failure(ProductMaintenanceOfferingFailureReason.ALREADY_EXISTS,
                    "同一产品、计划和Offering版本已存在");
        }
        String offeringId = UUID.randomUUID().toString();
        ProductMaintenanceOffering offering = ProductMaintenanceOffering.createDraft(
                offeringId, command.tenantId(), command.productId(), command.productVersion(),
                command.planVersion(), command.offeringVersion(), command.effectiveFrom(),
                command.effectiveTo(), command.allowedPolicyStatuses(), command.allowedChannels(),
                command.allowedItemCodes());
        offeringRepository.save(offering);
        return offeringId;
    }

    /** 发布 Offering；相同产品/计划的已发布有效期不能重叠。 */
    @Transactional
    public ProductMaintenanceOffering publish(String tenantId, String productId, String offeringId) {
        ProductMaintenanceOffering offering = requireOffering(tenantId, productId, offeringId);
        validateProductVersion(tenantId, productId, offering.productVersion());
        validatePlanVersion(
                tenantId, productId, offering.productVersion(), offering.planVersion(), true);
        if (offeringRepository.existsPublishedOverlap(
                tenantId, productId, offering.productVersion(), offering.planVersion(), offeringId,
                offering.effectiveFrom(), offering.effectiveTo())) {
            throw failure(ProductMaintenanceOfferingFailureReason.PERIOD_CONFLICT,
                    "同一产品和计划的已发布Offering有效期不能重叠");
        }
        offering.publish();
        offeringRepository.save(offering);
        return offering;
    }

    /** 退役已发布 Offering，阻止新案件继续解析该版本。 */
    @Transactional
    public ProductMaintenanceOffering retire(String tenantId, String productId, String offeringId) {
        ProductMaintenanceOffering offering = requireOffering(tenantId, productId, offeringId);
        offering.retire();
        offeringRepository.save(offering);
        return offering;
    }

    private ProductMaintenanceOffering requireOffering(
            String tenantId, String productId, String offeringId) {
        return offeringRepository.findById(tenantId, productId, offeringId)
                .orElseThrow(() -> failure(
                        ProductMaintenanceOfferingFailureReason.NOT_FOUND,
                        "Product保全Offering不存在"));
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
