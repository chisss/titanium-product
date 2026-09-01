package com.titanium.product.application.query.pricing.lifecycle;

import org.springframework.stereotype.Service;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.repository.PremiumLifecycleAdjustmentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 生命周期费用差额读侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class PremiumLifecycleAdjustmentQueryAppService {

    private final PremiumLifecycleAdjustmentRepository adjustmentRepository;

    public PremiumLifecycleAdjustment get(String tenantId, String adjustmentId) {
        return adjustmentRepository.findById(tenantId, adjustmentId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_CALCULATION_NOT_FOUND));
    }
}
