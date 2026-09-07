package com.titanium.product.valueobject.config;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 核保配置值对象 定义该产品的核保策略。不同险种的核保要求差异巨大： - 交强险/短期意外险：自动核保通过 -
 * 标准寿险（保额<50万）：智核（规则引擎自动核保） - 高保额寿险（保额≥50万）：人工核保 - 团险：自动核保 + 人工复核
 * Underwriting域根据此配置决定核保模式和规则集。
 *
 * @param underwritingMode 核保模式
 * @param autoApprovalCondition 自动核保通过条件描述（未来接入规则引擎后改为规则集ID）
 * @param manualReviewAmountThreshold 转人工核保的保额阈值
 * @param requiredDocuments 核保必需材料清单
 * @param underwritingSLADays 核保时效要求（天）
 * @param surchargeAcceptable 是否支持加费承保
 * @param specialAgreementAcceptable 是否支持特别约定
 * @param ruleSetCode 关联的规则引擎规则集编码（dev-505：产品核保配置→规则集→核保执行的链路入口；
 *                    为空表示未接入规则引擎，核保域回退内置评分逻辑）。旧版本事件/配置 JSON 无此字段，
 *                    Jackson 反序列化时取 null，向后兼容。
 */
public record UnderwritingConfig(ProductEnum.UnderwritingMode underwritingMode, String autoApprovalCondition,
                                 BigDecimal manualReviewAmountThreshold, List<String> requiredDocuments,
                                 Integer underwritingSLADays, boolean surchargeAcceptable,
                                 boolean specialAgreementAcceptable, String ruleSetCode) {
}
