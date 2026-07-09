package com.titanium.product.valueobject;

import java.io.Serializable;
import java.util.List;

/**
 * 出单阶段定义值对象 描述产品在出单过程中的每一个阶段及其要求
 *
 * @param stageCode 阶段编码: PROPOSAL / INSURANCE / POLICY
 * @param stageName 阶段名称
 * @param requiredComponents 必需的数据组件:
 *            ["APPLICANT","INSURED","SUBJECT","PRODUCT_LINE"]
 * @param validationRuleSet 校验规则集编码（接入规则引擎）
 * @param nextStageTransition 进入下一阶段的触发条件
 * @param autoTransition 是否自动流转
 */
public record PolicyStage(String stageCode, String stageName, List<String> requiredComponents, String validationRuleSet,
                          String nextStageTransition, boolean autoTransition)
        implements
            Serializable {
}
