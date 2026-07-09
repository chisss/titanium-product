package com.titanium.product.query.service;

import java.util.List;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.query.result.ProductTemplateQueryResult;

/**
 * 产品模板查询服务（CQRS 读侧）
 * <p>
 * 查询由事件投影维护的读模型表 {@code t_product_template_view}，实现读写分离。 所有查询强制携带
 * {@code tenantId} 保证多租户隔离。
 * </p>
 */
public interface ProductTemplateQueryService {

    /**
     * 根据模板ID + 租户ID查询产品模板
     */
    ProductTemplateQueryResult getTemplateById(String templateId, String tenantId);

    /**
     * 根据产品ID + 租户ID查询产品模板
     */
    ProductTemplateQueryResult getTemplateByProductId(String productId, String tenantId);

    /**
     * 根据模板编码 + 租户ID查询产品模板
     */
    ProductTemplateQueryResult getTemplateByCode(String templateCode, String tenantId);

    /**
     * 根据险种类型 + 租户ID查询产品模板列表
     */
    List<ProductTemplateQueryResult> getTemplatesByInsuranceType(InsuranceType insuranceType, String tenantId);
}
