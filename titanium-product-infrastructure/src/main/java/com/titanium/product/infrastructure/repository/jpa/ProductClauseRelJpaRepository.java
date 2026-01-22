package com.titanium.product.infrastructure.repository.jpa;

import com.titanium.product.infrastructure.entity.ProductClauseRelDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 产品-条款关联JPA仓储接口
 * 定义产品-条款关联数据库操作方法
 */
public interface ProductClauseRelJpaRepository extends JpaRepository<ProductClauseRelDO, Long> {
    /**
     * 根据产品ID查询关联的条款
     * @param productId 产品ID
     * @return 产品-条款关联数据库实体列表
     */
    List<ProductClauseRelDO> findByProductId(String productId);
}