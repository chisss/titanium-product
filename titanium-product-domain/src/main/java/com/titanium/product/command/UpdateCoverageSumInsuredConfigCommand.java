package com.titanium.product.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.product.valueobject.config.CoverageSumInsuredConfig;

/**
 * 更新责任级保额档次配置命令
 * <p>
 * <b>整体替换</b>语义：命令携带的清单即替换后的全部配置，不是增量。与条款绑定
 * （{@code UpdateProductClauseRelCommand}）同构，仅允许在产品 {@code DRAFT} 状态时使用。
 * </p>
 * <p>
 * 🔴 命令携带的是<b>已构造完成</b>的 {@link CoverageSumInsuredConfig}：七条不变量在值对象紧凑构造器
 * 中已裁决，本命令与聚合根不再重复校验——反序列化入站的命令若绕过值对象构造（如直接构造 record 分量），
 * 其校验发生在 web/api 层的协议转换（{@code ProductWebMapper} → 值对象构造）阶段。
 * </p>
 *
 * @param productId                 产品ID（聚合标识）
 * @param coverageSumInsuredConfigs 替换后的全部责任保额档次配置（整体替换语义；责任编码须唯一）
 * @param tenantId                  租户ID
 */
public record UpdateCoverageSumInsuredConfigCommand(@TargetAggregateIdentifier String productId,
                                                    List<CoverageSumInsuredConfig> coverageSumInsuredConfigs,
                                                    String tenantId) {
}
