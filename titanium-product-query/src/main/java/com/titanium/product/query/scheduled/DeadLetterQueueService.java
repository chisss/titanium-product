package com.titanium.product.query.scheduled;

import java.util.List;
import java.util.Optional;

import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.deadletter.SequencedDeadLetterProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * 死信队列监控 + 重试服务（CQRS 读侧最终一致性 + 跨域出站可靠性保障）
 * <p>
 * 定时扫描各处理组的死信队列（DLQ），重试此前失败的事件序列。覆盖两条链路：
 * </p>
 * <ul>
 *   <li>{@code product-query-group} —— 读侧投影组：投影失败的事件重放，保障产品/模板读模型最终一致；</li>
 *   <li>{@code product-kafka-group} —— 跨域出站组：Kafka 发布失败的事件重发。该组承载产品目录三主题
 *       （created/audited/invalidated），仓内无消费者、定位为对外集成出口，但仍须保证不漏发。</li>
 * </ul>
 * <p>
 * 🔴 组名须与 application.yml 的 {@code axon.eventhandling.processors} 键及各类
 * {@code @ProcessingGroup} 取值三者一致；漂移时 {@code sequencedDeadLetterProcessor} 恒为空，
 * 本服务静默空转且不报错。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    /**
     * 需重投的处理组清单。
     * <p>两组职责不同、各自独立启停 DLQ，故须分别重投。</p>
     */
    private static final List<String> PROCESSING_GROUPS = List.of("product-query-group", "product-kafka-group");

    private final EventProcessingConfiguration eventProcessingConfig;

    /**
     * 定时扫描各处理组的死信队列并重试失败事件（每30秒一次）
     * <p>
     * {@link SequencedDeadLetterProcessor#processAny()} 会取出任意一个待处理的死信序列尝试重新投递， 成功则从
     * DLQ 移除，失败则保留待下次重试。
     * </p>
     */
        @SchedulerLock(name = "product-retryDeadLetterEvents")
    @Scheduled(fixedRate = 30000)
    public void retryDeadLetterEvents() {
        PROCESSING_GROUPS.forEach(this::retryGroup);
    }

    /**
     * 重投单个处理组的死信序列；该组未启用 DLQ 时静默跳过。
     */
    private void retryGroup(String processingGroup) {
        Optional<SequencedDeadLetterProcessor<EventMessage<?>>> processorOpt =
                eventProcessingConfig.sequencedDeadLetterProcessor(processingGroup);

        if (processorOpt.isEmpty()) {
            log.debug("处理组 {} 未启用死信队列，跳过重试", processingGroup);
            return;
        }

        SequencedDeadLetterProcessor<EventMessage<?>> processor = processorOpt.get();
        try {
            boolean processed = processor.processAny();
            if (processed) {
                log.info("死信队列重试成功一条序列: group={}", processingGroup);
            } else {
                log.debug("死信队列为空或无可重试序列: group={}", processingGroup);
            }
        } catch (Exception e) {
            log.error("死信队列重试异常: group={}", processingGroup, e);
        }
    }
}
