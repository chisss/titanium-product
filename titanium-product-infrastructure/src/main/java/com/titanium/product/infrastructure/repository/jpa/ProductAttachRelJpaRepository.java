package com.titanium.product.infrastructure.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.entity.ProductAttachRelEntity;

/**
 * 产品附加险关联JPA仓储接口
 */
public interface ProductAttachRelJpaRepository extends JpaRepository<ProductAttachRelEntity, Long> {

    /**
     * 根据主产品ID查询附加险关联
     */
    List<ProductAttachRelEntity> findByMainProductId(String mainProductId);

    /**
     * 根据主产品ID删除附加险关联
     */
    void deleteByMainProductId(String mainProductId);
}
