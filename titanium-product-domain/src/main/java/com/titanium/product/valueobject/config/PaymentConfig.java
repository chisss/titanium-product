package com.titanium.product.valueobject.config;

import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 缴费方式配置值对象 定义产品允许的缴费方式、缴费期限、宽限期等
 *
 * @param allowedFrequencies 允许的缴费频率列表
 * @param allowedPaymentTerms 允许的缴费期限选项（如[5,10,15,20,30]年）
 * @param paymentTermUnit 缴费期限单位
 * @param autoRenewalSupported 是否支持自动续保
 * @param gracePeriodDays 宽限期天数
 */
public record PaymentConfig(List<ProductEnum.PaymentFrequency> allowedFrequencies, List<Integer> allowedPaymentTerms,
                            ProductEnum.PeriodUnit paymentTermUnit, boolean autoRenewalSupported,
                            Integer gracePeriodDays) {
}
