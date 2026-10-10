package com.titanium.product.web.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

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
import com.titanium.product.command.UpdateCoverageSumInsuredConfigCommand;
import com.titanium.product.query.result.CoverageSumInsuredConfigQueryResult;
import com.titanium.product.query.result.CoverageSumInsuredTierResult;
import com.titanium.product.valueobject.config.CoverageSumInsuredConfig;
import com.titanium.product.web.catalog.InsuranceProductDefinitionCatalog;
import com.titanium.product.web.mapper.ProductWebMapper;

/**
 * 责任级保额档次配置 api 入口测试（G01/AC-04 的远程通道）。
 *
 * <p>🔴 <b>路径断言的意义</b>：契约路径由 {@code ProductApi} 的 {@code @FeignClient(path)} 与
 * {@code @PutMapping}/{@code @GetMapping} 唯一定义，Provider 通过 {@code implements} 继承、
 * <b>不重复标注</b>。故本测试经真实 HTTP 路径发起请求——若 Provider 擅自改写路径（§3.4.10 禁止项），
 * 此处立即失败。且请求路径与 web 入口 <b>刻意不同前缀、同一末段</b>：两条入口面向两类消费者，
 * 共用同一末段命名而不共用同一路径。</p>
 *
 * <p>本类与 {@code ProductCoverageSumInsuredControllerTest} 共同证明 AC-04 的收敛：两条入口产出
 * <b>同一个</b> {@link UpdateCoverageSumInsuredConfigCommand} 并调用<b>同一个</b>
 * {@code ProductCommandAppService} 方法，两侧互不依赖、不得互调（本类零 Controller 引用，反之亦然）。</p>
 *
 * <p>🔴 与 web 通道的<b>唯一形式差异</b>在本测试中被显式钉住：api 层自包含，其档次结构是内嵌 record
 * {@code UpdateCoverageSumInsuredConfigRequest.CoverageSumInsuredConfigItem}，与 web 层 DTO 是两套独立契约
 * （§3.4.11 ①）——两条通道各自翻译、落到领域后必须是<b>同一个值对象</b>形态。</p>
 */
class ProductApiProviderCoverageSumInsuredTest {

    private ProductCommandAppService commandAppService;
    private ProductQueryAppService   queryAppService;
    private MockMvc                  mockMvc;

    @BeforeEach
    void setUp() {
        commandAppService = mock(ProductCommandAppService.class);
        queryAppService = mock(ProductQueryAppService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductApiProvider(commandAppService,
                        queryAppService,
                        Mappers.getMapper(ProductWebMapper.class),
                        mock(InsuranceProductDefinitionCatalog.class)))
                .addInterceptors(new RequestContextInterceptor())
                .build();
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id}/coverage-sum-insured-configs 整体替换档次，契约路径不被 Provider 篡改")
    void shouldReplaceCoverageSumInsuredThroughApiEntry() throws Exception {
        mockMvc.perform(put("/api/v1/products/product-1/coverage-sum-insured-configs")
                        .header("X-Tenant-Id", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"coverageSumInsuredConfigs":[
                                  {"coverageCode":"DEATH",
                                   "tiers":[{"tierCode":"D1","sumInsured":500000,"allowedTerms":[10],"sortNo":1}],
                                   "minSumInsured":500000,"maxSumInsured":500000,
                                   "allowCustomAmount":false,"defaultTierCode":"D1"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<UpdateCoverageSumInsuredConfigCommand> captor =
                ArgumentCaptor.forClass(UpdateCoverageSumInsuredConfigCommand.class);
        verify(commandAppService).updateCoverageSumInsuredConfig(captor.capture());
        UpdateCoverageSumInsuredConfigCommand command = captor.getValue();

        assertThat(command.productId()).isEqualTo("product-1");
        assertThat(command.tenantId()).isEqualTo("tenant-1");
        assertThat(command.coverageSumInsuredConfigs()).hasSize(1);
        CoverageSumInsuredConfig config = command.coverageSumInsuredConfigs().get(0);
        assertThat(config.coverageCode()).isEqualTo("DEATH");
        assertThat(config.allowCustomAmount()).isFalse();
        assertThat(config.tiers()).extracting(tier -> tier.tierCode()).containsExactly("D1");
        assertThat(config.tiers().get(0).allowedTerms()).containsExactly(10);
    }

    @Test
    @DisplayName("GET /api/v1/products/{id}/coverage-sum-insured-configs 返回响应契约，供出单链路取档次")
    void shouldQueryCoverageSumInsuredThroughApiEntry() throws Exception {
        when(queryAppService.queryCoverageSumInsuredConfigs("product-1")).thenReturn(List.of(queryResult()));

        mockMvc.perform(get("/api/v1/products/product-1/coverage-sum-insured-configs")
                        .header("X-Tenant-Id", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].coverageCode").value("HOSPITAL"))
                // 🔴 序号与档位保额必须成对出现：只断言档位数会漏掉「顺序错位」这类静默错（档位顺序
                // 参与「选档」的语义，错位会让投保侧选到另一个金额）
                .andExpect(jsonPath("$.data[0].tiers[0].tierCode").value("T1"))
                .andExpect(jsonPath("$.data[0].tiers[0].sumInsured").value(100000))
                .andExpect(jsonPath("$.data[0].tiers[1].tierCode").value("T2"))
                .andExpect(jsonPath("$.data[0].tiers[1].sumInsured").value(300000));
    }

    private static CoverageSumInsuredConfigQueryResult queryResult() {
        CoverageSumInsuredTierResult t1 = new CoverageSumInsuredTierResult();
        t1.setTierCode("T1");
        t1.setSumInsured(new BigDecimal("100000"));
        t1.setAllowedTerms(List.of(1));
        t1.setSortNo(1);
        CoverageSumInsuredTierResult t2 = new CoverageSumInsuredTierResult();
        t2.setTierCode("T2");
        t2.setSumInsured(new BigDecimal("300000"));
        t2.setAllowedTerms(List.of(1, 5));
        t2.setSortNo(2);

        CoverageSumInsuredConfigQueryResult result = new CoverageSumInsuredConfigQueryResult();
        result.setCoverageCode("HOSPITAL");
        result.setTiers(List.of(t1, t2));
        result.setMinSumInsured(new BigDecimal("100000"));
        result.setMaxSumInsured(new BigDecimal("300000"));
        result.setAllowCustomAmount(true);
        result.setDefaultTierCode("T2");
        return result;
    }
}
