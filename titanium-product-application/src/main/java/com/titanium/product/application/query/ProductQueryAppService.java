package com.titanium.product.application.query;

import java.util.List;

import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.query.query.FindCoverageSumInsuredByProductIdQuery;
import com.titanium.product.query.query.FindProductByIdQuery;
import com.titanium.product.query.query.FindProductClauseByProductIdQuery;
import com.titanium.product.query.query.GetTemplateByProductIdQuery;
import com.titanium.product.query.result.CoverageSumInsuredConfigQueryResult;
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
     * 在指定租户内查询产品详情，不存在时返回稳定的产品不存在错误。
     *
     * @param productId 产品ID
     * @param tenantId 租户ID
     * @return 产品查询结果
     */
    public ProductQueryResult queryProductDetail(String productId, String tenantId) {
        return requireProduct(productQueryService.findProductById(productId, tenantId));
    }

    /**
     * 在指定租户内按产品编码查询产品详情。
     *
     * @param productCode 产品编码
     * @param tenantId 租户ID
     * @return 产品查询结果
     */
    public ProductQueryResult queryProductByCode(String productCode, String tenantId) {
        return requireProduct(productQueryService.findProductByCode(productCode, tenantId));
    }

    /**
     * 在指定租户内查询同一产品编码下的全部版本（版本历史）。
     * <p>
     * 不复用 {@link #queryProductByCode(String, String)}：后者按编码取**单条**产品，而版本历史
     * 要的正是同编码下的**全部**版本行。此处直接委托读侧，不经查询总线（与分页查询同理，
     * 集合返回类型只走读模型服务）。
     * </p>
     *
     * @param productCode 产品编码（版本线归组键）
     * @param tenantId 租户ID
     * @return 该编码下的全部版本（新版在前），无版本时为空列表
     */
    public List<ProductQueryResult> queryProductVersions(String productCode, String tenantId) {
        return productQueryService.findProductVersions(productCode, tenantId);
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
     * @param tenantId 租户ID
     * @return 分页产品查询结果
     */
    public Page<ProductQueryResult> queryProductByCondition(String productName, ProductEnum.ProductForm form,
                                                            InsuranceProductType type, ProductEnum.ProductStatus status,
                                                            int pageNum, int pageSize, String tenantId) {
        // 直接委托读侧 ProductQueryService，不经 QueryGateway 派发：Axon 4.10 的 InstanceResponseType
        // 无法匹配返回 Page（继承 Iterable）的查询处理器，分页查询绕过查询总线直调读模型服务
        return productQueryService.findByCondition(productName, form, type, status, pageNum, pageSize, tenantId);
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
     * 查询产品的责任级保额档次配置
     * <p>
     * 供承保前置链路（G02 投保单录入）按产品取「每个责任可填什么保额」。走读模型
     * （{@code t_product_coverage_sum_insured_view}），最终一致；未配置或读模型未就绪时返回空列表。
     * </p>
     * <p>
     * 🔴 本入口只<b>提供数据</b>，不做「这条保额算选档还是自由填」的裁决——该裁决实现只有一份，
     * 在写侧值对象 {@code CoverageSumInsuredConfig#resolveSelection}。读侧复制一份必然漂移，
     * 出现「产品侧算命中档位、投保侧算自由填」的两侧结论不一致。
     * </p>
     *
     * @param productId 产品ID
     * @return 该产品全部责任的档次配置；未配置时为空列表
     */
    public List<CoverageSumInsuredConfigQueryResult> queryCoverageSumInsuredConfigs(String productId) {
        return queryGateway.query(new FindCoverageSumInsuredByProductIdQuery(productId),
                ResponseTypes.multipleInstancesOf(CoverageSumInsuredConfigQueryResult.class)).join();
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

    private ProductQueryResult requireProduct(ProductQueryResult result) {
        if (result == null) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_EXIST);
        }
        return result;
    }
}
