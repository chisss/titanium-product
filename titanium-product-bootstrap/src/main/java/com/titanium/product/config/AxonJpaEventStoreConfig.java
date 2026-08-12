package com.titanium.product.config;

import javax.sql.DataSource;

import org.axonframework.common.jdbc.PersistenceExceptionResolver;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventhandling.tokenstore.jpa.JpaTokenStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.SQLErrorCodesResolver;
import org.axonframework.serialization.Serializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Axon JPA 事件存储配置
 * <p>
 * 本域未接入 AxonServer（无注册中心的本地/容器环境），故必须显式提供基于 JPA 的
 * {@link EventStorageEngine}，否则事件溯源聚合（ProductTemplate 等）的 Repository
 * 无法装配，容器启动即失败。
 * </p>
 */
@Configuration
public class AxonJpaEventStoreConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /** 容器托管的 EntityManager 提供者 */
    @Bean
    public EntityManagerProvider entityManagerProvider() {
        return () -> entityManager;
    }

    /** 依数据源方言解析唯一键冲突等持久化异常（幂等去重依赖此判定） */
    @Bean
    public PersistenceExceptionResolver persistenceExceptionResolver(DataSource dataSource) {
        try {
            return new SQLErrorCodesResolver(dataSource);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 PersistenceExceptionResolver 失败", e);
        }
    }

    /** JPA 事件存储引擎（事件流落 axon_domain_event_entry） */
    @Bean
    public EventStorageEngine eventStorageEngine(Serializer defaultSerializer,
                                                 PersistenceExceptionResolver persistenceExceptionResolver,
                                                 EntityManagerProvider entityManagerProvider,
                                                 TransactionManager transactionManager) {
        return JpaEventStorageEngine.builder()
                .snapshotSerializer(defaultSerializer)
                .upcasterChain(org.axonframework.serialization.upcasting.event.NoOpEventUpcaster.INSTANCE)
                .persistenceExceptionResolver(persistenceExceptionResolver)
                .eventSerializer(defaultSerializer)
                .snapshotFilter(snapshot -> true)
                .transactionManager(transactionManager)
                .entityManagerProvider(entityManagerProvider)
                .build();
    }

    /** 跟踪型事件处理器的位点存储 */
    @Bean
    public TokenStore tokenStore(EntityManagerProvider entityManagerProvider, Serializer serializer) {
        return JpaTokenStore.builder()
                .entityManagerProvider(entityManagerProvider)
                .serializer(serializer)
                .build();
    }
}
