package com.titanium.product.infrastructure.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.infrastructure.entity.ProductClauseRelEntity;

/**
 * 产品-条款关联JPA仓储接口
 * 定义产品-条款关联数据库操作方法
 */
public interface ProductClauseRelJpaRepository extends JpaRepository<ProductClauseRelEntity, Long> {
    /**
     * 根据产品ID查询关联的条款
     * @param productId 产品ID
     * @return 产品-条款关联数据库实体列表
     */
    List<ProductClauseRelEntity> findByProductId(String productId);

    /**
     * 根据产品ID删除关联的条款（条款关系更新时先清旧关联）
     * @param productId 产品ID
     */
    @Modifying
    @Transactional
    void deleteByProductId(String productId);
}
