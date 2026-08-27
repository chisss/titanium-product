package com.titanium.product.infrastructure.pricing.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.titanium.channel.api.CommissionSchemeApi;
import com.titanium.channel.api.request.CommissionBaseLineRequest;
import com.titanium.channel.api.request.CommissionCalculationRequest;
import com.titanium.channel.api.response.CommissionCalculationResponse;
import com.titanium.channel.api.response.CommissionSchemeResponse;
import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.port.CommissionResolutionPort;
import com.titanium.product.valueobject.pricing.CommissionResolutionInstruction;
import com.titanium.product.valueobject.pricing.CommissionResolutionRequest;
import com.titanium.product.valueobject.pricing.CommissionResolutionResult;
import com.titanium.product.valueobject.pricing.CommissionSchemeValidationRequest;

import lombok.RequiredArgsConstructor;

/**
 * Product 调用 Channel 固定版本佣金方案的基础设施适配器。
 */
@Component
@RequiredArgsConstructor
public class CommissionResolutionAdapter implements CommissionResolutionPort {

    private final CommissionSchemeApi commissionSchemeApi;

    @Override
    public void validate(CommissionSchemeValidationRequest request) {
        CommissionSchemeResponse response = invokePublished(request);
        if (!request.reference().contentHash().equalsIgnoreCase(response.contentHash())
                || !request.reference().channelId().equals(response.channelId())
                || !request.productId().equals(response.productId())
                || !request.currency().equalsIgnoreCase(response.currency())) {
            throw invalidResponse("Channel佣金方案版本证据与定价包引用不一致", null);
        }
    }

    @Override
    public CommissionResolutionResult calculate(CommissionResolutionRequest request) {
        try {
            ApiResponse<CommissionCalculationResponse> apiResponse = commissionSchemeApi.calculate(
                    request.reference().schemeCode(), request.reference().schemeVersion(),
                    new CommissionCalculationRequest(
                            request.reference().contentHash(), request.channelId(), request.productId(),
                            request.currency(), request.businessTime(), request.policyYear(), request.paymentPeriods(),
                            request.roundingScale(), request.roundingMode().name(), request.baseComponents().stream()
                                    .map(line -> new CommissionBaseLineRequest(
                                            line.componentCode(), line.amount()))
                                    .toList()),
                    request.tenantId());
            CommissionCalculationResponse response = requireData(apiResponse, "Channel佣金计算");
            validateCalculationIdentity(request, response);
            return new CommissionResolutionResult(
                    response.schemeCode(), response.schemeVersion(), response.schemeHash(), response.channelId(),
                    response.productId(), response.currency(), response.baseAmount(), response.grossCommission(),
                    response.instructions() == null ? List.of() : response.instructions().stream()
                            .map(instruction -> new CommissionResolutionInstruction(
                                    instruction.beneficiaryType(), instruction.beneficiaryId(),
                                    instruction.splitRate(), instruction.amount(), instruction.installmentCount(),
                                    instruction.clawbackMonths()))
                            .toList());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw dependencyFailure("Channel佣金计算调用异常", exception);
        }
    }

    private CommissionSchemeResponse invokePublished(CommissionSchemeValidationRequest request) {
        try {
            ApiResponse<CommissionSchemeResponse> response = commissionSchemeApi.getPublished(
                    request.reference().schemeCode(), request.reference().schemeVersion(),
                    request.reference().channelId(), request.productId(), request.businessTime().toString(),
                    request.tenantId());
            return requireData(response, "Channel佣金方案查询");
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw dependencyFailure("Channel佣金方案查询调用异常", exception);
        }
    }

    private void validateCalculationIdentity(
            CommissionResolutionRequest request, CommissionCalculationResponse response) {
        if (!request.reference().schemeCode().equals(response.schemeCode())
                || !request.reference().schemeVersion().equals(response.schemeVersion())
                || !request.reference().contentHash().equalsIgnoreCase(response.schemeHash())
                || !request.channelId().equals(response.channelId())
                || !request.productId().equals(response.productId())
                || !request.currency().equalsIgnoreCase(response.currency())) {
            throw invalidResponse("Channel佣金计算响应标识与请求不一致", null);
        }
    }

    private <T> T requireData(ApiResponse<T> response, String operation) {
        if (response == null) {
            throw invalidResponse(operation + "响应为空", null);
        }
        if (!response.isSuccess()) {
            throw dependencyFailure(
                    operation + "返回业务失败: " + response.getCode() + ", " + response.getMessage(), null);
        }
        if (response.getData() == null) {
            throw invalidResponse(operation + "成功响应缺少data", null);
        }
        return response.getData();
    }

    private BusinessException dependencyFailure(String message, Throwable cause) {
        return cause == null
                ? new BusinessException(message, ProductErrorCode.PRICING_DEPENDENCY_FAILED)
                : new BusinessException(message, ProductErrorCode.PRICING_DEPENDENCY_FAILED, cause);
    }

    private BusinessException invalidResponse(String message, Throwable cause) {
        return cause == null
                ? new BusinessException(message, ProductErrorCode.PRICING_DEPENDENCY_RESPONSE_INVALID)
                : new BusinessException(message, ProductErrorCode.PRICING_DEPENDENCY_RESPONSE_INVALID, cause);
    }
}
