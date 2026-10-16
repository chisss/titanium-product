package com.titanium.product.valueobject.config;

import java.time.LocalDateTime;

/**
 * 产品销售期间（创建产品命令的内聚要素块）。
 *
 * <p>把「同进同出」的售期起止两字段收敛为一个值对象（红线 17 第 1 条：按内聚度拆分为嵌套值对象），
 * 使 {@code CreateProductCommand} 的调用方少对齐两个位置参数——该命令原有 31 个平铺组件，
 * 是 DSN-04「大 record 拆分」的公开样板（m25-06）。</p>
 *
 * <p>两侧均可为 {@code null}：表达「未限定起点 / 终点」，与拆分前的字段级 null 语义一致
 * （聚合装配时逐字段提取，见 {@code InsuranceProduct} 创建构造器）。</p>
 *
 * <p>🔴 与 {@code effectiveTime}（产品生效时间）是两个不同维度：生效时间是产品上架的时刻，
 * 销售期间是可售窗口，二者由不同业务动作决定，勿合并。</p>
 *
 * @param saleStartTime 销售开始时间（{@code null} = 未限定）
 * @param saleEndTime   销售结束时间（{@code null} = 未限定）
 */
public record SalesPeriod(LocalDateTime saleStartTime, LocalDateTime saleEndTime) {
}
