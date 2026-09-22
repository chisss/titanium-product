package com.titanium.product.query.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.query.view.ProductView;

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
     * 按产品编码 + 租户ID查询**最新创建的一版**。
     * <p>
     * 🔴 本方法原为 {@code findByProductCodeAndTenantId} 返回 {@code Optional}，隐含「编码唯一」这一
     * 与实际不符的前提：{@code t_product_view.product_code} 建的是普通索引（非 UNIQUE），而修订
     * <b>继承同一 productCode</b>（{@code InsuranceProduct.handle(ReviseProductCommand)}）⇒ 产品修订出第二版后，
     * 该查询会抛 {@code IncorrectResultSizeDataAccessException}（同一编码命中多行）。
     * 现改为「取最新一版」：语义在编码重名时有确定解，且不改变调用方拿单个结果的契约形状。
     * </p>
     */
    Optional<ProductView> findFirstByProductCodeAndTenantIdOrderByCreatedAtDesc(String productCode, String tenantId);

    /**
     * 按产品编码 + 租户ID查询**全部版本**（版本历史用），按业务创建时间倒序（新版在前）。
     */
    List<ProductView> findAllByProductCodeAndTenantIdOrderByCreatedAtDesc(String productCode, String tenantId);

    /**
     * 按险种类型 + 租户ID分页查询
     */
    List<ProductView> findByInsuranceTypeAndTenantId(InsuranceProductType insuranceType, String tenantId,
                                                     Pageable pageable);

    /**
     * 按状态 + 租户ID分页查询
     */
    List<ProductView> findByStatusAndTenantId(ProductEnum.ProductStatus status, String tenantId, Pageable pageable);
}
