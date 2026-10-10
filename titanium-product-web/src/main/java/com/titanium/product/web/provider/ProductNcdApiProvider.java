package com.titanium.product.web.provider;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.context.RequestContextHolder;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductNcdApi;
import com.titanium.product.api.request.ncd.NcdResolutionRequest;
import com.titanium.product.api.response.ncd.NcdResolutionResponse;
import com.titanium.product.application.command.pricing.ncd.NcdResolutionCommandAppService;
import com.titanium.product.application.model.pricing.ncd.NcdResolutionResult;
import com.titanium.product.command.pricing.ncd.ResolveNcdCommand;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Product NCD 反算远程契约实现。
 * <p>
 * 🔴 「取不到」的一切形态都以**领域异常**上抛（{@code NCD_RULE_NOT_CONFIGURED} /
 * {@code NCD_HISTORY_UNAVAILABLE} / {@code NCD_SUBJECT_IDENTIFIER_REQUIRED} /
 * {@code NCD_BAND_NOT_MATCHED} / {@code NCD_RULE_CONFLICT}），交由
 * {@code ProductExceptionHandler} 按错误码类别定 HTTP 状态并回显业务码——
 * 刻意**不**在此兜成「成功 + 系数 1.0」：那正是 AC-03 禁止的静默回落。
 * </p>
 */
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductNcdApiProvider implements ProductNcdApi {

    private final NcdResolutionCommandAppService ncdResolutionCommandAppService;

    @Override
    public ApiResponse<NcdResolutionResponse> resolveNcd(@Valid NcdResolutionRequest request) {
        NcdResolutionResult result = ncdResolutionCommandAppService.resolve(new ResolveNcdCommand(
                RequestContextHolder.requireTenantId(), request.productId(), request.vin(),
                request.subjectId(), request.vehicleType(), request.businessTime()));
        return ApiResponse.success(new NcdResolutionResponse(result.productId(), result.coefficient(),
                result.claimCount(), result.periodFrom(), result.periodTo(), result.ruleSetCode(),
                result.ruleSetVersion(), result.ruleSetContentHash()));
    }
}
