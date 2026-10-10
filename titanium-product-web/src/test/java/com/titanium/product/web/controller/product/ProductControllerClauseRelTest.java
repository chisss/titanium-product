package com.titanium.product.web.controller.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
 * 条款绑定「整体替换」web 入口测试（G01/AC-03 的后台/端上通道）。
 *
 * <p>🔴 <b>本类刻意不测「EFFECTIVE 状态被拒」</b>：该状态前置由<b>聚合根</b>裁决
 * （{@code InsuranceProduct#handle(UpdateProductClauseRelCommand)} 判 {@code DRAFT}），web 层不复制该判断
 * ——若在此用桩应用服务断言「被拒」，断言的是桩而非真实规则（守卫会守在一个无人可达的路径上）。
 * 状态前置的权威用例在 {@code InsuranceProductTest}（领域层），端到端正反成对由真机轮次覆盖。</p>
 */
class ProductControllerClauseRelTest {

    private ProductCommandAppService commandAppService;
    private MockMvc                  mockMvc;

    @BeforeEach
    void setUp() {
        commandAppService = mock(ProductCommandAppService.class);
        ProductController controller = new ProductController(commandAppService,
                mock(ProductQueryAppService.class),
                Mappers.getMapper(ProductWebMapper.class),
                mock(InsuranceProductDefinitionCatalog.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new RequestContextInterceptor())
                .build();
    }

    @Test
    @DisplayName("PUT /web/v1/products/{id}/clauses 整体替换绑定，租户取自请求头并收敛到同一门面")
    void shouldReplaceClauseRelsThroughWebEntry() throws Exception {
        mockMvc.perform(put("/web/v1/products/product-1/clauses")
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
        assertThat(command.newClauseRels()).hasSize(2);
        assertThat(command.newClauseRels())
                .extracting(ProductClauseRel::clauseId)
                .containsExactly("C-MAIN", "C-RIDER");
        assertThat(command.newClauseRels())
                .extracting(ProductClauseRel::isMainClause)
                .containsExactly(true, false);
    }

    @Test
    @DisplayName("🔴 空清单在前置被拒——不得落到聚合（绑定非空是产品聚合的硬不变量）")
    void shouldRejectEmptyClauseRelsBeforeReachingFacade() throws Exception {
        mockMvc.perform(put("/web/v1/products/product-1/clauses")
                        .header("X-Tenant-Id", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clauseRels\":[]}"))
                .andExpect(status().isBadRequest());

        verify(commandAppService, never()).updateProductClauseRel(any());
    }
}
