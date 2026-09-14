package com.titanium.product.infrastructure.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.axonframework.config.ProcessingGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.titanium.common.kafka.KafkaPublishException;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.common.constant.ProductConstants;
import com.titanium.product.event.ProductInvalidatedEvent;

/**
 * 产品 Kafka 事件发布器测试
 * <p>
 * 以 mockito 手动 mock {@link KafkaTemplate}，校验外发契约：topic 正确、
 * 分区键为聚合ID、事件体被 JSON 序列化（非直接发对象）。纯单元测试，不启动容器。
 * </p>
 * <p>
 * 另锁死出站可靠性两件事（m6-909）：④ 🔴 <b>发布失败必须抛出</b>——出站处理组为 tracking + DLQ 形态，
 * Axon 只对「处理器抛出的异常」入队死信；原实现直接丢弃 send 返回的 future，失败既不重投也不留痕；
 * ⑤ 出站组名与 {@code @ProcessingGroup} 一致，漂移会使 DLQ 与首启位点配置同时落空。
 * </p>
 */
class ProductKafkaEventPublisherTest {

    /** 租户ID：事件载荷须携带租户，供跨域消费端判定归属 */
    private static final String TENANT_ID = "T-1";

    private KafkaTemplate<String, String> kafkaTemplate;
    private ProductKafkaEventPublisher publisher;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        // 默认发布成功：发布器现在会等待 broker 确认（m6-909），未 stub 时 send 返回 null 会直接 NPE
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, String>) null));
        publisher = new ProductKafkaEventPublisher(kafkaTemplate);
    }

    @Test
    @DisplayName("产品下架事件外发到正确 topic，key 为 productId，值为 JSON 字符串")
    void shouldPublishInvalidatedEventAsJson() {
        ProductInvalidatedEvent event = new ProductInvalidatedEvent(
                "PROD-001", ProductEnum.ProductStatus.INVALID, LocalDateTime.now(), TENANT_ID);

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
        // 跨域载荷须带租户，否则消费端无法判定事件归属
        assertTrue(valueCaptor.getValue().contains("\"tenantId\":\"" + TENANT_ID + "\""),
                "事件体须携带 tenantId: " + valueCaptor.getValue());
    }

    @Test
    @DisplayName("🔴 发布失败必须抛出（不再丢弃 future），否则事件不会进死信队列、无法重投")
    void shouldThrowWhenSendFails() {
        CompletableFuture<SendResult<String, String>> failed = CompletableFuture
                .failedFuture(new RuntimeException("broker unreachable"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failed);

        ProductInvalidatedEvent event = new ProductInvalidatedEvent(
                "PROD-002", ProductEnum.ProductStatus.INVALID, LocalDateTime.now(), TENANT_ID);

        KafkaPublishException exception = assertThrows(KafkaPublishException.class, () -> publisher.on(event));

        assertTrue(exception.getMessage().contains(ProductConstants.TOPIC_PRODUCT_INVALIDATED),
                "异常须携带主题以便定位");
        assertNotNull(exception.getCause(), "须保留底层失败原因");
    }

    @Test
    @DisplayName("出站处理组名与 @ProcessingGroup 一致（漂移会使 DLQ 与首启位点配置同时落空）")
    void shouldDeclareProcessingGroupConsistently() {
        ProcessingGroup annotation = ProductKafkaEventPublisher.class.getAnnotation(ProcessingGroup.class);

        assertNotNull(annotation, "出站发布器必须声明 @ProcessingGroup");
        assertEquals(ProductKafkaEventPublisher.PROCESSING_GROUP, annotation.value());
        assertEquals("product-kafka-group", ProductKafkaEventPublisher.PROCESSING_GROUP,
                "组名须与 application.yml 的 axon.eventhandling.processors 键及 outbound-relay.groups 一致");
    }
}
