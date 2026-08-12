package com.titanium.product.application.query;

import java.util.List;

import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.query.query.FindProductByIdQuery;
import com.titanium.product.query.query.FindProductClauseByProductIdQuery;
import com.titanium.product.query.query.GetTemplateByProductIdQuery;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.valueobject.LifeProductSpec;

/**
 * 产品查询应用服务 处理产品相关的查询操作
 */
@Service
@Transactional(readOnly = true)
public class ProductQueryAppService {
    private final QueryGateway queryGateway;
    private final ProductQueryService productQueryService;

    /**
     * 构造函数
     *
     * @param queryGateway        查询网关
     * @param productQueryService 产品读模型查询服务（分页查询直调，绕过查询总线）
     */
    public ProductQueryAppService(QueryGateway queryGateway, ProductQueryService productQueryService) {
        this.queryGateway = queryGateway;
        this.productQueryService = productQueryService;
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
     * @param productName 产品名称（模糊匹配，可为空）
     * @param form 产品形态（可为空）
     * @param type 险种类型（可为空）
     * @param status 产品状态（可为空）
     * @param pageNum 页码（从 0 开始）
     * @param pageSize 每页条数
     * @return 分页产品查询结果
     */
    public Page<ProductQueryResult> queryProductByCondition(String productName, ProductEnum.ProductForm form,
                                                            InsuranceProductType type, ProductEnum.ProductStatus status,
                                                            int pageNum, int pageSize) {
        // 直接委托读侧 ProductQueryService，不经 QueryGateway 派发：Axon 4.10 的 InstanceResponseType
        // 无法匹配返回 Page（继承 Iterable）的查询处理器，分页查询绕过查询总线直调读模型服务
        return productQueryService.findByCondition(productName, form, type, status, pageNum, pageSize);
    }

    /**
     * 查询产品绑定的条款
     *
     * @param productId 产品ID
     * @return 条款查询结果列表
     */
    public List<ProductClauseQueryResult> queryProductClauses(String productId) {
        return queryGateway.query(new FindProductClauseByProductIdQuery(productId),
                ResponseTypes.multipleInstancesOf(ProductClauseQueryResult.class)).join();
    }

    /**
     * 查询产品对应模板已配置的寿险产品规格。
     * <p>
     * 寿险规格作为产品模板（{@code ProductTemplate}）专属配置存储，读侧经产品ID定位其关联模板读模型，
     * 取出寿险规格值对象；模板不存在或未配置寿险规格时返回 {@code null}。
     * </p>
     *
     * @param productId 产品ID
     * @param tenantId 租户ID
     * @return 寿险产品规格，未配置时返回 null
     */
    public LifeProductSpec queryLifeProductSpec(String productId, String tenantId) {
        ProductTemplateQueryResult template = queryGateway.query(
                new GetTemplateByProductIdQuery(productId, tenantId),
                ResponseTypes.instanceOf(ProductTemplateQueryResult.class)).join();
        return template != null ? template.getLifeProductSpec() : null;
    }
}
