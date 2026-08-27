package com.titanium.product.infrastructure.pricing.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.valueobject.pricing.PricingRuleComputationRequest;
import com.titanium.ruleengine.api.RuleArtifactApi;
import com.titanium.ruleengine.api.request.RuleArtifactComputeRequest;
import com.titanium.ruleengine.api.response.RuleArtifactComputeResponse;

@ExtendWith(MockitoExtension.class)
class RuleEngineComputationAdapterTest {

    private static final String TENANT_ID = "tenant-a";

    @Mock
    private RuleArtifactApi ruleArtifactApi;

    @InjectMocks
    private RuleEngineComputationAdapter adapter;

    @Test
    void shouldMapResultAndForwardFixedArtifactVersionAndTenant() {
        PricingRuleComputationRequest request = request();
        RuleArtifactComputeResponse response = RuleArtifactComputeResponse.builder()
                .executionId("execution-1")
                .artifactCode("premium-calc")
                .artifactVersion("7")
                .inputSchemaVersion("pricing-input:1")
                .computedValue(new BigDecimal("123.45"))
                .lineItems(Map.of("basePremium", new BigDecimal("100.00")))
                .matchedSteps(List.of("base-rate", "discount"))
                .artifactHash("hash-7")
                .durationMs(12)
                .build();
        when(ruleArtifactApi.compute(eq("premium-calc"), eq("7"), any(), eq(TENANT_ID)))
                .thenReturn(ApiResponse.success(response));

        var result = adapter.compute(request);

        assertThat(result.computedValue()).isEqualByComparingTo("123.45");
        assertThat(result.artifactHash()).isEqualTo("hash-7");
        ArgumentCaptor<RuleArtifactComputeRequest> requestCaptor = ArgumentCaptor.forClass(RuleArtifactComputeRequest.class);
        verify(ruleArtifactApi).compute(eq("premium-calc"), eq("7"), requestCaptor.capture(), eq(TENANT_ID));
        assertThat(requestCaptor.getValue().getInputSchemaVersion()).isEqualTo("pricing-input:1");
        assertThat(requestCaptor.getValue().getExecutionId()).isEqualTo("execution-1");
    }

    @Test
    void shouldFailClosedWhenDownstreamReturnsBusinessFailure() {
        when(ruleArtifactApi.compute(eq("premium-calc"), eq("7"), any(), eq(TENANT_ID)))
                .thenReturn(new ApiResponse<>("62000001", "rule failed", null));

        assertThatThrownBy(() -> adapter.compute(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ProductErrorCode.PRICING_DEPENDENCY_FAILED.getCode());
    }

    @Test
    void shouldFailClosedWhenResponseDataIsEmpty() {
        when(ruleArtifactApi.compute(eq("premium-calc"), eq("7"), any(), eq(TENANT_ID)))
                .thenReturn(ApiResponse.success(null));

        assertThatThrownBy(() -> adapter.compute(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ProductErrorCode.PRICING_DEPENDENCY_RESPONSE_INVALID.getCode());
    }

    @Test
    void shouldFailClosedWhenArtifactVersionDoesNotMatchRequest() {
        RuleArtifactComputeResponse response = RuleArtifactComputeResponse.builder()
                .executionId("execution-1")
                .artifactCode("premium-calc")
                .artifactVersion("8")
                .inputSchemaVersion("pricing-input:1")
                .computedValue(BigDecimal.ONE)
                .artifactHash("hash-8")
                .build();
        when(ruleArtifactApi.compute(eq("premium-calc"), eq("7"), any(), eq(TENANT_ID)))
                .thenReturn(ApiResponse.success(response));

        assertThatThrownBy(() -> adapter.compute(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ProductErrorCode.PRICING_DEPENDENCY_RESPONSE_INVALID.getCode());
    }

    private PricingRuleComputationRequest request() {
        return new PricingRuleComputationRequest(
                TENANT_ID,
                "execution-1",
                "premium-calc",
                "7",
                "pricing-input:1",
                Map.of("age", 35, "sumInsured", new BigDecimal("100000")),
                true,
                LocalDateTime.of(2026, 8, 18, 10, 0));
    }
}
