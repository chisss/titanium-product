package com.titanium.product.infrastructure.messaging;

import java.time.LocalDateTime;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSONObject;

import com.titanium.common.messaging.Idempotent;
import com.titanium.metadata.topic.CrossDomainTopics;
import com.titanium.product.infrastructure.messaging.inbound.ClaimCreatedMessage;
import com.titanium.product.infrastructure.messaging.inbound.ClaimRejectedMessage;
import com.titanium.product.infrastructure.messaging.inbound.ClaimStatusChangedMessage;
import com.titanium.product.infrastructure.messaging.inbound.ClaimUpdatedMessage;
import com.titanium.product.query.repository.ClaimHistoryViewRepository;
import com.titanium.product.query.view.ClaimHistoryView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 历史赔案投影监听器（product 域 Kafka 入站适配器 / 防腐层，m27-04/PKG-04）
 *
 * <p>订阅理赔域四个赔案主题，维护本域读模型 {@code t_product_claim_history_view}，供 NCD 反算
 * 本地取数。🔴 <b>这是打破 {@code product → claim} 同步依赖的关键</b>：原实现由
 * {@code ClaimHistoryAdapter} 直接 Feign 调理赔域，使服务依赖图出现反馈边并成环
 * （{@code policy → billing → product → claim → policy}）；改为事件投影后，产品域只依赖
 * 「理赔域已经发布的事实」，不再依赖理赔域服务本身。</p>
 *
 * <h3>🔴 幂等与乱序（乱序是真问题，不是理论风险）</h3>
 * <p>四个主题各自分区，<b>跨主题的到达顺序没有保证</b>：{@code claim-status-changed} /
 * {@code claim-rejected} 可能先于 {@code claim-created} 到达。处理方式：</p>
 * <ul>
 *   <li>任何事件到达时若本地无该 claimId 行，先落一行「状态桩」（仅 claimId/tenantId/状态），
 *       不因「找不到行」丢弃——丢弃会让拒赔事实永久丢失，NCD 把该案计入赔款次数；</li>
 *   <li>{@code claim-created} 到达时补齐 VIN/标的/出险时间，且<b>不覆盖已有状态</b>——
 *       状态事件可能先到，用 created 的缺省 PENDING 覆盖会把 REJECTED 抹掉；</li>
 *   <li>全部写入按 claimId 幂等 upsert，Kafka 至少一次投递与重试重投均安全。</li>
 * </ul>
 * <p>状态桩缺 VIN/标的/出险时间，不会命中 NCD 的区间查询，created 到达后自愈。</p>
 *
 * <h3>🔴 有意不订阅 {@code claim-closed}</h3>
 * <p>该主题<b>在理赔域只有常量、没有发布点</b>，且 {@code ClaimClosedEvent} 不携带 tenantId
 * （多租户投影无法定位租户）。造成两种事实的可见性差异，须知晓：</p>
 * <ul>
 *   <li>本投影：拒赔后即使结案，状态仍停留在 {@code REJECTED}，该案不计入 NCD 赔款次数；</li>
 *   <li>理赔域自身检索：状态变为 {@code CLOSED} 后不再匹配「排除 REJECTED」，该案会被计入。</li>
 * </ul>
 * <p>本投影的口径与 NCD 业务规则（拒赔不构成赔款）一致，差异已登记于 ADR-0002 与适配器注释。</p>
 *
 * <h3>失败语义</h3>
 * <p>报文畸形 / 缺主键 → 记 warn 后跳过（单条坏报文不得阻塞消费位点）；<b>读模型写入失败则
 * 原样上抛</b>，由 Kafka 容器有界重试与死信主题兜底（G16/AC-03：入站失败不得静默）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimHistoryEventListener {

    /** 赔案创建后的缺省状态（理赔域 {@code ClaimStatus.PENDING} 的序列化值） */
    private static final String STATUS_PENDING = "PENDING";

    /** 拒赔状态（理赔域 {@code ClaimStatus.REJECTED} 的序列化值） */
    private static final String STATUS_REJECTED = "REJECTED";

    private final ClaimHistoryViewRepository claimHistoryViewRepository;

    /**
     * 消费赔案创建事件：建档并补齐 NCD 取数要素。
     *
     * @param payload 事件 JSON 报文
     */
    @Idempotent(strategy = Idempotent.Strategy.DERIVED_KEY,
            reason = "claimId 即读模型主键，findById+upsert 重投命中同一行；created 不覆盖已落地状态，重投等价")
    @KafkaListener(topics = CrossDomainTopics.CLAIM_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onClaimCreated(String payload) {
        ClaimCreatedMessage message = parse(payload, ClaimCreatedMessage.class, CrossDomainTopics.CLAIM_CREATED);
        if (message == null || !hasIdentity(message.claimIdValue(), message.tenantId(), payload)) {
            return;
        }
        ClaimHistoryView view = loadOrCreate(message.claimIdValue());
        view.setTenantId(message.tenantId());
        view.setVin(message.vin());
        view.setSubjectId(message.subjectId());
        view.setIncidentDate(message.incidentDate());
        // 状态事件可能先到：只在尚无状态时补缺省 PENDING，绝不覆盖已落地的变更后状态
        if (view.getStatus() == null) {
            view.setStatus(STATUS_PENDING);
        }
        save(view);
    }

    /**
     * 消费赔案更新事件：同步可更正的出险时间（NCD 区间过滤列）。
     *
     * @param payload 事件 JSON 报文
     */
    @Idempotent(strategy = Idempotent.Strategy.DERIVED_KEY,
            reason = "claimId 即读模型主键，重投按同一行 upsert；仅同步 incidentDate，重复写入结果相同")
    @KafkaListener(topics = CrossDomainTopics.CLAIM_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onClaimUpdated(String payload) {
        ClaimUpdatedMessage message = parse(payload, ClaimUpdatedMessage.class, CrossDomainTopics.CLAIM_UPDATED);
        if (message == null || !hasIdentity(message.claimIdValue(), message.tenantId(), payload)) {
            return;
        }
        ClaimHistoryView view = loadOrCreate(message.claimIdValue());
        view.setTenantId(message.tenantId());
        if (message.incidentDate() != null) {
            view.setIncidentDate(message.incidentDate());
        }
        save(view);
    }

    /**
     * 消费赔案状态变更事件：更新状态列。
     *
     * @param payload 事件 JSON 报文
     */
    @Idempotent(strategy = Idempotent.Strategy.DERIVED_KEY,
            reason = "claimId 即读模型主键，重投按同一行 upsert 同一状态，结果等价")
    @KafkaListener(topics = CrossDomainTopics.CLAIM_STATUS_CHANGED,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onClaimStatusChanged(String payload) {
        ClaimStatusChangedMessage message = parse(payload, ClaimStatusChangedMessage.class,
                CrossDomainTopics.CLAIM_STATUS_CHANGED);
        if (message == null || !hasIdentity(message.claimIdValue(), message.tenantId(), payload)) {
            return;
        }
        if (message.newStatus() == null || message.newStatus().isBlank()) {
            log.warn("赔案状态变更事件缺少 newStatus，跳过, claimId={}, payload={}",
                    message.claimIdValue(), payload);
            return;
        }
        upsertStatus(message.claimIdValue(), message.tenantId(), message.newStatus());
    }

    /**
     * 消费赔案拒赔事件：置 REJECTED（NCD 统计的唯一排除状态）。
     *
     * <p>理赔域的拒赔路径只发本事件、不发 {@code claim-status-changed}，故本处理器不可省略。</p>
     *
     * @param payload 事件 JSON 报文
     */
    @Idempotent(strategy = Idempotent.Strategy.DERIVED_KEY,
            reason = "claimId 即读模型主键，重投按同一行 upsert REJECTED，结果等价")
    @KafkaListener(topics = CrossDomainTopics.CLAIM_REJECTED, groupId = "${spring.kafka.consumer.group-id}")
    public void onClaimRejected(String payload) {
        ClaimRejectedMessage message = parse(payload, ClaimRejectedMessage.class, CrossDomainTopics.CLAIM_REJECTED);
        if (message == null || !hasIdentity(message.claimIdValue(), message.tenantId(), payload)) {
            return;
        }
        upsertStatus(message.claimIdValue(), message.tenantId(), STATUS_REJECTED);
    }

    /**
     * 按 claimId 幂等 upsert 状态（无行时落状态桩，见类注释「乱序」）。
     */
    private void upsertStatus(String claimId, String tenantId, String status) {
        ClaimHistoryView view = loadOrCreate(claimId);
        view.setTenantId(tenantId);
        view.setStatus(status);
        save(view);
    }

    /**
     * 载入既有行；不存在则新建仅含主键的空行（待调用方填充）。
     */
    private ClaimHistoryView loadOrCreate(String claimId) {
        return claimHistoryViewRepository.findById(claimId).orElseGet(() -> {
            ClaimHistoryView view = new ClaimHistoryView();
            view.setClaimId(claimId);
            return view;
        });
    }

    /**
     * 写入读模型：创建时间只在首次落地时赋值，更新时间逐次刷新（与既有投影一致）。
     */
    private void save(ClaimHistoryView view) {
        LocalDateTime now = LocalDateTime.now();
        if (view.getCreateTime() == null) {
            view.setCreateTime(now);
        }
        view.setUpdateTime(now);
        claimHistoryViewRepository.save(view);
    }

    /**
     * 校验投影主键与租户是否齐备（缺任一即无法落行）。
     */
    private boolean hasIdentity(String claimId, String tenantId, String payload) {
        if (claimId == null || claimId.isBlank() || tenantId == null || tenantId.isBlank()) {
            log.warn("赔案事件缺少 claimId/tenantId，跳过投影, claimId={}, tenantId={}, payload={}",
                    claimId, tenantId, payload);
            return false;
        }
        return true;
    }

    /**
     * 一次性反序列化为防腐入站 record；畸形报文记 warn 后返回 {@code null}（不阻塞位点）。
     */
    private <T> T parse(String payload, Class<T> type, String topic) {
        try {
            T message = JSONObject.parseObject(payload, type);
            if (message == null) {
                log.warn("赔案事件报文为空，跳过投影, topic={}, payload={}", topic, payload);
            }
            return message;
        } catch (RuntimeException e) {
            log.warn("赔案事件报文解析失败，跳过本条（不阻塞消费位点）, topic={}, payload={}", topic, payload, e);
            return null;
        }
    }
}
