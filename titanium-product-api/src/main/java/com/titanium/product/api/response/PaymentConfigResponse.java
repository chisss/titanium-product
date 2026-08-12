package com.titanium.product.api.response;

import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 缴费方式配置响应（api 契约，镜像领域值对象 PaymentConfig）。
 *
 * @param allowedFrequencies 允许的缴费频率列表
 * @param allowedPaymentTerms 允许的缴费期限选项（如[5,10,15,20,30]年）
 * @param paymentTermUnit 缴费期限单位
 * @param autoRenewalSupported 是否支持自动续保
 * @param gracePeriodDays 宽限期天数
 */
public record PaymentConfigResponse(List<ProductEnum.PaymentFrequency> allowedFrequencies,
                                    List<Integer> allowedPaymentTerms, ProductEnum.PeriodUnit paymentTermUnit,
                                    boolean autoRenewalSupported, Integer gracePeriodDays) {
}
