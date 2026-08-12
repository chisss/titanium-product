package com.titanium.product.api.response;

import com.titanium.metadata.enums.FileFormat;
import com.titanium.product.common.enums.DocumentType;

/**
 * 生成文档模板响应（api 契约，镜像领域值对象 DocumentConfig.DocumentTemplate）。
 * <p>下游/前端据此了解产品在各环节自动生成的单证模板及输出格式。</p>
 *
 * @param documentType 文档类型（投保单/保单/健康告知书/理赔材料清单等）
 * @param templateCode 模板编码
 * @param templateName 模板名称
 * @param outputFormat 输出文件格式
 * @param autoGenerate 是否自动生成
 * @param description 模板说明
 */
public record DocumentTemplateResponse(DocumentType documentType, String templateCode, String templateName,
                                       FileFormat outputFormat, boolean autoGenerate, String description) {
}
