package com.titanium.product.query.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
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
     * 按权威三级码或遗留扁平码 + 租户ID查询（G10/g10-02 P1 双写期过渡查询：新列命中迁移后行，旧列兜住历史行）。
     * <p>
     * 🔴 必须显式 JPQL：派生查询方法名中的 {@code Or}/{@code And} 优先级会把 tenantId 并入 Or 右侧，
     * 退化为 {@code insurance_type = ? OR (legacy_insurance_type = ? AND tenant_id = ?)}，造成跨租户行泄漏。
     * </p>
     */
    @Query("SELECT v FROM ProductTemplateView v WHERE v.tenantId = :tenantId"
            + " AND (v.insuranceType = :authoritative OR v.legacyInsuranceType = :legacy)")
    List<ProductTemplateView> findByAuthoritativeOrLegacyType(@Param("authoritative") InsuranceProductType authoritative,
            @Param("legacy") InsuranceType legacy, @Param("tenantId") String tenantId);
}
