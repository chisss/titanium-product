package com.titanium.product.query.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.query.entity.ProductView;

/**
 * 产品读模型仓储
 * <p>
 * CQRS 查询侧仓储，直接访问读模型表 {@code t_product_view}，与写侧仓储隔离。 查询方法强制携带
 * {@code tenantId} 实现多租户数据隔离。
 * </p>
 */
@Repository
public interface ProductViewRepository
        extends JpaRepository<ProductView, String>, JpaSpecificationExecutor<ProductView> {

    /**
     * 按产品ID + 租户ID查询
     */
    Optional<ProductView> findByProductIdAndTenantId(String productId, String tenantId);

    /**
     * 按产品编码 + 租户ID查询
     */
    Optional<ProductView> findByProductCodeAndTenantId(String productCode, String tenantId);

    /**
     * 按险种类型 + 租户ID分页查询
     */
    List<ProductView> findByInsuranceTypeAndTenantId(InsuranceType insuranceType, String tenantId, Pageable pageable);

    /**
     * 按状态 + 租户ID分页查询
     */
    List<ProductView> findByStatusAndTenantId(ProductEnum.ProductStatus status, String tenantId, Pageable pageable);
}
