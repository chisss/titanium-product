package com.titanium.product.infrastructure.repository.jpa;

import com.titanium.product.infrastructure.entity.ProductDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 产品JPA仓储接口
 * 定义产品数据库操作方法
 */
public interface ProductJpaRepository extends JpaRepository<ProductDO, String> {
    /**
     * 根据原始产品ID查询历史版本
     * @param originalProductId 原始产品ID
     * @return 产品数据库实体列表
     */
    List<ProductDO> findByOriginalProductId(String originalProductId);
}