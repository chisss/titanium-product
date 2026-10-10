package com.titanium.product.query.handler.projection;

import java.time.LocalDateTime;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.event.CoverageSumInsuredConfigUpdatedEvent;
import com.titanium.product.event.ProductCreatedEvent;
import com.titanium.product.event.ProductRevisedEvent;
import com.titanium.product.query.repository.CoverageSumInsuredConfigViewRepository;
import com.titanium.product.query.view.CoverageSumInsuredConfigView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 责任级保额档次配置读模型投影事件处理器（CQRS 读侧）
 * <p>
 * 将产品的责任保额档次清单投影到读模型表 {@code t_product_coverage_sum_insured_view}，
 * 以 JSON 整体序列化存储。处理组 {@code product-query-group}，与写侧隔离。订阅三类事件：
 * </p>
 * <ul>
 * <li>{@link ProductCreatedEvent}：新产品建立读模型行，档次清单初始为 {@code []}
 * （创建命令不承载档次，由后续更新命令按责任录入）；</li>
 * <li>{@link ProductRevisedEvent}：修订生成新产品ID，据 {@code newCoverageSumInsuredConfigs} 建立读模型行；</li>
 * <li>{@link CoverageSumInsuredConfigUpdatedEvent}：草稿态整体覆盖该产品的档次清单。</li>
 * </ul>
 * <p>
 * 🔴 <b>三条路径一律 upsert（找不到就建），且租户取自事件</b>：与同族
 * {@code ProductClauseProjectionEventHandler} 的「找不到原行即告警跳过」不同。本能力上线时
 * 存量产品（含全部在跑的 DRAFT 产品）<b>都没有对应读模型行</b>，沿用「跳过」会让给它们配置档次
 * 变成<b>写侧成功、读侧永远查不到</b>的静默失败。故事件携带 {@code tenantId}
 * （{@code ProductCreatedEvent}/{@code ProductRevisedEvent} 本就带），据此可安全插入。
 * </p>
 * <p>
 * 只做「事件 → 读模型」写入，<b>不发命令、不持有 {@code CommandGateway}</b>（项目规约 §3.4.9 ⑤）。
 * </p>
 */
@Slf4j
@Component
@ProcessingGroup("product-query-group")
@RequiredArgsConstructor
public class CoverageSumInsuredProjectionEventHandler {

    private final CoverageSumInsuredConfigViewRepository configViewRepository;

    /**
     * 投影产品创建事件：建立读模型行，档次清单初始为空
     */
    @EventHandler
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ProductCreatedEvent event) {
        log.info("[读模型投影] 保额档次配置初始化: productId={}", event.productId());
        saveJson(event.productId(), "[]", event.tenantId());
    }

    /**
     * 投影产品修订事件：据新版本档次清单建立读模型行，租户取自事件
     */
    @EventHandler
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ProductRevisedEvent event) {
        int size = event.newCoverageSumInsuredConfigs() != null ? event.newCoverageSumInsuredConfigs().size() : 0;
        log.info("[读模型投影] 保额档次配置修订: newProductId={}, originalProductId={}, 责任数={}",
                event.newProductId(), event.originalProductId(), size);
        String json = event.newCoverageSumInsuredConfigs() != null
                ? JSON.toJSONString(event.newCoverageSumInsuredConfigs()) : "[]";
        saveJson(event.newProductId(), json, event.tenantId());
    }

    /**
     * 投影档次配置更新事件：整体覆盖该产品的档次清单（upsert，不依赖原行是否存在）
     */
    @EventHandler
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(CoverageSumInsuredConfigUpdatedEvent event) {
        log.info("[读模型投影] 保额档次配置更新: productId={}, 责任数={}",
                event.productId(), event.coverageSumInsuredConfigs() != null
                        ? event.coverageSumInsuredConfigs().size() : 0);
        String json = event.coverageSumInsuredConfigs() != null
                ? JSON.toJSONString(event.coverageSumInsuredConfigs()) : "[]";
        saveJson(event.productId(), json, event.tenantId());
    }

    /**
     * upsert 读模型行：createTime 仅首次写入、updateTime 每次刷新
     */
    private void saveJson(String productId, String configsJson, String tenantId) {
        CoverageSumInsuredConfigView view =
                configViewRepository.findById(productId).orElseGet(CoverageSumInsuredConfigView::new);
        view.setProductId(productId);
        view.setConfigsJson(configsJson);
        view.setTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now();
        if (view.getCreateTime() == null) {
            view.setCreateTime(now);
        }
        view.setUpdateTime(now);
        configViewRepository.save(view);
    }
}
