package com.titanium.product.query.query;

import com.titanium.metadata.enums.insurance.InsuranceProductType;

/**
 * 根据险种类型查询产品模板列表
 * <p>
 * G10/g10-04：入参自遗留扁平码（InsuranceType）切到权威三级码（InsuranceProductType）——
 * 读侧切换由 g10-02 显式移交；迁移前历史行由服务实现按反向映射回落，见
 * {@code ProductTemplateQueryServiceImpl#getTemplatesByInsuranceType}。
 * </p>
 */
public record GetTemplatesByInsuranceTypeQuery(
        InsuranceProductType insuranceType,
        String tenantId
) {
}
