package com.titanium.product.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.aggregate.InsuranceProduct;
import com.titanium.product.domain.repository.ProductRepository;
import com.titanium.product.infrastructure.entity.ProductClauseRelDO;
import com.titanium.product.infrastructure.entity.ProductDO;
import com.titanium.product.infrastructure.mapper.ProductInfraMapper;
import com.titanium.product.infrastructure.repository.jpa.ProductClauseRelJpaRepository;
import com.titanium.product.infrastructure.repository.jpa.ProductJpaRepository;

/**
 * 产品仓储实现类 使用JPA实现产品领域数据访问
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository          productJpaRepository;
    private final ProductClauseRelJpaRepository productClauseRelJpaRepository;

    /**
     * 构造函数
     * 
     * @param productJpaRepository JPA仓储接口
     * @param productClauseRelJpaRepository 产品条款关联JPA仓储接口
     */
    public ProductRepositoryImpl(ProductJpaRepository productJpaRepository,
                                 ProductClauseRelJpaRepository productClauseRelJpaRepository) {
        this.productJpaRepository = productJpaRepository;
        this.productClauseRelJpaRepository = productClauseRelJpaRepository;
    }

    /**
     * 根据ID查询产品
     * 
     * @param productId 产品ID
     * @return 产品聚合根
     */
    @Override
    public InsuranceProduct findById(String productId) {
        return productJpaRepository.findById(productId).map(ProductInfraMapper.INSTANCE::toInsuranceProduct)
                .orElse(null);
    }

    /**
     * 根据条件查询产品列表
     * 
     * @param form 产品形态
     * @param type 险种类型
     * @param status 产品状态
     * @return 产品聚合根列表
     */
    @Override
    public List<InsuranceProduct> findByCondition(ProductEnum.ProductForm form, InsuranceType type,
                                                  ProductEnum.ProductStatus status) {
        // TODO: 实现带条件的查询，目前先返回所有产品
        return productJpaRepository.findAll().stream().map(ProductInfraMapper.INSTANCE::toInsuranceProduct)
                .collect(Collectors.toList());
    }

    /**
     * 根据原始产品ID查询历史版本
     * 
     * @param originalProductId 原始产品ID
     * @return 产品聚合根列表
     */
    @Override
    public List<InsuranceProduct> findHistoryByOriginalId(String originalProductId) {
        return productJpaRepository.findByOriginalProductId(originalProductId).stream()
                .map(ProductInfraMapper.INSTANCE::toInsuranceProduct).collect(Collectors.toList());
    }

    /**
     * 保存产品聚合根
     * 
     * @param product 产品聚合根
     */
    @Override
    public void save(InsuranceProduct product) {
        ProductDO productDO = ProductInfraMapper.INSTANCE.toProductDO(product);
        productDO.setTenantId("default"); // 从上下文获取实际的租户ID
        productDO.setCreatedAt(LocalDateTime.now());
        productDO.setCreatedBy("system");
        productDO.setUpdatedAt(LocalDateTime.now());
        productDO.setUpdatedBy("system");
        productJpaRepository.save(productDO);

        // 保存产品条款关联
        List<ProductClauseRelDO> clauseRelDOs = ProductInfraMapper.INSTANCE
                .toProductClauseRelDOs(product.getClauseRels());
        clauseRelDOs.forEach(clauseRelDO -> {
            clauseRelDO.setProductId(product.getProductId());
            clauseRelDO.setTenantId("default"); // 从上下文获取实际的租户ID
            clauseRelDO.setCreatedAt(LocalDateTime.now());
            clauseRelDO.setCreatedBy("system");
        });
        productClauseRelJpaRepository.saveAll(clauseRelDOs);
    }
}
