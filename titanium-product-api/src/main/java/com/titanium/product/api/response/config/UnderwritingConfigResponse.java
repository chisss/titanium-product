package com.titanium.product.api.response.config;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 核保配置响应（api 契约，镜像领域值对象 UnderwritingConfig）。
 * <p>Underwriting 域据此决定核保模式与规则集。</p>
 *
 * @param underwritingMode 核保模式
 * @param autoApprovalCondition 自动核保通过条件描述
 * @param manualReviewAmountThreshold 转人工核保的保额阈值
 * @param requiredDocuments 核保必需材料清单
 * @param underwritingSLADays 核保时效要求（天）
 * @param surchargeAcceptable 是否支持加费承保
 * @param specialAgreementAcceptable 是否支持特别约定
 */
public record UnderwritingConfigResponse(ProductEnum.UnderwritingMode underwritingMode, String autoApprovalCondition,
                                         BigDecimal manualReviewAmountThreshold, List<String> requiredDocuments,
                                         Integer underwritingSLADays, boolean surchargeAcceptable,
                                         boolean specialAgreementAcceptable) {
}
