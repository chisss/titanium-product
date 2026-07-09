package com.titanium.product.valueobject;

import java.io.Serializable;
import java.util.List;

/**
 * 理赔配置值对象 定义产品的理赔流程阶段、等待期、报案时效等
 *
 * @param claimStages 理赔阶段列表（有序）
 * @param reportDeadlineDays 报案时效（天）
 * @param waitingPeriodDays 等待期天数（0=无）
 * @param claimRuleSet 理赔审核规则集编码
 * @param requiredDocuments 理赔所需材料列表
 */
public record ClaimConfig(List<String> claimStages, int reportDeadlineDays, int waitingPeriodDays, String claimRuleSet,
                          List<String> requiredDocuments)
        implements
            Serializable {
}
