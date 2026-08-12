package com.titanium.product.api.response;

import java.util.List;

/**
 * 产品文档配置响应（api 契约，镜像领域值对象 DocumentConfig）。
 * <p>下游/前端据此了解产品的所需投保材料清单与生成文档模板清单，替代原缺失的文档配置读出口。</p>
 *
 * @param requiredMaterials 所需投保材料清单
 * @param documentTemplates 生成文档模板清单
 */
public record DocumentConfigResponse(List<RequiredMaterialResponse> requiredMaterials,
                                     List<DocumentTemplateResponse> documentTemplates) {
}
