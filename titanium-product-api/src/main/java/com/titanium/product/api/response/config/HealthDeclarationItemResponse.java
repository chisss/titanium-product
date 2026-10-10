package com.titanium.product.api.response.config;

/**
 * 告知项响应（api 契约，镜像领域值对象 HealthDeclarationItem，G12/g12-02 AC-02）。
 * <p>
 * 🔴 <b>跨域传输形态：编码一律用 String（枚举常量名）</b>——下游（policy 出单校验）以
 * {@code fromCode} 承接，未知编码属滚动升级窗口的扩展项，由下游跳过留痕而非整链失败
 * （m8-1103 判据变体：集合扩展项与单值必达的容错口径不同）。故本契约<b>不</b>用枚举类型承接，
 * 避免下游旧版本反序列化即失败。
 * </p>
 *
 * @param question 问题编码（权威值域枚举常量名，如 {@code HOSPITALIZATION_TWO_YEARS}）
 * @param text 展示文案（可空）
 * @param required 是否必答
 * @param handling 命中处置（枚举常量名，如 {@code REJECT}）
 */
public record HealthDeclarationItemResponse(String question, String text, boolean required, String handling) {
}
