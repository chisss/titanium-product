package com.titanium.product.query.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.titanium.product.query.view.ProductClauseRelView;

/**
 * 产品条款关联读模型仓储
 * <p>
 * CQRS 查询侧仓储，直接访问读模型表 {@code t_product_clause_rel_view}，与写侧仓储隔离。
 * </p>
 */
@Repository
public interface ProductClauseRelViewRepository extends JpaRepository<ProductClauseRelView, String> {
}
