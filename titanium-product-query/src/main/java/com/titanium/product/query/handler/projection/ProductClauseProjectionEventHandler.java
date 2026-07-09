package com.titanium.product.query.handler.projection;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.event.ProductClauseRelUpdatedEvent;
import com.titanium.product.query.repository.ProductClauseRelViewRepository;
import com.titanium.product.query.view.ProductClauseRelView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品条款关联读模型投影事件处理器（CQRS 读侧）
 * <p>
 * 订阅 {@link ProductClauseRelUpdatedEvent}，将产品的条款绑定清单投影到读模型表
 * {@code t_product_clause_rel_view}，条款关联以 JSON 整体序列化存储。 处理组
 * {@code product-query-group}，与写侧隔离。
 * </p>
 */
@Slf4j
@Component
@ProcessingGroup("product-query-group")
@RequiredArgsConstructor
public class ProductClauseProjectionEventHandler {

    private final ProductClauseRelViewRepository clauseRelViewRepository;

    /**
     * 投影产品条款关联更新事件：整体覆盖该产品的条款绑定清单
     */
    @EventHandler
    @Transactional
    public void on(ProductClauseRelUpdatedEvent event) {
        log.info("[读模型投影] 产品条款关联更新: productId={}, 条款数={}", event.productId(),
                event.clauseRels() != null ? event.clauseRels().size() : 0);

        ProductClauseRelView view =
                clauseRelViewRepository.findById(event.productId()).orElseGet(ProductClauseRelView::new);
        view.setProductId(event.productId());
        view.setClauseRelsJson(event.clauseRels() != null ? JSON.toJSONString(event.clauseRels()) : null);
        clauseRelViewRepository.save(view);
    }
}
