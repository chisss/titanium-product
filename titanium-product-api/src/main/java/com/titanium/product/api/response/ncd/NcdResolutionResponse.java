package com.titanium.product.api.response.ncd;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * NCD（无赔款优待）反算结果（含取证字段，G11/g11-03 AC-04）
 * <p>
 * 🔴 取证字段是契约的一部分：{@code claimCount} / {@code periodFrom} / {@code periodTo} /
 * {@code ruleSetCode} / {@code ruleSetVersion} / {@code ruleSetContentHash} 回答「这个系数依据什么算出来」，
 * 供出单随单落库与事后复核。
 * </p>
 * <p>
 * 🔴 本响应**只由一次完整反算产生**：规则集取不到、历史赔案取不到、档位未命中或冲突时，
 * 响应为失败信封（业务码非成功码），**不存在携带默认系数 1.0 的成功响应**。
 * </p>
 *
 * @param productId           产品ID
 * @param coefficient         反算得到的 NCD 系数（取自命中档位，非默认值）
 * @param claimCount          统计区间内计入的历史赔案条数（取证：条数）
 * @param periodFrom          统计区间起点（含，取证：区间）
 * @param periodTo            统计区间止点（不含，取证：区间）
 * @param ruleSetCode         所用规则集编码（取证：规则版本）
 * @param ruleSetVersion      所用规则集版本（取证：规则版本）
 * @param ruleSetContentHash  所用规则集内容指纹（取证：内容未变则版本可比）
 */
public record NcdResolutionResponse(
        String productId,
        BigDecimal coefficient,
        Integer claimCount,
        LocalDateTime periodFrom,
        LocalDateTime periodTo,
        String ruleSetCode,
        String ruleSetVersion,
        String ruleSetContentHash) {
}
