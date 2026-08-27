package com.titanium.product.web.dto.pricing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 定价包引用的 Channel 固定版本佣金方案。
 */
public record CommissionSchemeRefDTO(
        @NotBlank String channelId,
        @NotBlank String schemeCode,
        @NotBlank String schemeVersion,
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String contentHash) {
}
