package com.titanium.product.command.pricing.ncd;

import java.time.LocalDateTime;

/**
 * NCD（无赔款优待）反算应用命令（G11/g11-03）
 * <p>
 * 应用层入参模型（JPA 同步服务形态，**不经** Axon 总线），承载「按谁、在什么时点、按什么车型
 * 反算 NCD」三个定位要素。反算所需的其余一切（统计区间长度、次数档位、系数）都取自 product 域
 * 已发布的 NCD 规则集，**调用方不得透传系数或赔案条数**——否则反算口径可被上游改写。
 * </p>
 *
 * @param tenantId     租户ID
 * @param productId    产品ID
 * @param vin          车辆识别代号（与 subjectId 至少提供一个，VIN 优先）
 * @param subjectId    标的标识（VIN 缺失时的兜底检索键）
 * @param vehicleType  车型标识；{@code null} 表示不按车型取档（只有通用档可命中）
 * @param businessTime 业务时点（决定取哪一版规则集、以及统计区间的截止日）
 */
public record ResolveNcdCommand(
        String tenantId,
        String productId,
        String vin,
        String subjectId,
        String vehicleType,
        LocalDateTime businessTime) {
}
