package com.titanium.product.port.clause;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 条款可绑定性的核查结论
 *
 * <p>{@link ClauseExistencePort} 一次往返同时回答两个问题，二者**必须分开承载**：</p>
 * <ul>
 *   <li>{@link #missingClauseIds()}：条款域**明确回答不存在**的引用 —— 调用方须判为用户输入错误；</li>
 *   <li>{@link #nonActiveClauseIds()}：条款域里**确实存在、但状态不允许绑定**的引用 —— 调用方须判为
 *       配置时序错误（该先让条款生效）—— 与上者混为一谈会把「条款还没批下来」说成「条款不存在」，
 *       运营照提示去新建一条同编码条款，只会撞上编码唯一约束。</li>
 * </ul>
 *
 * <p>两个列表均**保持入参顺序**，使错误提示稳定可复现。条款服务不可用时不由本对象承载 ——
 * 端口实现直接上抛，不会被表达成「不存在」（见 {@link ClauseExistencePort}）。</p>
 *
 * @param missingClauseIds   条款域确认不存在的条款主键
 * @param nonActiveClauseIds 存在但状态非 ACTIVE、当前不可绑定的条款主键
 */
public record ClauseBindingCheck(List<String> missingClauseIds, List<String> nonActiveClauseIds) {

    /**
     * 全部可绑定（无缺失、无状态不合规）。
     *
     * @return 空结论
     */
    public static ClauseBindingCheck clean() {
        return new ClauseBindingCheck(List.of(), List.of());
    }

    /**
     * 是否全部可绑定。
     *
     * <p>🔴 {@code @JsonIgnore} 不可删（D-501-18 / D-501-19，与 {@code DividendConfig} 同病）：
     * Axon 的 Jackson 序列化器为<b>严格模式</b>，record 上「非分量的派生 getter」会被当作普通属性
     * 写进载荷（{@code isBindable()} → {@code "bindable"}），反序列化端遇未知属性即抛
     * {@code UnrecognizedPropertyException}。该缺陷写入侧永远正常、只在事件被重新读取时暴露。</p>
     *
     * @return {@code true} 两个拒绝清单均为空
     */
    @JsonIgnore
    public boolean isBindable() {
        return missingClauseIds.isEmpty() && nonActiveClauseIds.isEmpty();
    }
}
