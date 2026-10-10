package com.titanium.product.web.dto.product;

/**
 * 条款关联入参（后台/端上 HTTP 入参的公共片段）
 *
 * <p>产品↔条款绑定的写入路径（创建 / 修订 / 整体替换）共用同一入参形态：条款主键 + 版本 + 主条款标记。
 * 此前它作为 {@code ReviseProductDTO} 的内部 record 存在，新增「整体替换」入口后会变成两份同形 record，
 * 故提取为顶层类型——<b>同一个数据形状不得在两处各写一份</b>（项目 CLAUDE.md §3.4.11）。</p>
 *
 * @param clauseId      条款<b>主键</b>；传条款编码会被条款域判为「不存在」（R7-01 的故障值）
 * @param clauseVersion 绑定的条款版本（产品域刻意绑定具体版本，使条款后续修订不影响已绑产品）
 * @param isMainClause  是否主条款；{@code null} 视同<b>非</b>主条款（{@code Boolean.TRUE.equals} 判定）
 */
public record ClauseRelDTO(String clauseId, String clauseVersion, Boolean isMainClause) {
}
