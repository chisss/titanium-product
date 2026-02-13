package com.titanium.product.infrastructure.event;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import com.titanium.product.domain.event.ProductAuditedEvent;
import com.titanium.product.domain.event.ProductCreatedEvent;
import com.titanium.product.domain.event.ProductInvalidatedEvent;

/**
 * 产品Kafka事件发布者
 * 监听产品领域事件，发布到Kafka消息总线供其他域消费
 *
 * 跨域事件消费方：
 * - Policy域：消费ProductCreatedEvent / ProductAuditedEvent，获取出单流程/保单形态/核保配置
 * - Underwriting域：消费ProductAuditedEvent，获取核保配置
 * - Clause域：消费ProductCreatedEvent，获取条款绑定关系
 *
 * TODO: 接入实际的Kafka配置后启用
 */
@Component
public class ProductKafkaEventPublisher {

    // @Autowired
    // private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PRODUCT_CREATED = "titanium.product.created";
    private static final String TOPIC_PRODUCT_AUDITED = "titanium.product.audited";
    private static final String TOPIC_PRODUCT_INVALIDATED = "titanium.product.invalidated";

    @EventHandler
    public void on(ProductCreatedEvent event) {
        // TODO: kafkaTemplate.send(TOPIC_PRODUCT_CREATED, event.productId(), event);
    }

    @EventHandler
    public void on(ProductAuditedEvent event) {
        // TODO: kafkaTemplate.send(TOPIC_PRODUCT_AUDITED, event.productId(), event);
    }

    @EventHandler
    public void on(ProductInvalidatedEvent event) {
        // TODO: kafkaTemplate.send(TOPIC_PRODUCT_INVALIDATED, event.productId(), event);
    }
}
