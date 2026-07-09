package com.titanium.product.query.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.query.view.ProductTemplateView;

/**
 * 产品模板读模型仓储
 * <p>
 * CQRS 查询侧仓储，直接访问读模型表 {@code t_product_template_view}，与写侧仓储隔离。 查询方法强制携带
 * {@code tenantId} 实现多租户数据隔离。
 * </p>
 */
@Repository
public interface ProductTemplateViewRepository
        extends JpaRepository<ProductTemplateView, String>, JpaSpecificationExecutor<ProductTemplateView> {

    /**
     * 按模板ID + 租户ID查询
     */
    Optional<ProductTemplateView> findByTemplateIdAndTenantId(String templateId, String tenantId);

    /**
     * 按产品ID + 租户ID查询
     */
    Optional<ProductTemplateView> findByProductIdAndTenantId(String productId, String tenantId);

    /**
     * 按模板编码 + 租户ID查询
     */
    Optional<ProductTemplateView> findByTemplateCodeAndTenantId(String templateCode, String tenantId);

    /**
     * 按险种类型 + 租户ID查询
     */
    List<ProductTemplateView> findByInsuranceTypeAndTenantId(InsuranceType insuranceType, String tenantId);
}
