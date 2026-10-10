package com.titanium.product.infrastructure.pricing.repository.ncd;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.infrastructure.pricing.entity.ncd.NcdRuleSetDO;

/** NCD 规则集 JPA 仓储（G11/g11-03）。 */
public interface NcdRuleSetJpaRepository extends JpaRepository<NcdRuleSetDO, String> {

    /**
     * 按业务键（租户+产品+规则集编码+规则集版本）判断规则集是否已存在，
     * 与唯一键 {@code uk_product_ncd_rule_set_version} 对齐。
     */
    boolean existsByTenantIdAndProductIdAndRuleSetCodeAndRuleSetVersion(
            String tenantId, String productId, String ruleSetCode, String ruleSetVersion);

    /**
     * 按规则集ID + 租户 + 产品查询（写侧生命周期流转的加载入口）。
     */
    Optional<NcdRuleSetDO> findByRuleSetIdAndTenantIdAndProductId(
            String ruleSetId, String tenantId, String productId);

    /**
     * 查询业务时点适用的已发布规则集。
     * <p>
     * 同一租户+产品在任一时点至多一条（见 {@code NcdRuleSetRepository} 的形态说明）；若库中出现
     * 多条同时生效，Spring Data 会以 {@code IncorrectResultSizeDataAccessException} 显式失败——
     * 这是刻意的：静默取其一会让出单在不同请求间用上不同规则集，且无从察觉。
     * </p>
     */
    @Query("""
            select r from NcdRuleSetDO r
            where r.tenantId = :tenantId and r.productId = :productId
              and r.status = com.titanium.product.common.enums.ActuarialDefinitionStatus.PUBLISHED
              and r.effectiveFrom <= :businessTime
              and (r.effectiveTo is null or r.effectiveTo > :businessTime)
            """)
    Optional<NcdRuleSetDO> findPublished(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("businessTime") LocalDateTime businessTime);
}
