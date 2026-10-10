package com.titanium.product.query.view;

import com.titanium.common.jpa.BaseView;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 责任级保额档次配置读模型实体（CQRS Projection）
 * <p>
 * 对应读模型表 {@code t_product_coverage_sum_insured_view}，以产品ID为主键，
 * <b>该产品全部责任的</b>保额档次配置以 JSON 整体存储（一条产品一行，不是一条责任一行——
 * 档次总是「按产品整体替换」，拆行只会让整体替换语义退化成逐责任拼装）。
 * </p>
 * <p>
 * 由 {@link com.titanium.product.query.handler.projection.CoverageSumInsuredProjectionEventHandler} 订阅
 * {@code ProductCreatedEvent} / {@code ProductRevisedEvent} / {@code CoverageSumInsuredConfigUpdatedEvent}
 * 投影维护。查询侧据此返回档次清单，无需回查写侧聚合。
 * </p>
 * <p>
 * 继承 {@link BaseView}，复用租户ID、创建/更新时间、乐观锁版本等读模型公共字段。
 * </p>
 */
@Entity
@Table(name = "t_product_coverage_sum_insured_view")
@Getter
@Setter
public class CoverageSumInsuredConfigView extends BaseView {

    /** 产品ID（写侧聚合根ID，读模型主键） */
    @Id
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    /** 责任保额档次配置清单（JSON 序列化的 CoverageSumInsuredConfig 列表） */
    @Lob
    @Column(name = "configs_json", columnDefinition = "TEXT")
    private String configsJson;
}
