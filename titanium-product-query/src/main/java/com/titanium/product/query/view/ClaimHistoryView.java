package com.titanium.product.query.view;

import java.time.LocalDateTime;

import com.titanium.common.jpa.BaseView;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 历史赔案读模型（CQRS Projection，m27-04/PKG-04）
 *
 * <p>对应读模型表 {@code t_product_claim_history_view}，保存 NCD 反算所需的最小赔案事实
 * （赔案ID / VIN / 标的ID / 出险时间 / 状态）。由
 * {@code com.titanium.product.infrastructure.messaging.ClaimHistoryEventListener} 订阅理赔域
 * {@code claim-created} / {@code claim-updated} / {@code claim-status-changed} /
 * {@code claim-rejected} 四个主题投影维护。</p>
 *
 * <p>🔴 <b>为什么产品域要存一份赔案</b>：NCD 反算（无赔款优待）需要按车辆/标的统计历史赔案，
 * 而<b>同步调用理赔域</b>会使 {@code product → claim} 成为服务依赖图中的反馈边
 * （{@code policy → billing → product → claim → policy} 成环，见 ADR-0002）。改为消费理赔域
 * 已发布的领域事件、在本域维护投影后，依赖方向回到「只允许事件向上游之外传递」。</p>
 *
 * <p>🔴 <b>投影语义</b>：主键是 {@code claimId}，按事件做幂等 upsert。跨主题到达顺序无保证
 * （不同 topic 不同分区），故 {@code claim-status-changed}/{@code claim-rejected} 先于
 * {@code claim-created} 到达时先落一行「状态桩」（VIN/标的/出险时间为空），created 到达后再补齐
 * 要素，且<b>不覆盖已有状态</b>。状态桩因缺少 VIN/标的/出险时间，不会命中 NCD 区间查询。</p>
 *
 * <p>继承 {@link BaseView}，复用租户ID、创建/更新时间、乐观锁版本等读模型公共字段。</p>
 */
@Entity
@Table(name = "t_product_claim_history_view")
@Getter
@Setter
public class ClaimHistoryView extends BaseView {

    /** 赔案ID（理赔域聚合标识，跨域投影主键） */
    @Id
    @Column(name = "claim_id", nullable = false, length = 36)
    private String claimId;

    /** 车辆识别代号（NCD 按车取数的主路径；历史事件或状态桩可能为空） */
    @Column(name = "vin", length = 64)
    private String vin;

    /** 标的标识（VIN 缺失时的兜底路径） */
    @Column(name = "subject_id", length = 64)
    private String subjectId;

    /** 出险时间（NCD 统计区间 [from, to) 的过滤列） */
    @Column(name = "incident_date")
    private LocalDateTime incidentDate;

    /** 赔案状态（理赔域 {@code ClaimStatus} 的序列化值，如 PENDING/APPROVED/REJECTED/CLOSED） */
    @Column(name = "status", length = 20)
    private String status;
}
