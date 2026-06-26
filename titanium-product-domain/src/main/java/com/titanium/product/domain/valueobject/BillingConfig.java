package com.titanium.product.domain.valueobject;

import java.io.Serializable;
import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 缴费配置值对象
 * 定义产品允许的缴费方式、宽限期、失效规则
 *
 * @param allowedPaymentModes 允许的缴费方式: LUMP_SUM/ANNUAL/SEMI_ANNUAL/QUARTERLY/MONTHLY
 * @param gracePeriodDays     宽限期天数
 * @param lapseAfterDays      宽限期后多少天失效
 * @param autoDeductEnabled   是否支持自动扣款
 */
public record BillingConfig(
        List<ProductEnum.PaymentFrequency> allowedPaymentModes,
        int gracePeriodDays,
        int lapseAfterDays,
        boolean autoDeductEnabled
) implements Serializable {
}
