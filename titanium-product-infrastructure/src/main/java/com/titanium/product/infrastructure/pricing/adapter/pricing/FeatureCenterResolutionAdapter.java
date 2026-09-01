package com.titanium.product.infrastructure.pricing.adapter.pricing;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.titanium.common.exception.BusinessException;
import com.titanium.featurecenter.api.FeatureCenterApi;
import com.titanium.featurecenter.api.request.FeatureRequirementRequest;
import com.titanium.featurecenter.api.request.FeatureResolveRequest;
import com.titanium.featurecenter.api.response.FeatureResolveResponse;
import com.titanium.featurecenter.api.response.TypedFeatureValueResponse;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.common.enums.PricingFeatureDataType;
import com.titanium.product.port.pricing.FeatureResolutionPort;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureRequirement;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureResolution;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureResolutionRequest;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureValue;

import lombok.RequiredArgsConstructor;

/**
 * Feature Center 适配器。
 *
 * <p>远程 DTO 和 Feign 异常均在此处隔离，Product 领域不感知外部服务实现。</p>
 */
@Component
@RequiredArgsConstructor
public class FeatureCenterResolutionAdapter implements FeatureResolutionPort {

    private final FeatureCenterApi featureCenterApi;

    @Override
    public PricingFeatureResolution resolve(PricingFeatureResolutionRequest request) {
        FeatureResolveResponse response = invoke(request);
        return mapResponse(response);
    }

    private FeatureResolveResponse invoke(PricingFeatureResolutionRequest request) {
        try {
            ApiResponse<FeatureResolveResponse> response = featureCenterApi.resolve(toRequest(request), request.tenantId());
            if (response == null) {
                throw invalidResponse("Feature Center响应为空", null);
            }
            if (!response.isSuccess()) {
                throw dependencyFailure("Feature Center返回业务失败: " + response.getCode() + ", "
                        + response.getMessage(), null);
            }
            if (response.getData() == null) {
                throw invalidResponse("Feature Center成功响应缺少data", null);
            }
            return response.getData();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw dependencyFailure("Feature Center调用异常", exception);
        }
    }

    private FeatureResolveRequest toRequest(PricingFeatureResolutionRequest request) {
        FeatureResolveRequest target = new FeatureResolveRequest();
        target.setRequestId(request.requestId());
        target.setContractId(request.contractId());
        target.setContractVersion(request.contractVersion());
        target.setBusinessTime(request.businessTime());
        target.setRequirements(request.requirements().stream().map(this::toRequirement).toList());
        target.setRequestSnapshot(request.requestSnapshot());
        target.setSourceReferences(request.sourceReferences());
        return target;
    }

    private FeatureRequirementRequest toRequirement(PricingFeatureRequirement requirement) {
        FeatureRequirementRequest target = new FeatureRequirementRequest();
        target.setFeatureCode(requirement.featureCode());
        target.setDataType(requirement.dataType().name());
        target.setRequired(requirement.required());
        target.setDefinitionVersion(requirement.definitionVersion());
        target.setMissingPolicy(requirement.missingPolicy());
        target.setSensitivity(requirement.sensitivity());
        return target;
    }

    private PricingFeatureResolution mapResponse(FeatureResolveResponse response) {
        try {
            List<TypedFeatureValueResponse> values = response.getValues() == null ? List.of() : response.getValues();
            return new PricingFeatureResolution(
                    response.getSnapshotId(),
                    values.stream().map(this::toValue).toList(),
                    response.getDefinitionVersions(),
                    response.getMissingRequired(),
                    response.getLineageDigest());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidResponse("Feature Center返回数据无效", exception);
        }
    }

    private PricingFeatureValue toValue(TypedFeatureValueResponse source) {
        if (source == null) {
            throw new IllegalArgumentException("特征值为空");
        }
        PricingFeatureDataType dataType = parseDataType(source.getDataType());
        return new PricingFeatureValue(
                source.getFeatureCode(), dataType, source.getStatus(), source.getSourceType(),
                source.getDefinitionVersion(), source.getObservedAt(), source.getQualityFlags(),
                source.getStringValue(), source.getIntegerValue(), source.getDecimalValue(),
                source.getBooleanValue(), source.getDateValue(), source.getDateTimeValue(),
                source.getEnumValue(), source.getJsonValue());
    }

    private PricingFeatureDataType parseDataType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("特征数据类型为空");
        }
        return PricingFeatureDataType.valueOf(value.trim().toUpperCase(Locale.ROOT));
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
