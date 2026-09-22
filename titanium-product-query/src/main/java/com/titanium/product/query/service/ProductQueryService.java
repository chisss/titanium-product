package com.titanium.product.query.service;

import java.util.List;

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
     * 根据产品ID和租户ID查询产品详情。
     *
     * @param productId 产品ID
     * @param tenantId 租户ID
     * @return 产品详情查询结果，不存在时返回 null
     */
    ProductQueryResult findProductById(String productId, String tenantId);

    /**
     * 根据产品编码和租户ID查询产品详情。
     * <p>
     * 同一编码下可能存在多个版本（修订继承 productCode），此时返回**最新创建的一版**；
     * 要取全部版本请用 {@link #findProductVersions(String, String)}。
     * </p>
     *
     * @param productCode 产品编码
     * @param tenantId 租户ID
     * @return 产品详情查询结果，不存在时返回 null
     */
    ProductQueryResult findProductByCode(String productCode, String tenantId);

    /**
     * 查询同一产品编码下的**全部版本**（版本历史）。
     * <p>
     * 产品是版本化实体：修订不改写当前生效版本，而是以父版本指针（{@code originalProductId}）
     * 派生一个独立聚合的新版本产出，各版本的 {@code productCode} 相同 ⇒ 版本线即「同编码的全部读模型行」。
     * 按业务创建时间倒序返回（新版在前）。
     * </p>
     *
     * @param productCode 产品编码（版本线的归组键）
     * @param tenantId 租户ID
     * @return 该编码下的全部版本，无版本时返回空列表
     */
    List<ProductQueryResult> findProductVersions(String productCode, String tenantId);

    /**
     * 按条件分页查询产品列表（产品名称/形态/险种/状态任意组合，均可为空表示不限）
     *
     * @param productName 产品名称（模糊匹配），可为空
     * @param form        产品形态，可为空
     * @param type        险种类型，可为空
     * @param status      产品状态，可为空
     * @param pageNum     页码（从 0 开始）
     * @param pageSize    每页条数
     * @param tenantId    租户ID
     * @return 分页产品查询结果
     */
    Page<ProductQueryResult> findByCondition(String productName, ProductEnum.ProductForm form,
                                             InsuranceProductType type, ProductEnum.ProductStatus status,
                                             int pageNum, int pageSize, String tenantId);
}
