package com.titanium.product.infrastructure.pricing.adapter.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.titanium.common.exception.BusinessException;
import com.titanium.featurecenter.api.FeatureCenterApi;
import com.titanium.featurecenter.api.request.FeatureResolveRequest;
import com.titanium.featurecenter.api.response.FeatureResolveResponse;
import com.titanium.featurecenter.api.response.TypedFeatureValueResponse;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.common.enums.PricingFeatureDataType;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureRequirement;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureResolutionRequest;

@ExtendWith(MockitoExtension.class)
class FeatureCenterResolutionAdapterTest {

    private static final String TENANT_ID = "tenant-a";

    @Mock
    private FeatureCenterApi featureCenterApi;

    @InjectMocks
    private FeatureCenterResolutionAdapter adapter;

    @Test
    void shouldMapTypedResponseAndForwardContractAndTenant() {
        PricingFeatureResolutionRequest request = request();
        FeatureResolveResponse response = FeatureResolveResponse.builder()
                .snapshotId("snapshot-1")
                .values(List.of(TypedFeatureValueResponse.builder()
                        .featureCode("insured.age")
                        .dataType("INTEGER")
                        .status("RESOLVED")
                        .sourceType("REQUEST")
                        .definitionVersion("age:3")
                        .integerValue(35L)
                        .qualityFlags(List.of("verified"))
                        .build()))
                .definitionVersions(Map.of("insured.age", "age:3"))
                .missingRequired(List.of())
                .lineageDigest("digest-1")
                .build();
        when(featureCenterApi.resolve(any(), eq(TENANT_ID))).thenReturn(ApiResponse.success(response));

        var result = adapter.resolve(request);

        assertThat(result.snapshotId()).isEqualTo("snapshot-1");
        assertThat(result.values()).singleElement().satisfies(value -> {
            assertThat(value.dataType()).isEqualTo(PricingFeatureDataType.INTEGER);
            assertThat(value.integerValue()).isEqualTo(35L);
        });
        ArgumentCaptor<FeatureResolveRequest> requestCaptor = ArgumentCaptor.forClass(FeatureResolveRequest.class);
        verify(featureCenterApi).resolve(requestCaptor.capture(), eq(TENANT_ID));
        assertThat(requestCaptor.getValue().getContractId()).isEqualTo("pricing-contract");
        assertThat(requestCaptor.getValue().getContractVersion()).isEqualTo("2026-08");
        assertThat(requestCaptor.getValue().getRequirements()).hasSize(1);
    }

    @Test
    void shouldFailClosedWhenDownstreamReturnsBusinessFailure() {
        when(featureCenterApi.resolve(any(), eq(TENANT_ID)))
                .thenReturn(new ApiResponse<>("61000001", "feature failed", null));

        assertThatThrownBy(() -> adapter.resolve(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ProductErrorCode.PRICING_DEPENDENCY_FAILED.getCode());
    }

    @Test
    void shouldFailClosedWhenResponseDataIsEmpty() {
        when(featureCenterApi.resolve(any(), eq(TENANT_ID))).thenReturn(ApiResponse.success(null));

        assertThatThrownBy(() -> adapter.resolve(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ProductErrorCode.PRICING_DEPENDENCY_RESPONSE_INVALID.getCode());
    }

    @Test
    void shouldRejectResolvedValueWithTwoTypedFields() {
        FeatureResolveResponse response = FeatureResolveResponse.builder()
                .snapshotId("snapshot-1")
                .values(List.of(TypedFeatureValueResponse.builder()
                        .featureCode("insured.age")
                        .dataType("INTEGER")
                        .status("RESOLVED")
                        .integerValue(35L)
                        .stringValue("35")
                        .build()))
                .definitionVersions(Map.of("insured.age", "age:3"))
                .missingRequired(List.of())
                .lineageDigest("digest-1")
                .build();
        when(featureCenterApi.resolve(any(), eq(TENANT_ID))).thenReturn(ApiResponse.success(response));

        assertThatThrownBy(() -> adapter.resolve(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ProductErrorCode.PRICING_DEPENDENCY_RESPONSE_INVALID.getCode());
    }

    private PricingFeatureResolutionRequest request() {
        return new PricingFeatureResolutionRequest(
                TENANT_ID,
                "request-1",
                "pricing-contract",
                "2026-08",
                LocalDateTime.of(2026, 8, 18, 10, 0),
                List.of(new PricingFeatureRequirement(
                        "insured.age", PricingFeatureDataType.INTEGER, true, "age:3", "BLOCK", "NORMAL")),
                Map.of("insuredId", "insured-1"),
                Map.of());
    }
}
