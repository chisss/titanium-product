package com.titanium.product.valueobject;

import java.util.List;

/**
 * 费率表引用值对象（费率表查询模式专用）
 * <p>
 * 指向 clause 域的具体费率表，billing 出单算保费时经 {@code ClauseServicePort} 按此引用
 * 查询四维（年龄×性别×缴费期×保障期）费率。
 * </p>
 *
 * @param clauseId    所属条款ID（费率表挂在条款下）
 * @param tableCode   费率表编码（同一条款可能有多张费率表版本）
 * @param version     费率表版本（用于精确匹配，对应条款版本）
 * @param dimensionKeys 启用的查询维度（如 ["age","gender","paymentTerm","coverageTerm"]，
 *                    null 时默认全四维）
 */
public record RateTableRef(
        String       clauseId,
        String       tableCode,
        String       version,
        List<String> dimensionKeys
) {

    /** 默认维度：年龄、性别、缴费期、保障期 */
    public static final List<String> DEFAULT_DIMENSIONS = List.of("age", "gender", "paymentTerm", "coverageTerm");

    /** 构造标准四维费率表引用 */
    public static RateTableRef of(String clauseId, String tableCode, String version) {
        return new RateTableRef(clauseId, tableCode, version, DEFAULT_DIMENSIONS);
    }
}
