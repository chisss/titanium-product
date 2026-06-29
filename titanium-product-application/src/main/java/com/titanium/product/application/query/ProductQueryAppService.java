package com.titanium.product.application.query;

import java.util.List;

import org.axonframework.queryhandling.QueryGateway;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.query.FindProductByConditionQuery;
import com.titanium.product.domain.query.FindProductByIdQuery;
import com.titanium.product.domain.query.FindProductClauseByProductIdQuery;
import com.titanium.product.query.entity.ProductQueryResult;

/**
 * 产品查询应用服务 处理产品相关的查询操作
 */
@Service
@Transactional(readOnly = true)
public class ProductQueryAppService {
    private final QueryGateway queryGateway;

    /**
     * 构造函数
     *
     * @param queryGateway 查询网关
     */
    public ProductQueryAppService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    /**
     * 查询产品详情
     *
     * @param productId 产品ID
     * @return 产品查询结果
     */
    public ProductQueryResult queryProductDetail(String productId) {
        return queryGateway.query(new FindProductByIdQuery(productId), ProductQueryResult.class).join();
    }

    /**
     * 根据条件查询产品列表
     *
     * @param form 产品形态
     * @param type 险种类型
     * @param status 产品状态
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 产品查询结果列表
     */
    public Page<ProductQueryResult> queryProductByCondition(ProductEnum.ProductForm form, InsuranceType type,
                                                            ProductEnum.ProductStatus status, int pageNum,
                                                            int pageSize) {
        FindProductByConditionQuery query = new FindProductByConditionQuery(form, type, status, pageNum, pageSize);
        return queryGateway.query(query, Page.class).join();
    }

    /**
     * 查询产品绑定的条款
     *
     * @param productId 产品ID
     * @return 条款查询结果列表
     */
    public List<Object> queryProductClauses(String productId) {
        return queryGateway.query(new FindProductClauseByProductIdQuery(productId), List.class).join();
    }
}
