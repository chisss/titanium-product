package com.titanium.product.event;

import java.util.List;

import com.titanium.product.valueobject.config.CoverageSumInsuredConfig;

/**
 * 责任级保额档次配置更新事件
 * <p>
 * 产品的责任保额档次被整体替换时发布（仅草稿态）。
 * </p>
 * <p>
 * 🔴 <b>刻意携带 {@code tenantId}，与同族 {@code ProductClauseRelUpdatedEvent} 不同</b>：
 * 读模型 {@code BaseView.tenant_id} 非空，投影时若无租户只能「找不到原行就跳过」——
 * 而本能力上线时存量产品（含全部在跑的 DRAFT 产品）<b>都没有对应的读模型行</b>，
 * 一旦沿用「跳过」，给它们配置保额档次会<b>静默投影失败</b>（写侧成功、读侧永远查不到）。
 * 携带租户后投影可安全 upsert，不依赖产品是否在本能力上线后重建过。
 * </p>
 *
 * @param productId                 产品ID
 * @param coverageSumInsuredConfigs 替换后的全部责任保额档次配置
 * @param tenantId                  租户ID（投影 upsert 时写入 {@code tenant_id}，非空约束）
 */
public record CoverageSumInsuredConfigUpdatedEvent(String productId,
                                                   List<CoverageSumInsuredConfig> coverageSumInsuredConfigs,
                                                   String tenantId) {
}
