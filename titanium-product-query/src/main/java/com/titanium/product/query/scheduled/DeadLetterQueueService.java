package com.titanium.product.query.scheduled;

import java.util.Optional;

import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.deadletter.SequencedDeadLetterProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 死信队列监控 + 重试服务（CQRS 读侧最终一致性保障）
 * <p>
 * 定时扫描 {@code product-query-group} 处理组的死信队列（DLQ），重试此前投影失败的事件。 配合 bootstrap
 * 中开启的 DLQ 配置使用，保证产品/模板读模型投影的最终一致性。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    /** 投影处理组名，与 ProductProjectionEventHandler 的 @ProcessingGroup 一致 */
    private static final String PROCESSING_GROUP = "product-query-group";

    private final EventProcessingConfiguration eventProcessingConfig;

    /**
     * 定时扫描死信队列并重试失败事件（每30秒一次）
     * <p>
     * {@link SequencedDeadLetterProcessor#processAny()} 会取出任意一个待处理的死信序列尝试重新投递， 成功则从
     * DLQ 移除，失败则保留待下次重试。
     * </p>
     */
    @Scheduled(fixedRate = 30000)
    public void retryDeadLetterEvents() {
        Optional<SequencedDeadLetterProcessor<EventMessage<?>>> processorOpt =
                eventProcessingConfig.sequencedDeadLetterProcessor(PROCESSING_GROUP);

        if (processorOpt.isEmpty()) {
            log.debug("处理组 {} 未启用死信队列，跳过重试", PROCESSING_GROUP);
            return;
        }

        SequencedDeadLetterProcessor<EventMessage<?>> processor = processorOpt.get();
        try {
            boolean processed = processor.processAny();
            if (processed) {
                log.info("死信队列重试成功一条序列: group={}", PROCESSING_GROUP);
            } else {
                log.debug("死信队列为空或无可重试序列: group={}", PROCESSING_GROUP);
            }
        } catch (Exception e) {
            log.error("死信队列重试异常: group={}", PROCESSING_GROUP, e);
        }
    }
}
