package com.titanium.product.api.request.ncd;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * NCD（无赔款优待）反算请求（G11/g11-03）
 *
 * @param productId    产品ID
 * @param vin          车辆识别代号（与 subjectId 至少提供一个，VIN 优先——跨保单的车辆身份，
 *                     续保换单后 policyId/subjectId 均变、唯 VIN 不变）
 * @param subjectId    标的标识（保单内唯一，VIN 缺失时的兜底检索键）
 * @param vehicleType  车型标识；{@code null} 表示不按车型取档（此时只有通用档可命中）
 * @param businessTime 业务时点（决定取哪一版规则集，以及统计区间的截止日）
 */
public record NcdResolutionRequest(
        @NotBlank String productId,
        String vin,
        String subjectId,
        String vehicleType,
        @NotNull LocalDateTime businessTime) {

    /**
     * 兼容构造器：不含车型（按通用档反算）。
     * <p>
     * 🔴 车型缺省**不等于**命中通用档：若规则集只有车型专属档，反算将以
     * {@code NCD_BAND_NOT_MATCHED} 显式失败，不会退而取任一档位。
     * </p>
     */
    public NcdResolutionRequest(String productId, String vin, String subjectId, LocalDateTime businessTime) {
        this(productId, vin, subjectId, null, businessTime);
    }
}
