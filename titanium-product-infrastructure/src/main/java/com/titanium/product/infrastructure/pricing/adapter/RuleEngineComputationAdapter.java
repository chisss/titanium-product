package com.titanium.product.infrastructure.pricing.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.port.RuleComputationPort;
import com.titanium.product.valueobject.pricing.PricingRuleComputationRequest;
import com.titanium.product.valueobject.pricing.PricingRuleComputationResult;
import com.titanium.ruleengine.api.RuleArtifactApi;
import com.titanium.ruleengine.api.request.RuleArtifactComputeRequest;
import com.titanium.ruleengine.api.response.RuleArtifactComputeResponse;

import lombok.RequiredArgsConstructor;

/**
 * Rule Engine 固定版本工件适配器。
 */
@Component
@RequiredArgsConstructor
public class RuleEngineComputationAdapter implements RuleComputationPort {

    private final RuleArtifactApi ruleArtifactApi;

    @Override
    public PricingRuleComputationResult compute(PricingRuleComputationRequest request) {
        RuleArtifactComputeResponse response = invoke(request);
        try {
            validateIdentity(request, response);
            return new PricingRuleComputationResult(
                    response.getExecutionId(), response.getArtifactCode(), response.getArtifactVersion(),
                    response.getInputSchemaVersion(), response.getComputedValue(), response.getLineItems(),
                    response.getMatchedSteps() == null ? List.of() : response.getMatchedSteps(),
                    response.getArtifactHash(), response.getDurationMs());
        } catch (RuntimeException exception) {
            throw invalidResponse("Rule Engine返回数据无效", exception);
        }
    }

    private void validateIdentity(
            PricingRuleComputationRequest request, RuleArtifactComputeResponse response) {
        if (!request.executionId().equals(response.getExecutionId())
                || !request.artifactCode().equals(response.getArtifactCode())
                || !request.artifactVersion().equals(response.getArtifactVersion())
                || !request.inputSchemaVersion().equals(response.getInputSchemaVersion())) {
            throw new IllegalArgumentException("Rule Engine响应标识与请求不一致");
        }
    }

    private RuleArtifactComputeResponse invoke(PricingRuleComputationRequest request) {
        try {
            ApiResponse<RuleArtifactComputeResponse> response = ruleArtifactApi.compute(
                    request.artifactCode(), request.artifactVersion(), toRequest(request), request.tenantId());
            if (response == null) {
                throw invalidResponse("Rule Engine响应为空", null);
            }
            if (!response.isSuccess()) {
                throw dependencyFailure("Rule Engine返回业务失败: " + response.getCode() + ", "
                        + response.getMessage(), null);
            }
            if (response.getData() == null) {
                throw invalidResponse("Rule Engine成功响应缺少data", null);
            }
            return response.getData();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw dependencyFailure("Rule Engine调用异常", exception);
        }
    }

    private RuleArtifactComputeRequest toRequest(PricingRuleComputationRequest request) {
        RuleArtifactComputeRequest target = new RuleArtifactComputeRequest();
        target.setExecutionId(request.executionId());
        target.setInputSchemaVersion(request.inputSchemaVersion());
        target.setVariables(request.variables());
        target.setExplain(request.explain());
        target.setBusinessTime(request.businessTime());
        return target;
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
