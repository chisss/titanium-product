package com.titanium.product.query.service;

import org.springframework.data.domain.Page;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.query.result.ProductQueryResult;

/**
 * 产品查询服务（CQRS 读侧）
 * <p>
 * 查询由事件投影维护的读模型表 {@code t_product_view}，实现真正的读写分离。 复杂查询逻辑内聚于本服务实现，应用层读入口仅表达「要查什么」。
 * </p>
 */
public interface ProductQueryService {

    /**
     * 根据产品ID查询产品详情
     *
     * @param productId 产品ID
     * @return 产品详情查询结果，不存在时返回 null
     */
    ProductQueryResult findProductById(String productId);

    /**
     * 按条件分页查询产品列表（产品名称/形态/险种/状态任意组合，均可为空表示不限）
     *
     * @param productName 产品名称（模糊匹配），可为空
     * @param form        产品形态，可为空
     * @param type        险种类型，可为空
     * @param status      产品状态，可为空
     * @param pageNum     页码（从 0 开始）
     * @param pageSize    每页条数
     * @return 分页产品查询结果
     */
    Page<ProductQueryResult> findByCondition(String productName, ProductEnum.ProductForm form,
                                             InsuranceProductType type, ProductEnum.ProductStatus status,
                                             int pageNum, int pageSize);
}
