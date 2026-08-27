package com.titanium.product.application.query.pricing;

import org.springframework.stereotype.Service;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.repository.PremiumCalculationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Product 确认计算查询应用服务。
 */
@Service
@RequiredArgsConstructor
public class PremiumCalculationQueryAppService {

    private final PremiumCalculationRepository premiumCalculationRepository;

    public PremiumCalculation get(PremiumCalculationQuery query) {
        return premiumCalculationRepository.findById(query.tenantId(), query.calculationId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_CALCULATION_NOT_FOUND));
    }
}
