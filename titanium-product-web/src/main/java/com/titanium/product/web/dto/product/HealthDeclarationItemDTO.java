package com.titanium.product.web.dto.product;

import com.titanium.metadata.enums.underwriting.HealthDeclarationHandling;
import com.titanium.metadata.enums.underwriting.HealthDeclarationQuestion;

import lombok.Data;

/**
 * 告知项请求（后台/端上 HTTP 入参，G12/g12-02 AC-02）
 * <p>
 * 产品投保条件中单个自定义告知项的前端提交形态，由 {@code ProductWebMapper} 翻译为领域值对象
 * {@code HealthDeclarationItem}。
 * </p>
 * <p>
 * 🔴 <b>写入路径 fail-closed</b>：{@code question}/{@code handling} 以权威枚举为类型，
 * 前端提交未知编码在反序列化即被拒绝（HTTP 400），<b>不得</b>以 String 承接后静默丢弃。
 * 字段名必须全部存在于领域值对象 {@code HealthDeclarationItem}，由
 * {@code ProductNestedInputDTOContractTest} 反射断言固化。
 * </p>
 *
 * @see com.titanium.product.valueobject.config.HealthDeclarationItem
 */
@Data
public class HealthDeclarationItemDTO {

    /** 问题编码（权威值域枚举） */
    private HealthDeclarationQuestion question;
    /** 展示文案（可空，缺省由展示层回落枚举 desc） */
    private String text;
    /** 是否必答 */
    private boolean required;
    /** 命中处置 */
    private HealthDeclarationHandling handling;
}
