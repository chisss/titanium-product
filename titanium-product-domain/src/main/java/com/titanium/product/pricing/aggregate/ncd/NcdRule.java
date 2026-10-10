package com.titanium.product.pricing.aggregate.ncd;

import java.math.BigDecimal;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;

/**
 * NCD（无赔款优待）规则档位
 * <p>
 * 规则集的**子事实**：一条档位声明「统计区间内赔案次数落在 [minCount, maxCount] 且车型相符时，
 * 适用系数 coefficient」。次数分档、区间长度（规则集 {@code lookbackMonths}）与**车型是否分档**
 * 均属业务口径（R-3 未确认），本值对象只承载结构、不预设任何具体档位值。
 * </p>
 * <p>
 * 🔴 区间为**闭区间** {@code [minCount, maxCount]}（{@code maxCount} 为 {@code null} 表示无上界，
 * 即「{@code minCount} 次及以上」档）。这与出险日期区间的半开口径不同，两者不可互相套用：
 * 次数是离散计数，用半开会让「恰好等于上界」的次数落空。
 * </p>
 * <p>
 * 🔴 {@code vehicleType} 为 {@code null} 表示**通用档**（适用全部车型）；非 null 时为车型专属档。
 * 车型取值域（车系/车种/使用性质…）待 R-3 确认，本值对象只要求「同规则集内口径一致」，
 * 不定义枚举——按车型分档与否本身就是待确认项，此刻写死枚举等于替业务做了选型。
 * </p>
 *
 * @param minCount    赔案次数下界（含），不小于 0
 * @param maxCount    赔案次数上界（含）；{@code null} 表示无上界（该次数及以上）
 * @param vehicleType 车型标识；{@code null} 表示通用档（适用全部车型）
 * @param coefficient NCD 系数（须为正数；是否允许大于 1 的加费档由业务规则决定，本值对象不设上界）
 */
public record NcdRule(Integer minCount, Integer maxCount, String vehicleType, BigDecimal coefficient) {

    public NcdRule {
        if (minCount == null || minCount < 0) {
            throw invalid("档位下界（赔案次数）不能为空且不能为负数");
        }
        if (maxCount != null && maxCount < minCount) {
            throw invalid("档位上界不能小于下界");
        }
        if (coefficient == null || coefficient.signum() <= 0) {
            throw invalid("NCD 系数不能为空且必须为正数");
        }
        vehicleType = vehicleType == null || vehicleType.isBlank() ? null : vehicleType.trim();
    }

    /**
     * 该档位是否适用于给定车型。
     * <p>
     * 通用档（{@code vehicleType == null}）适用任何车型；车型专属档只在车型标识完全相等时适用
     * （不做大小写折叠——车型标识的规范形态待 R-3 确认，此刻替业务做归一化同样是替他做选型）。
     * </p>
     */
    public boolean appliesTo(String targetVehicleType) {
        return vehicleType == null || vehicleType.equals(targetVehicleType);
    }

    /** 赔案次数是否落在本档位区间（闭区间，上界为 null 时无上界）。 */
    public boolean covers(int claimCount) {
        return claimCount >= minCount && (maxCount == null || claimCount <= maxCount);
    }

    /** 是否为车型专属档（非通用档）。 */
    public boolean vehicleSpecific() {
        return vehicleType != null;
    }

    /**
     * 档位键的规范化文本（{@code 下界|上界|车型}，无量值，{@code null} 以 {@code *} 占位）。
     * <p>
     * 用途有二：① 落库冗余列 + 唯一约束的载体——MySQL 唯一键对 NULL 不去重，故不能直接以
     * 可空的 {@code max_count}/{@code vehicle_type} 承担「同档位不得重复」的约束；② 内容指纹的
     * 组成片段（见 {@code NcdRuleSet#canonicalContent()}）。
     * </p>
     */
    public String bandKey() {
        return minCount + "|" + (maxCount == null ? "*" : maxCount) + "|" + (vehicleType == null ? "*" : vehicleType);
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.NCD_RULE_INVALID, detail);
    }
}
