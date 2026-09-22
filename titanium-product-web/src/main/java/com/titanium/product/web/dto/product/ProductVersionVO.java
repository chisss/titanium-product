package com.titanium.product.web.dto.product;

import java.time.LocalDateTime;

/**
 * 产品版本条目（版本历史列表用）。
 * <p>
 * 版本历史只需要「定位版本 + 判定先后 + 看出状态」这几项，故本 VO **刻意不带配置块**：
 * 配置对比由前端按选中的两个版本各自取详情完成（{@code GET /web/v1/products/{productId}}），
 * 把全量配置塞进列表会让一次版本历史查询变成 N 份产品配置的传输。
 * </p>
 * <p>
 * {@code originalProductId} 是父版本指针（首个版本为空），前端据此可画出「本版由哪一版修订而来」；
 * {@code effectiveTime} 是审核通过时写入的生效时间，与销售起止时间（{@code saleStartTime}/{@code saleEndTime}）
 * 语义不同，二者都保留以免使用者误用一个当另一个。
 * </p>
 *
 * @param productId         产品ID（版本线内唯一，取值用）
 * @param productCode       产品编码（版本线的归组键，同一编码的多个 productId 即该产品的全部版本）
 * @param version           版本号（如 V1.0 / V2.0，由修订递增）
 * @param productName       产品名称（各版本可不同，故逐版本记录）
 * @param status            产品状态（枚举名：DRAFT/AUDITING/EFFECTIVE/INVALID）
 * @param originalProductId 父版本产品ID（首个版本为空）
 * @param effectiveTime     生效时间（审核通过时写入）
 * @param saleStartTime     销售起期
 * @param saleEndTime       销售止期
 * @param createdBy         创建人
 * @param createdAt         业务创建时间（版本先后的兜底排序依据）
 */
public record ProductVersionVO(
        String productId,
        String productCode,
        String version,
        String productName,
        String status,
        String originalProductId,
        LocalDateTime effectiveTime,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        String createdBy,
        LocalDateTime createdAt) {
}
