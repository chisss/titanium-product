package com.titanium.product.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.query.handler.query.ProductClauseQueryHandler;
import com.titanium.product.query.handler.query.ProductConditionQueryHandler;
import com.titanium.product.query.query.FindProductByConditionQuery;
import com.titanium.product.query.query.FindProductClauseByProductIdQuery;
import com.titanium.product.query.repository.ProductClauseRelViewRepository;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductClauseQueryService;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.query.service.impl.ProductClauseQueryServiceImpl;
import com.titanium.product.query.view.ProductClauseRelView;

/**
 * 产品查询处理器与查询服务测试
 * <p>
 * 覆盖此前缺失 QueryHandler 的两条查询：按条件分页查询、按产品ID查询绑定条款。 处理器验证委托正确，条款查询服务验证读模型 JSON
 * 反序列化（含键名映射、null 安全、空清单）。仅用 mockito-core 手动构造替身。
 * </p>
 */
class ProductQueryHandlerTest {

    @Test
    @DisplayName("条件查询处理器：按查询参数委托查询服务并回传分页结果")
    void conditionHandlerShouldDelegateToService() {
        ProductQueryService productQueryService = mock(ProductQueryService.class);
        ProductConditionQueryHandler handler = new ProductConditionQueryHandler(productQueryService);
        FindProductByConditionQuery query = new FindProductByConditionQuery(
                null, ProductEnum.ProductForm.GROUP, InsuranceProductType.TERM_LIFE,
                ProductEnum.ProductStatus.EFFECTIVE, 0, 10);
        Page<ProductQueryResult> expected = new PageImpl<>(List.of(new ProductQueryResult()));
        when(productQueryService.findByCondition(null, ProductEnum.ProductForm.GROUP, InsuranceProductType.TERM_LIFE,
                ProductEnum.ProductStatus.EFFECTIVE, 0, 10)).thenReturn(expected);

        Page<ProductQueryResult> actual = handler.handle(query);

        assertEquals(1, actual.getTotalElements());
        verify(productQueryService).findByCondition(null, ProductEnum.ProductForm.GROUP,
                InsuranceProductType.TERM_LIFE, ProductEnum.ProductStatus.EFFECTIVE, 0, 10);
    }

    @Test
    @DisplayName("条款查询处理器：按产品ID委托查询服务")
    void clauseHandlerShouldDelegateToService() {
        ProductClauseQueryService productClauseQueryService = mock(ProductClauseQueryService.class);
        ProductClauseQueryHandler handler = new ProductClauseQueryHandler(productClauseQueryService);
        FindProductClauseByProductIdQuery query = new FindProductClauseByProductIdQuery("PROD_001");
        when(productClauseQueryService.findByProductId("PROD_001")).thenReturn(List.of());

        handler.handle(query);

        verify(productClauseQueryService).findByProductId("PROD_001");
    }

    @Test
    @DisplayName("条款查询服务：读模型 JSON 反序列化，isMainClause 正确映射为 mainClause")
    void clauseServiceShouldParseJsonAndMapKeys() {
        ProductClauseRelViewRepository repository = mockRepositoryWith("PROD_001",
                JSON.toJSONString(List.of(
                        new ProductClauseRel("CLAUSE_A", "1.0", true),
                        new ProductClauseRel("CLAUSE_B", "2.0", false))));
        ProductClauseQueryService service = new ProductClauseQueryServiceImpl(repository);

        List<ProductClauseQueryResult> results = service.findByProductId("PROD_001");

        assertEquals(2, results.size());
        ProductClauseQueryResult main = results.get(0);
        assertEquals("CLAUSE_A", main.getClauseId());
        assertEquals("1.0", main.getClauseVersion());
        assertEquals(Boolean.TRUE, main.getMainClause());
        assertNotNull(main.getBindTime());
        assertEquals(Boolean.FALSE, results.get(1).getMainClause());
    }

    @Test
    @DisplayName("条款查询服务：读模型不存在时返回空列表")
    void clauseServiceShouldReturnEmptyWhenViewMissing() {
        ProductClauseRelViewRepository repository = mock(ProductClauseRelViewRepository.class);
        when(repository.findById(any())).thenReturn(Optional.empty());
        ProductClauseQueryService service = new ProductClauseQueryServiceImpl(repository);

        assertTrue(service.findByProductId("PROD_404").isEmpty());
    }

    @Test
    @DisplayName("条款查询服务：JSON 为空时返回空列表")
    void clauseServiceShouldReturnEmptyWhenJsonNull() {
        ProductClauseRelViewRepository repository = mockRepositoryWith("PROD_002", null);
        ProductClauseQueryService service = new ProductClauseQueryServiceImpl(repository);

        assertTrue(service.findByProductId("PROD_002").isEmpty());
    }

    private ProductClauseRelViewRepository mockRepositoryWith(String productId, String clauseRelsJson) {
        ProductClauseRelViewRepository repository = mock(ProductClauseRelViewRepository.class);
        ProductClauseRelView view = new ProductClauseRelView();
        view.setProductId(productId);
        view.setClauseRelsJson(clauseRelsJson);
        when(repository.findById(eq(productId))).thenReturn(Optional.of(view));
        return repository;
    }
}
