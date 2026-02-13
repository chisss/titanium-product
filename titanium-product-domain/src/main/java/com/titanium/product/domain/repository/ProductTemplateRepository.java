package com.titanium.product.domain.repository;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.domain.aggregate.ProductTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 产品模板仓储接口
 */
public interface ProductTemplateRepository {

    /**
     * 根据模板ID查询
     */
    Optional<ProductTemplate> findById(String templateId);

    /**
     * 根据产品ID查询
     */
    Optional<ProductTemplate> findByProductId(String productId);

    /**
     * 根据模板编码查询
     */
    Optional<ProductTemplate> findByTemplateCode(String templateCode);

    /**
     * 根据险种类型查询列表
     */
    List<ProductTemplate> findByInsuranceType(InsuranceType insuranceType);

    /**
     * 根据租户ID查询所有模板
     */
    List<ProductTemplate> findByTenantId(String tenantId);

    /**
     * 检查模板编码是否存在
     */
    boolean existsByTemplateCode(String templateCode);
}
