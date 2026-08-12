package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 产品文档类型枚举（产品域专属）
 * <p>
 * 标识产品配置中「生成文档模板」的业务文档类型，供出单/保全/理赔各环节按类型取模板生成单证。
 * 属产品域专属业务维度（非技术文件格式，文件格式见 metadata {@code FileFormat}），故置于本域 common。
 * </p>
 */
@Getter
public enum DocumentType implements BaseEnum {
    /** 投保单 */
    APPLICATION_FORM(1, "APPLICATION_FORM", "投保单"),
    /** 保单 */
    POLICY(2, "POLICY", "保单"),
    /** 健康告知书 */
    HEALTH_NOTICE(3, "HEALTH_NOTICE", "健康告知书"),
    /** 理赔材料清单 */
    CLAIM_MATERIAL_LIST(4, "CLAIM_MATERIAL_LIST", "理赔材料清单"),
    /** 续保通知书 */
    RENEWAL_NOTICE(5, "RENEWAL_NOTICE", "续保通知书"),
    /** 保费收据 */
    PREMIUM_RECEIPT(6, "PREMIUM_RECEIPT", "保费收据");

    private final Integer enumCode;
    private final String  code;
    private final String  name;

    DocumentType(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    /**
     * 根据 code 反查枚举（统一范式入口，委托 {@link BaseEnum}）。
     *
     * @param code 文档类型编码
     * @return 匹配的枚举，未匹配返回 null
     */
    public static DocumentType fromCode(String code) {
        return BaseEnum.fromCode(DocumentType.class, code);
    }
}
