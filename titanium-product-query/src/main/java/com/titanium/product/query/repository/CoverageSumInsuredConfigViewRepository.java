package com.titanium.product.query.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.titanium.product.query.view.CoverageSumInsuredConfigView;

/**
 * 责任级保额档次配置读模型仓储
 * <p>
 * CQRS 查询侧仓储，直接访问读模型表 {@code t_product_coverage_sum_insured_view}，与写侧仓储隔离。
 * </p>
 */
@Repository
public interface CoverageSumInsuredConfigViewRepository extends JpaRepository<CoverageSumInsuredConfigView, String> {
}
