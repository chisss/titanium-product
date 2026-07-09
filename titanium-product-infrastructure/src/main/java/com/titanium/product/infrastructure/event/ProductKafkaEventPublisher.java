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
 * 跨域事件消费方：
 * <ul>
 *   <li>Policy 域：消费 ProductCreatedEvent / ProductAuditedEvent，获取出单流程/保单形态/核保配置</li>
 *   <li>Underwriting 域：消费 ProductAuditedEvent，获取核保配置</li>
 *   <li>Clause 域：消费 ProductCreatedEvent，获取条款绑定关系</li>
 * </ul>
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

    @EventHandler
    public void on(ProductCreatedEvent event) {
        publish(ProductConstants.TOPIC_PRODUCT_CREATED, event.productId(), event);
    }

    @EventHandler
    public void on(ProductAuditedEvent event) {
        publish(ProductConstants.TOPIC_PRODUCT_AUDITED, event.productId(), event);
    }

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
