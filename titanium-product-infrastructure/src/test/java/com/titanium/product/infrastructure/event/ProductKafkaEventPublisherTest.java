package com.titanium.product.infrastructure.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.common.constant.ProductConstants;
import com.titanium.product.event.ProductInvalidatedEvent;

/**
 * 产品 Kafka 事件发布器测试
 * <p>
 * 以 mockito-core 手动 mock {@link KafkaTemplate}，校验外发契约：topic 正确、
 * 分区键为聚合ID、事件体被 JSON 序列化（非直接发对象）。纯单元测试，不启动容器。
 * </p>
 */
class ProductKafkaEventPublisherTest {

    @Test
    @DisplayName("产品下架事件外发到正确 topic，key 为 productId，值为 JSON 字符串")
    void shouldPublishInvalidatedEventAsJson() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ProductKafkaEventPublisher publisher = new ProductKafkaEventPublisher(kafkaTemplate);

        ProductInvalidatedEvent event = new ProductInvalidatedEvent(
                "PROD-001", ProductEnum.ProductStatus.INVALID, LocalDateTime.now());

        publisher.on(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertEquals(ProductConstants.TOPIC_PRODUCT_INVALIDATED, topicCaptor.getValue());
        assertEquals("PROD-001", keyCaptor.getValue());
        // 值为 fastjson2 序列化的 JSON 字符串，含 productId 字段
        assertTrue(valueCaptor.getValue().contains("\"productId\":\"PROD-001\""),
                "事件体应为 JSON 字符串: " + valueCaptor.getValue());
    }
}
