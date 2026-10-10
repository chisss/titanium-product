package com.titanium.product.query.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.query.mapper.CoverageSumInsuredQueryResultMapper;
import com.titanium.product.query.repository.CoverageSumInsuredConfigViewRepository;
import com.titanium.product.query.result.CoverageSumInsuredConfigQueryResult;
import com.titanium.product.query.service.CoverageSumInsuredQueryService;
import com.titanium.product.query.view.CoverageSumInsuredConfigView;

import lombok.RequiredArgsConstructor;

/**
 * 责任级保额档次配置查询服务实现（CQRS 读侧）
 * <p>
 * 读取读模型表 {@code t_product_coverage_sum_insured_view} 中以 JSON 存储的配置清单，
 * 反序列化为防腐条目结构后经 {@link CoverageSumInsuredQueryResultMapper} 声明式映射为查询结果 DTO。
 * </p>
 * <p>
 * 只读事务；「读模型未就绪」（产品在本能力上线前创建、尚未投影）与「确实未配置」都返回空列表——
 * 二者对消费方是同一个事实：<b>此刻拿不到档次约束</b>，须由消费方决定是放行还是阻断，
 * 读侧不替它下结论。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CoverageSumInsuredQueryServiceImpl implements CoverageSumInsuredQueryService {

    private final CoverageSumInsuredConfigViewRepository configViewRepository;
    private final CoverageSumInsuredQueryResultMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CoverageSumInsuredConfigQueryResult> findByProductId(String productId) {
        return configViewRepository.findById(productId)
                .map(this::toResults)
                .orElseGet(Collections::emptyList);
    }

    /**
     * 读模型 JSON 清单 → 查询结果 DTO 列表（防腐条目解析 + 声明式映射）
     */
    private List<CoverageSumInsuredConfigQueryResult> toResults(CoverageSumInsuredConfigView view) {
        if (view.getConfigsJson() == null || view.getConfigsJson().isBlank()) {
            return Collections.emptyList();
        }
        List<CoverageSumInsuredQueryResultMapper.ConfigItem> items =
                JSON.parseArray(view.getConfigsJson(), CoverageSumInsuredQueryResultMapper.ConfigItem.class);
        return items.stream().map(mapper::toQueryResult).toList();
    }
}
