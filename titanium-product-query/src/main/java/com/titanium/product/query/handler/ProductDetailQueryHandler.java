package com.titanium.product.query.handler;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import com.titanium.product.domain.aggregate.InsuranceProduct;
import com.titanium.product.domain.query.FindProductByIdQuery;
import com.titanium.product.domain.repository.ProductRepository;
import com.titanium.product.query.entity.ProductQueryResult;
import com.titanium.product.query.mapper.ProductQueryMapper;

/**
 * 产品详情查询处理器 处理根据ID查询产品详情的请求
 */
@Component
public class ProductDetailQueryHandler {

    private final ProductRepository  productRepository;
    private final ProductQueryMapper productQueryMapper;

    /**
     * 构造函数
     * 
     * @param productRepository 产品仓储
     * @param productQueryMapper 产品查询映射器
     */
    public ProductDetailQueryHandler(ProductRepository productRepository, ProductQueryMapper productQueryMapper) {
        this.productRepository = productRepository;
        this.productQueryMapper = productQueryMapper;
    }

    /**
     * 处理查询请求
     * 
     * @param query 查询请求
     * @return 产品详情查询结果
     */
    @QueryHandler
    public ProductQueryResult handle(FindProductByIdQuery query) {
        // 根据ID查询产品聚合根
        InsuranceProduct insuranceProduct = productRepository.findById(query.productId());

        // 使用MapStruct转换为查询结果
        return productQueryMapper.toProductQueryResult(insuranceProduct);
    }
}
