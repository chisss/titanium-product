package com.titanium.product.web.provider;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductPricingApi;
import com.titanium.product.api.request.PremiumQuoteRequest;
import com.titanium.product.api.response.CalculationLineResponse;
import com.titanium.product.api.response.CalculationTotalsResponse;
import com.titanium.product.api.response.PremiumQuoteResponse;
import com.titanium.product.application.query.pricing.PremiumPricingQueryAppService;
import com.titanium.product.application.query.pricing.PremiumQuoteQuery;
import com.titanium.product.valueobject.PremiumQuote;

import lombok.RequiredArgsConstructor;

/**
 * Product 定价远程契约实现。
 */
@Validated
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductPricingApiProvider implements ProductPricingApi {

    private final PremiumPricingQueryAppService premiumPricingQueryAppService;

    @Override
    public ApiResponse<PremiumQuoteResponse> quotePremium(
            String productId, PremiumQuoteRequest request, String tenantId) {
        PremiumQuote quote = premiumPricingQueryAppService.quote(new PremiumQuoteQuery(
                tenantId, productId, request.requestId(), request.businessTime(), request.currency(),
                request.sumInsured(), request.age(), request.gender(), request.paymentTermYears(),
                request.coverageTermYears(), request.paymentPeriods(), request.requestSnapshot(),
                request.channelId(), request.policyYear() == null ? 1 : request.policyYear()));
        return ApiResponse.success(toResponse(quote));
    }

    private PremiumQuoteResponse toResponse(PremiumQuote quote) {
        return new PremiumQuoteResponse(
                quote.quoteId(), quote.requestId(), quote.productId(), quote.productVersion(), quote.currency(),
                quote.totalPremium(), quote.installmentAmount(), quote.periods(), quote.matchedRate(), quote.matchedRowId(),
                quote.rateTableCode(), quote.rateTableVersion(), quote.rateTableContentHash(),
                quote.pricingPlanVersion(), quote.pricingPlanContentHash(), quote.featureSnapshotId(),
                quote.ruleArtifactCode(), quote.ruleArtifactVersion(), quote.ruleArtifactHash(),
                quote.roundingScale(), quote.roundingMode(),
                quote.inputHash(), quote.resultHash(),
                new CalculationTotalsResponse(
                        quote.calculationTotals().premiumSubtotal(), quote.calculationTotals().taxAndLevyTotal(),
                        quote.calculationTotals().customerPayable(), null),
                quote.calculationLines().stream().filter(line -> line.customerVisible()).map(line -> new CalculationLineResponse(
                        line.lineId(), line.componentCode(), line.componentVersion(), line.category().getCode(),
                        line.amountChannel().getCode(), line.direction().getCode(), line.payerType().getCode(),
                        line.accountingClass(), line.currency(), line.baseAmount(), line.rate(),
                        line.calculatedAmount(), line.nodeCode(), line.customerVisible(), line.description(),
                        line.affectsCustomerPayable(),
                        line.taxEvidence() == null ? null : line.taxEvidence().jurisdictionCode(),
                        line.taxEvidence() == null ? null : line.taxEvidence().regulatoryReferenceId(),
                        line.taxEvidence() == null ? null : line.taxEvidence().priceMode().name(),
                        line.taxEvidence() == null ? null : line.taxEvidence().policyHash(),
                        line.taxEvidence() == null ? null : line.taxEvidence().exempt(),
                        null, null, null, null, null, null, null, null, null, null)).toList(),
                quote.calculationModelCode(), quote.calculationModelVersion(), quote.calculationModelHash());
    }
}
