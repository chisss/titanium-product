package com.titanium.product.domain.valueobject;

import java.io.Serializable;
import java.util.List;

/**
 * 保全配置值对象
 * 定义产品允许的保全操作类型及犹豫期、退保规则
 *
 * @param allowedTypes        允许的保全类型列表
 * @param freeLookPeriodDays  犹豫期天数（0=无）
 * @param surrenderRuleSet    退保规则集编码
 * @param endorsementRuleSet  批改规则集编码
 */
public record MaintenanceConfig(
        List<String> allowedTypes,
        int freeLookPeriodDays,
        String surrenderRuleSet,
        String endorsementRuleSet
) implements Serializable {
}
