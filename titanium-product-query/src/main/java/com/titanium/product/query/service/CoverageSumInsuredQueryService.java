package com.titanium.product.query.service;

import java.util.List;

import com.titanium.product.query.result.CoverageSumInsuredConfigQueryResult;

/**
 * 责任级保额档次配置查询服务（CQRS 读侧）
 * <p>
 * 查读模型 {@code t_product_coverage_sum_insured_view}，供承保前置链路（G02 投保单录入）
 * 按产品取「每个责任可填什么保额」。
 * </p>
 */
public interface CoverageSumInsuredQueryService {

    /**
     * 按产品ID查询全部责任的保额档次配置
     *
     * @param productId 产品ID
     * @return 责任配置清单；产品未配置或读模型未就绪时返回空列表（不返回 null）
     */
    List<CoverageSumInsuredConfigQueryResult> findByProductId(String productId);
}
