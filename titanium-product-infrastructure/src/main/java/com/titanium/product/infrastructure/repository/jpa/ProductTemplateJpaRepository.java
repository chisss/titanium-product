package com.titanium.product.infrastructure.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.infrastructure.entity.ProductTemplateDO;

/**
 * 产品模板 JPA 仓储接口
 */
@Repository
public interface ProductTemplateJpaRepository extends JpaRepository<ProductTemplateDO, String> {

    Optional<ProductTemplateDO> findByProductIdAndTenantId(String productId, String tenantId);

    Optional<ProductTemplateDO> findByTemplateCodeAndTenantId(String templateCode, String tenantId);

    List<ProductTemplateDO> findByInsuranceTypeAndTenantId(InsuranceType insuranceType, String tenantId);

    List<ProductTemplateDO> findByTenantId(String tenantId);

    boolean existsByTemplateCodeAndTenantId(String templateCode, String tenantId);

    boolean existsByTemplateCode(String templateCode);
}
