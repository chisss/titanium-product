package com.titanium.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 产品服务启动类
 * 产品服务的入口点，负责启动Spring Boot应用
 * <p>
 * 组合根：写侧聚合已纯事件溯源（Axon 持久化事件流），JPA 仅承载 CQRS 读侧读模型
 * （query.view/query.repository）；开启定时任务以驱动读侧死信队列（DLQ）重试，保障读模型投影最终一致。
 * </p>
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "com.titanium.product.api")
// 除读模型外，须显式纳入 Axon 的 JPA 实体（事件流/快照/位点/Saga），否则事件存储运行时找不到实体
@EntityScan(basePackages = {
        "com.titanium.product.query.view",
        "org.axonframework.eventsourcing.eventstore.jpa",
        "org.axonframework.eventhandling.tokenstore.jpa",
        "org.axonframework.modelling.saga.repository.jpa"
})
@EnableJpaRepositories(basePackages = {
        "com.titanium.product.query.repository"
})
public class ProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
