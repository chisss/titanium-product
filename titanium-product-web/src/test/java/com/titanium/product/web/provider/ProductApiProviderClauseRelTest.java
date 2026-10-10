package com.titanium.product.web.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.titanium.common.context.RequestContextInterceptor;
import com.titanium.product.application.command.ProductCommandAppService;
import com.titanium.product.application.query.ProductQueryAppService;
import com.titanium.product.command.UpdateProductClauseRelCommand;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.web.catalog.InsuranceProductDefinitionCatalog;
import com.titanium.product.web.mapper.ProductWebMapper;

/**
 * 条款绑定「整体替换」api 入口测试（G01/AC-03 的远程通道）。
 *
 * <p>🔴 <b>路径断言的意义</b>：契约路径由 {@code ProductApi} 的 {@code @FeignClient(path)} 与
 * {@code @PutMapping} 唯一定义，Provider 通过 {@code implements} 继承、<b>不重复标注</b>。故本测试经真实
 * HTTP 路径发起请求——若 Provider 擅自改写路径（§3.4.10 禁止项），此处立即失败。且请求路径与 web 入口
 * 的 {@code /web/v1/products/{id}/clauses} <b>刻意不同前缀、同一末段</b>：两条入口面向两类消费者，
 * 共用同一末段命名而不共用同一路径。</p>
 *
 * <p>本类与 {@code ProductControllerClauseRelTest} 共同证明 AC-03 的收敛：两条入口产出<b>同一个</b>
 * {@link UpdateProductClauseRelCommand} 并调用<b>同一个</b> {@code ProductCommandAppService} 方法，
 * 两侧互不依赖、不得互调（本类零 Controller 引用，反之亦然）。</p>
 */
class ProductApiProviderClauseRelTest {

    private ProductCommandAppService commandAppService;
    private MockMvc                  mockMvc;

    @BeforeEach
    void setUp() {
        commandAppService = mock(ProductCommandAppService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductApiProvider(commandAppService,
                        mock(ProductQueryAppService.class),
                        Mappers.getMapper(ProductWebMapper.class),
                        mock(InsuranceProductDefinitionCatalog.class)))
                .addInterceptors(new RequestContextInterceptor())
                .build();
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id}/clauses 整体替换绑定，契约路径不被 Provider 篡改")
    void shouldReplaceClauseRelsThroughApiEntry() throws Exception {
        mockMvc.perform(put("/api/v1/products/product-1/clauses")
                        .header("X-Tenant-Id", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clauseRels":[
                                  {"clauseId":"C-MAIN","clauseVersion":"V1.0","isMainClause":true},
                                  {"clauseId":"C-RIDER","clauseVersion":"V1.0"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<UpdateProductClauseRelCommand> captor =
                ArgumentCaptor.forClass(UpdateProductClauseRelCommand.class);
        verify(commandAppService).updateProductClauseRel(captor.capture());
        UpdateProductClauseRelCommand command = captor.getValue();

        assertThat(command.productId()).isEqualTo("product-1");
        assertThat(command.tenantId()).isEqualTo("tenant-1");
        assertThat(command.newClauseRels())
                .extracting(ProductClauseRel::clauseId)
                .containsExactly("C-MAIN", "C-RIDER");
        assertThat(command.newClauseRels())
                .extracting(ProductClauseRel::isMainClause)
                .containsExactly(true, false);
    }
}
