package com.titanium.product.infrastructure.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.entity.ProductSalesChannelEntity;

/**
 * 产品销售渠道JPA仓储接口
 */
public interface ProductSalesChannelJpaRepository extends JpaRepository<ProductSalesChannelEntity, Long> {

    /**
     * 根据产品ID查询销售渠道
     */
    List<ProductSalesChannelEntity> findByProductId(String productId);

    /**
     * 根据产品ID删除销售渠道
     */
    void deleteByProductId(String productId);
}
