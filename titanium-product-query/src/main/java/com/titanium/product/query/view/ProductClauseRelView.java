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
 * 产品条款关联读模型实体（CQRS Projection）
 * <p>
 * 对应读模型表 {@code t_product_clause_rel_view}，以产品ID为主键，条款关联清单以 JSON 整体存储。 由
 * {@link com.titanium.product.query.handler.projection.ProductClauseProjectionEventHandler} 订阅
 * {@code ProductClauseRelUpdatedEvent} 投影维护。查询侧据此返回产品绑定条款，无需回查写侧。
 * </p>
 * <p>
 * 继承 {@link BaseView}，复用租户ID、创建/更新时间、乐观锁版本等读模型公共字段。
 * </p>
 */
@Entity
@Table(name = "t_product_clause_rel_view")
@Getter
@Setter
public class ProductClauseRelView extends BaseView {

    /** 产品ID（写侧聚合根ID，读模型主键） */
    @Id
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    /** 条款关联清单（JSON 序列化的 ProductClauseRel 列表） */
    @Lob
    @Column(name = "clause_rels_json", columnDefinition = "TEXT")
    private String clauseRelsJson;
}
