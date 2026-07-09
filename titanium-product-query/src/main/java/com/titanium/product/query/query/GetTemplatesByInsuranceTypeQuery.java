package com.titanium.product.query.query;

import com.titanium.metadata.enums.InsuranceType;

/**
 * 根据险种类型查询产品模板列表
 */
public record GetTemplatesByInsuranceTypeQuery(
        InsuranceType insuranceType,
        String tenantId
) {
}
