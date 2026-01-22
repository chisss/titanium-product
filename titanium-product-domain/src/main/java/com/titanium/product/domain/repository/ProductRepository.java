package com.titanium.product.domain.repository;

import java.util.List;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.aggregate.InsuranceProduct;

/**
 * 产品仓储接口 定义产品领域数据访问契约
 */
public interface ProductRepository {
    /**
     * 根据ID查询产品
     * 
     * @param productId 产品ID
     * @return 产品聚合根
     */
    InsuranceProduct findById(String productId);

    /**
     * 根据条件查询产品列表
     * 
     * @param form 产品形态
     * @param type 险种类型
     * @param status 产品状态
     * @return 产品聚合根列表
     */
    List<InsuranceProduct> findByCondition(ProductEnum.ProductForm form, InsuranceType type, ProductEnum.ProductStatus status);

    /**
     * 根据原始产品ID查询历史版本
     * 
     * @param originalProductId 原始产品ID
     * @return 产品聚合根列表
     */
    List<InsuranceProduct> findHistoryByOriginalId(String originalProductId);

    /**
     * 保存产品聚合根
     * 
     * @param product 产品聚合根
     */
    void save(InsuranceProduct product);
}
