package com.titanium.product.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.titanium.common.messaging.DltMetricsRecoverer;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Kafka 入站消费配置（m27-04/PKG-04）
 *
 * <p>本域引入 {@code spring-boot-starter-kafka}，消费工厂/容器工厂由 Boot 自动配置
 * （{@code KafkaAnnotationDrivenConfiguration} 已启用 {@code @EnableKafka}），故这里<b>不重复声明</b>
 * ConsumerFactory/ContainerFactory——只补一件 Boot 默认不提供的事：<b>入站失败的死信兜底</b>。</p>
 *
 * <p>🔴 <b>不声明本 Bean 的后果</b>：Spring Kafka 容器退回默认 {@code DefaultErrorHandler}
 * （10 次零间隔重试后丢弃记录），赔案投影事件会在重试耗尽后<b>静默消失</b>——NCD 反算把「投影丢了」
 * 当成「没有赔案」，给出偏高优待。G16/AC-03 明确要求入站失败不得静默，故与 billing 域同构：
 * 有界重试 + 死信主题 + 指标计数。</p>
 *
 * <p>装配方式：Boot 的 {@code ConcurrentKafkaListenerContainerFactoryConfigurer} 会拾取容器内唯一的
 * {@link CommonErrorHandler} Bean 挂到监听容器工厂（Boot 4.0.7 实测：该类有
 * {@code commonErrorHandler} 字段，见 {@code javap} 输出）。</p>
 */
@Configuration
public class KafkaConsumerConfig {

    /** 有界重试：3 次 × 2s，与 billing/policy 域既有口径一致 */
    private static final long RETRY_INTERVAL_MS = 2000L;

    /** 有界重试次数（不含首次消费） */
    private static final long RETRY_ATTEMPTS = 3L;

    /**
     * 入站消息错误处理器：重试耗尽后把记录投到 {@code <原主题>-dlt}。
     *
     * <p>死信投递经 {@link DltMetricsRecoverer} 旁路计数
     * （{@code titanium.kafka.dlt.published} / {@code titanium.kafka.dlt.recovery.failed}），
     * 死信条数与投递失败可被 Prometheus 采集告警。</p>
     *
     * @param kafkaTemplate   Boot 自动配置的生产者模板（本域 yml 为 String 序列化）
     * @param meterRegistry   指标注册表
     * @return 默认错误处理器
     */
    @Bean
    public CommonErrorHandler kafkaCommonErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate,
            MeterRegistry meterRegistry) {
        return new DefaultErrorHandler(
                new DltMetricsRecoverer(new DeadLetterPublishingRecoverer(kafkaTemplate), meterRegistry),
                new FixedBackOff(RETRY_INTERVAL_MS, RETRY_ATTEMPTS));
    }
}
