package com.titanium.product.valueobject.config;

import java.io.Serializable;
import java.util.List;

import com.titanium.metadata.enums.FileFormat;
import com.titanium.product.common.enums.DocumentType;

/**
 * 产品文档配置值对象
 * <p>
 * 定义产品在投保/出单/理赔各环节的单证配置，是纯产品配置（不与 document 文档域交互、不发起跨域调用）：
 * <ul>
 *   <li>{@code requiredMaterials}——所需投保材料清单：投保时被保人/投保人须提交的材料（如身份证、体检报告）；</li>
 *   <li>{@code documentTemplates}——生成文档模板清单：产品需生成的单证模板（投保单/保单/健康告知书/理赔材料清单等）。</li>
 * </ul>
 * 作为 {@code InsuranceProduct} 的配置字段，随创建命令写入，供出单/理赔时按类型取模板与校验材料齐全性。
 * </p>
 *
 * @param requiredMaterials 所需投保材料清单
 * @param documentTemplates 生成文档模板清单
 */
public record DocumentConfig(List<RequiredMaterial> requiredMaterials, List<DocumentTemplate> documentTemplates)
        implements
            Serializable {

    public DocumentConfig {
        requiredMaterials = requiredMaterials == null ? List.of() : List.copyOf(requiredMaterials);
        documentTemplates = documentTemplates == null ? List.of() : List.copyOf(documentTemplates);
    }

    /**
     * 所需投保材料。
     *
     * @param materialCode 材料编码（产品内唯一标识）
     * @param materialName 材料名称（如"被保人身份证""近一年体检报告"）
     * @param mandatory 是否必需（false 表示选交）
     * @param acceptedFormats 接受的文件格式（如 PDF/JPG，为空表示不限）
     * @param description 材料说明/提交要求
     */
    public record RequiredMaterial(String materialCode, String materialName, boolean mandatory,
                                   List<FileFormat> acceptedFormats, String description) implements Serializable {
        public RequiredMaterial {
            acceptedFormats = acceptedFormats == null ? List.of() : List.copyOf(acceptedFormats);
        }
    }

    /**
     * 生成文档模板。
     *
     * @param documentType 文档类型（投保单/保单/健康告知书/理赔材料清单等）
     * @param templateCode 模板编码（指向文档模板，仅产品配置引用，不解析模板内容）
     * @param templateName 模板名称
     * @param outputFormat 输出文件格式（默认 PDF）
     * @param autoGenerate 是否在对应环节自动生成
     * @param description 模板说明
     */
    public record DocumentTemplate(DocumentType documentType, String templateCode, String templateName,
                                   FileFormat outputFormat, boolean autoGenerate, String description)
            implements
                Serializable {
    }
}
