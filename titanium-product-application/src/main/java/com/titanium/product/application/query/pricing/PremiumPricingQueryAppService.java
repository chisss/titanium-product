package com.titanium.product.application.query.pricing;

import org.springframework.stereotype.Service;

import com.titanium.product.application.service.pricing.PremiumQuoteApplicationService;
import com.titanium.product.command.pricing.PremiumQuoteCommand;
import com.titanium.product.valueobject.PremiumQuote;

import lombok.RequiredArgsConstructor;

/**
 * Product 定价查询入口，屏蔽内部编排实现。
 */
@Service
@RequiredArgsConstructor
public class PremiumPricingQueryAppService {

    private final PremiumQuoteApplicationService premiumQuoteApplicationService;

    /**
     * 执行不落账的保费试算。
     */
    public PremiumQuote quote(PremiumQuoteQuery query) {
        return premiumQuoteApplicationService.quote(new PremiumQuoteCommand(
                query.tenantId(), query.productId(), query.requestId(), query.businessTime(), query.currency(),
                query.sumInsured(), query.age(), query.gender(), query.paymentTermYears(), query.coverageTermYears(),
                query.paymentPeriods(), query.requestSnapshot(), query.channelId(), query.policyYear()));
    }
}
