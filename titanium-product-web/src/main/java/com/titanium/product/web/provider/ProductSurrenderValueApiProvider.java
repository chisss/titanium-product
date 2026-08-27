package com.titanium.product.web.provider;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.api.ProductSurrenderValueApi;
import com.titanium.product.api.request.SurrenderValueCalculationRequest;
import com.titanium.product.api.response.SurrenderValueCalculationResponse;
import com.titanium.product.application.command.pricing.surrender.CalculateSurrenderValueCommand;
import com.titanium.product.application.command.pricing.surrender.SurrenderValueCommandAppService;
import com.titanium.product.application.model.pricing.surrender.SurrenderValueCalculationResult;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Product 退保价值远程契约实现。 */
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductSurrenderValueApiProvider implements ProductSurrenderValueApi {

    private final SurrenderValueCommandAppService commandAppService;

    @Override
    public ApiResponse<SurrenderValueCalculationResponse> calculate(
            @Valid SurrenderValueCalculationRequest request, String tenantId) {
        SurrenderValueCalculationResult result = commandAppService.calculate(
                new CalculateSurrenderValueCommand(
                        tenantId, request.surrenderRequestId(), request.bizNo(), request.originalBizNo(),
                        request.originalCalculationId(),
                        request.policyEffectiveDate(), request.surrenderDate(), request.policyYear(),
                        request.businessTime(), request.reason()));
        PremiumLifecycleAdjustment adjustment = result.adjustment();
        return ApiResponse.success(new SurrenderValueCalculationResponse(
                result.surrenderRequestId(), result.policyCode(), result.policyVersion(), result.policyContentHash(),
                result.policyYear(), result.coolingOffDays(), result.refundType().name(),
                result.withinCoolingOff(), result.cashValueRate(), result.refundAmount(),
                result.retainedCustomerAmount(), result.internalCostRetentionRate(),
                adjustment.getOriginalCalculationId(), result.originalResultHash(),
                adjustment.getReplacementCalculationId(), result.replacementResultHash(),
                adjustment.getAdjustmentId(), result.requestHash(), adjustment.getResultHash(),
                result.pricingPlanVersion(), result.pricingPlanContentHash(), adjustment.getDirection().getCode(),
                adjustment.getCustomerAmount(), adjustment.getCurrency()));
    }
}
