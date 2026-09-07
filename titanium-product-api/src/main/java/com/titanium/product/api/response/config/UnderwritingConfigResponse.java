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
 * @param ruleSetCode 关联的规则引擎规则集编码（dev-505）；为空表示未接入规则引擎，核保域回退内置评分逻辑。
 *                    旧版本响应 JSON 无此字段，Jackson 反序列化时取 null，向后兼容。
 */
public record UnderwritingConfigResponse(ProductEnum.UnderwritingMode underwritingMode, String autoApprovalCondition,
                                         BigDecimal manualReviewAmountThreshold, List<String> requiredDocuments,
                                         Integer underwritingSLADays, boolean surchargeAcceptable,
                                         boolean specialAgreementAcceptable, String ruleSetCode) {
}
