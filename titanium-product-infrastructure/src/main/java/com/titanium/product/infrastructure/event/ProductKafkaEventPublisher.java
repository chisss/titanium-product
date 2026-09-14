package com.titanium.product.infrastructure.event;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.common.constant.ProductConstants;
import com.titanium.product.event.ProductAuditedEvent;
import com.titanium.product.event.ProductCreatedEvent;
import com.titanium.product.event.ProductInvalidatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品Kafka事件发布者
 * <p>
 * 监听产品领域事件，序列化为 JSON 后发布到 Kafka 消息总线供其它域消费。
 * 与客户域 {@code KafkaEventPublisher} 采用一致的桥接模式：Axon 进程内总线（simple）
 * 将事件投递给本处理器，处理器再经 spring-kafka 主动外发——无需将 Axon eventBus 替换为 Kafka。
 * </p>
 * <p>
 * 🔴 <b>跨域消费方：仓内零消费者，且经判定不应接线（2026-09-14 m6-907）</b>——三个主题（created/audited/
 * invalidated）均已外发，但全仓无任何 {@code @KafkaListener} 订阅。原 javadoc 曾记「Policy/Underwriting/Clause
 * 消费」，与实读不符：各域**对产品配置的取用一律是「业务时点的同步 Feign 实时查询」**，而非长期状态订阅——
 * policy 走 {@code ProductServicePort}（出单装配取产品名/版本/投保规则/条款绑定），maintenance 走
 * {@code ProductMaintenancePremiumQuoteAdapter} / {@code ProductPremiumLifecycleAdapter}（保全试算与生命周期调整）。
 * </p>
 * <p>
 * 更关键的是，<b>产品在售状态已由同步强校验承担</b>：出单必经 product CONFIRM 试算，其内部
 * {@code PremiumQuoteApplicationService.validateProductAvailability} 对非 {@code EFFECTIVE} 产品抛
 * {@code PRICING_PLAN_NOT_EFFECTIVE} 并<b>阻断出单</b>——「失败即阻断、强一致」优于「异步最终一致」，
 * 订阅这三个主题做在售校验属<b>降级</b>。且保单在出单时已冻结产品快照（编码/名称/版本/条款/责任），
 * 产品后续修订或下架<b>不回溯</b>存量保单（停售不波及已承保保单），policy 侧亦无「在途单批量作废」命令
 * 可供承接。故三主题定位为<b>对外集成出口</b>（三方系统 / 数据平台 / 产品目录同步），非仓内跨域事件。
 * </p>
 * <p>
 * 判定依据（四条判据 + 证据链）见 {@code docs/技术文档/跨域事件目录-2026-09.md} §六.15。
 * </p>
 */
@Slf4j
@Component
@ProcessingGroup(ProductKafkaEventPublisher.PROCESSING_GROUP)
@RequiredArgsConstructor
public class ProductKafkaEventPublisher {

    /** 跨域外发处理组，与写侧 product-group、读侧 product-query-group 隔离 */
    public static final String PROCESSING_GROUP = "product-kafka-group";

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 外发产品创建事件。
     * <p>
     * 🔴 仓内零消费者，**已判定不应接 policy**（2026-09-14 m6-907）：出单装配经 policy
     * {@code ProductServicePort.getProductBasicInfo} <b>同步</b>实时取产品编码/名称/版本，
     * 无需异步订阅；详见类注释与目录 §六.15。
     * </p>
     */
    @EventHandler
    public void on(ProductCreatedEvent event) {
        publish(ProductConstants.TOPIC_PRODUCT_CREATED, event.productId(), event);
    }

    /**
     * 外发产品审核（生效/驳回）事件。
     * <p>
     * 🔴 仓内零消费者，**已判定不应接 policy**（2026-09-14 m6-907）：产品是否在售由出单必经的
     * CONFIRM 试算<b>同步强校验</b>（非 {@code EFFECTIVE} 即抛 {@code PRICING_PLAN_NOT_EFFECTIVE} 阻断出单），
     * 异步订阅属降级；详见目录 §六.15。
     * </p>
     */
    @EventHandler
    public void on(ProductAuditedEvent event) {
        publish(ProductConstants.TOPIC_PRODUCT_AUDITED, event.productId(), event);
    }

    /**
     * 外发产品下架事件。
     * <p>
     * 🔴 仓内零消费者，**已判定不应接 policy**（2026-09-14 m6-907）：① 在售校验已由出单试算同步承担；
     * ② 已出保单为产品快照、下架<b>不回溯</b>（停售不波及已承保保单）；③ policy {@code Insurance} 聚合
     * 无作废命令，「在途投保单处置」能力尚不存在，订阅亦无从承接。详见目录 §六.15。
     * </p>
     */
    @EventHandler
    public void on(ProductInvalidatedEvent event) {
        publish(ProductConstants.TOPIC_PRODUCT_INVALIDATED, event.productId(), event);
    }

    /**
     * 统一外发：以聚合ID为分区键保证同一产品事件有序，事件体经 fastjson2 序列化为字符串。
     *
     * @param topic     目标 Kafka topic
     * @param productId 聚合ID，作为消息 key
     * @param event     领域事件对象
     */
    private void publish(String topic, String productId, Object event) {
        String eventJson = JSON.toJSONString(event);
        log.info("发布产品事件到 Kafka, topic: {}, productId: {}", topic, productId);
        kafkaTemplate.send(topic, productId, eventJson);
    }
}
