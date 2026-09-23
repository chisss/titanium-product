package com.titanium.product.web.provider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.context.RequestContextHolder;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductSurrenderValueApi;
import com.titanium.product.api.request.premium.SurrenderValueCalculationRequest;
import com.titanium.product.api.response.premium.CashValueQueryResponse;
import com.titanium.product.api.response.premium.SurrenderValueCalculationResponse;
import com.titanium.product.application.command.pricing.surrender.SurrenderValueCommandAppService;
import com.titanium.product.application.model.pricing.surrender.CashValueSnapshot;
import com.titanium.product.application.model.pricing.surrender.SurrenderValueCalculationResult;
import com.titanium.product.application.query.pricing.surrender.CashValueQueryAppService;
import com.titanium.product.command.pricing.surrender.CalculateSurrenderValueCommand;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Product 退保价值远程契约实现。 */
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductSurrenderValueApiProvider implements ProductSurrenderValueApi {

    private final SurrenderValueCommandAppService commandAppService;
    private final CashValueQueryAppService cashValueQueryAppService;

    @Override
    public ApiResponse<SurrenderValueCalculationResponse> calculate(
            @Valid SurrenderValueCalculationRequest request) {
        SurrenderValueCalculationResult result = commandAppService.calculate(
                new CalculateSurrenderValueCommand(
                        RequestContextHolder.requireTenantId(), request.surrenderRequestId(), request.bizNo(), request.originalBizNo(),
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

    @Override
    public ApiResponse<CashValueQueryResponse> queryCashValue(String productId, int policyYear,
            BigDecimal customerPayable, LocalDate policyEffectiveDate, LocalDate valuationDate,
            LocalDateTime businessTime) {
        Optional<CashValueSnapshot> snapshot = cashValueQueryAppService.query(RequestContextHolder.requireTenantId(), productId, policyYear,
                customerPayable, policyEffectiveDate, valuationDate, businessTime);
        // 该时点无已发布退保价值策略：返回业务码失败（非 HTTP 错误），上游据此降级为「取不到现金价值」
        return snapshot
                .map(value -> ApiResponse.success(new CashValueQueryResponse(value.productId(), value.policyYear(),
                        value.coolingOffDays(), value.withinCoolingOff(), value.refundType(), value.cashValueRate(),
                        value.cashValue(), value.policyCode(), value.policyVersion(), value.policyContentHash())))
                .orElseGet(() -> ApiResponse.error(ProductErrorCode.SURRENDER_VALUE_POLICY_NOT_FOUND,
                        "产品 " + productId + " 第 " + policyYear + " 保单年度无适用的已发布退保价值策略"));
    }
}
