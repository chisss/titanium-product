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
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

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

    /**
     * Axon 事务管理器：显式绑定 Spring 的 {@link PlatformTransactionManager}（本域实际为 JPA 事务管理器）。
     * <p>
     * 🔴 不可依赖 Axon 的 {@code TransactionAutoConfiguration} 自动提供：它同样以 Boot 4 已迁包的旧类名声明
     * {@code @AutoConfigureAfter}，评估早于 JPA 自动配置注册 {@code transactionManager} 的时机，其
     * {@code @ConditionalOnBean(PlatformTransactionManager)} 遂不匹配，随后由
     * {@code NoOpTransactionAutoConfiguration} 兜底注入 {@code NoOpTransactionManager}。
     * 2026-10-19 条件评估报告实测：{@code TransactionAutoConfiguration#axonTransactionManager} did not match —
     * 「did not find any beans of type PlatformTransactionManager」，而
     * {@code NoOpTransactionAutoConfiguration#axonTransactionManager} matched。
     * </p>
     * <p>
     * NoOp 下 {@code JpaEventStorageEngine.appendEvents} 不在事务内执行，真实写事件即抛
     * {@code TransactionRequiredException: No EntityManager with actual transaction available for current thread}
     * （payment 域 {@code PaymentDeadLetterResilienceTest} 首次真实写事件时暴露，本域同步修复）。
     * </p>
     */
    @Bean
    public TransactionManager axonTransactionManager(PlatformTransactionManager platformTransactionManager) {
        return new SpringTransactionManager(platformTransactionManager);
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
