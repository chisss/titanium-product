package com.titanium.product.query.handler.projection;

import java.time.LocalDateTime;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.event.ProductClauseRelUpdatedEvent;
import com.titanium.product.event.ProductCreatedEvent;
import com.titanium.product.event.ProductRevisedEvent;
import com.titanium.product.query.repository.ProductClauseRelViewRepository;
import com.titanium.product.query.view.ProductClauseRelView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品条款关联读模型投影事件处理器（CQRS 读侧）
 * <p>
 * 将产品的条款绑定清单投影到读模型表 {@code t_product_clause_rel_view}，条款关联以 JSON 整体序列化存储。
 * 处理组 {@code product-query-group}，与写侧隔离。订阅三类事件：
 * </p>
 * <ul>
 * <li>{@link ProductCreatedEvent}：产品创建即携带初始条款绑定，据此新建条款读模型（此前遗漏，
 * 导致新建产品的条款读模型始终为空，详情页无法呈现绑定条款）；</li>
 * <li>{@link ProductRevisedEvent}：修订生成新产品ID，据 {@code newClauseRels} 新建读模型，
 * 租户从原产品记录继承（修订事件未携带租户）；</li>
 * <li>{@link ProductClauseRelUpdatedEvent}：草稿态整体覆盖条款绑定清单。</li>
 * </ul>
 * <p>
 * 读模型继承 {@code BaseView}，{@code tenant_id} 非空，故创建/修订投影必须显式赋租户与审计时间。
 * </p>
 */
@Slf4j
@Component
@ProcessingGroup("product-query-group")
@RequiredArgsConstructor
public class ProductClauseProjectionEventHandler {

    private final ProductClauseRelViewRepository clauseRelViewRepository;

    /**
     * 投影产品创建事件：据初始条款绑定新建条款读模型
     */
    @EventHandler
    @Transactional
    public void on(ProductCreatedEvent event) {
        log.info("[读模型投影] 产品条款关联初始化: productId={}, 条款数={}", event.productId(),
                event.clauseRels() != null ? event.clauseRels().size() : 0);

        ProductClauseRelView view =
                clauseRelViewRepository.findById(event.productId()).orElseGet(ProductClauseRelView::new);
        view.setProductId(event.productId());
        view.setClauseRelsJson(event.clauseRels() != null ? JSON.toJSONString(event.clauseRels()) : null);
        view.setTenantId(event.tenantId());
        stampAuditTime(view);
        clauseRelViewRepository.save(view);
    }

    /**
     * 投影产品修订事件：修订生成新产品ID，据新条款绑定新建读模型，租户从原产品继承
     */
    @EventHandler
    @Transactional
    public void on(ProductRevisedEvent event) {
        log.info("[读模型投影] 产品条款关联修订: newProductId={}, originalProductId={}, 条款数={}",
                event.newProductId(), event.originalProductId(),
                event.newClauseRels() != null ? event.newClauseRels().size() : 0);

        // 修订事件未携带租户，从原产品条款读模型继承（与主投影处理器一致）
        String inheritedTenantId = clauseRelViewRepository.findById(event.originalProductId())
                .map(origin -> origin.getTenantId())
                .orElse(null);

        ProductClauseRelView view =
                clauseRelViewRepository.findById(event.newProductId()).orElseGet(ProductClauseRelView::new);
        view.setProductId(event.newProductId());
        view.setClauseRelsJson(event.newClauseRels() != null ? JSON.toJSONString(event.newClauseRels()) : null);
        view.setTenantId(inheritedTenantId);
        stampAuditTime(view);
        clauseRelViewRepository.save(view);
    }

    /**
     * 投影产品条款关联更新事件：整体覆盖该产品的条款绑定清单
     * <p>
     * 该事件在产品创建之后（草稿态）触发，读模型应已由 {@link #on(ProductCreatedEvent)} 建立；
     * 缺失时告警跳过（可能事件乱序，由 DLQ 重试），避免以空租户插入违反非空约束。
     * </p>
     */
    @EventHandler
    @Transactional
    public void on(ProductClauseRelUpdatedEvent event) {
        log.info("[读模型投影] 产品条款关联更新: productId={}, 条款数={}", event.productId(),
                event.clauseRels() != null ? event.clauseRels().size() : 0);

        clauseRelViewRepository.findById(event.productId()).ifPresentOrElse(view -> {
            view.setClauseRelsJson(event.clauseRels() != null ? JSON.toJSONString(event.clauseRels()) : null);
            view.setUpdateTime(LocalDateTime.now());
            clauseRelViewRepository.save(view);
        }, () -> log.warn("[读模型投影] 产品条款关联更新失败：未找到读模型记录 productId={}（可能事件乱序，将由DLQ重试）",
                event.productId()));
    }

    /**
     * 统一填充读模型审计时间戳：createTime 仅首次创建时写入、updateTime 每次投影刷新。
     */
    private void stampAuditTime(ProductClauseRelView view) {
        LocalDateTime now = LocalDateTime.now();
        if (view.getCreateTime() == null) {
            view.setCreateTime(now);
        }
        view.setUpdateTime(now);
    }
}
