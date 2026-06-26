package com.titanium.product.infrastructure.repository.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.infrastructure.entity.ProductEntity;

/**
 * 产品JPA仓储接口
 * 定义产品数据库操作方法
 */
public interface ProductJpaRepository extends JpaRepository<ProductEntity, String> {

    /**
     * 根据原始产品ID查询历史版本
     */
    List<ProductEntity> findByOriginalProductId(String originalProductId);

    /**
     * 根据产品代码和租户ID查询
     */
    ProductEntity findByProductCodeAndTenantId(String productCode, String tenantId);

    /**
     * 根据条件分页查询产品
     */
    @Query("SELECT p FROM ProductEntity p WHERE " +
            "(:form IS NULL OR p.form = :form) AND " +
            "(:insuranceType IS NULL OR p.insuranceType = :insuranceType) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "p.tenantId = :tenantId")
    Page<ProductEntity> findByCondition(
            @Param("form") String form,
            @Param("insuranceType") InsuranceType insuranceType,
            @Param("status") ProductEnum.ProductStatus status,
            @Param("tenantId") String tenantId,
            Pageable pageable);

    /**
     * 查询指定状态和险种类型的产品列表
     */
    List<ProductEntity> findByStatusAndInsuranceTypeAndTenantId(
            ProductEnum.ProductStatus status, InsuranceType insuranceType, String tenantId);

    /**
     * 查询指定租户下的所有生效产品
     */
    List<ProductEntity> findByStatusAndTenantId(ProductEnum.ProductStatus status, String tenantId);
}
