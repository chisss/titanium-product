package com.titanium.product.api.response.document;

import java.util.List;

import com.titanium.metadata.enums.FileFormat;

/**
 * 所需投保材料响应（api 契约，镜像领域值对象 DocumentConfig.RequiredMaterial）。
 * <p>下游/前端据此展示投保时须提交的材料清单及格式要求。</p>
 *
 * @param materialCode 材料编码（产品内唯一标识）
 * @param materialName 材料名称
 * @param mandatory 是否必需
 * @param acceptedFormats 接受的文件格式
 * @param description 材料说明/提交要求
 */
public record RequiredMaterialResponse(String materialCode, String materialName, boolean mandatory,
                                       List<FileFormat> acceptedFormats, String description) {
}
