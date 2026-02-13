package com.titanium.product.infrastructure.listener;

import org.springframework.stereotype.Component;

/**
 * 条款域事件监听器
 * 监听Clause域发布的事件（条款版本更新、条款停用等），
 * 触发Product域内的相应处理逻辑
 *
 * 监听事件：
 * - ClauseVersionUpdatedEvent → 更新产品绑定的条款版本
 * - ClauseDeactivatedEvent → 检查并预警影响到的产品
 *
 * TODO: 接入实际的Kafka Consumer后启用
 */
@Component
public class ClauseEventListener {

    // @KafkaListener(topics = "titanium.clause.version-updated")
    // public void onClauseVersionUpdated(ClauseVersionUpdatedEvent event) {
    //     // 查询绑定了该条款的产品
    //     // 根据策略决定是否自动更新条款版本
    // }

    // @KafkaListener(topics = "titanium.clause.deactivated")
    // public void onClauseDeactivated(ClauseDeactivatedEvent event) {
    //     // 查询绑定了该条款的产品
    //     // 对草稿产品发出告警，对生效产品记录预警
    // }
}
