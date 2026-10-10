package com.titanium.product.web.controller.product;

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
import com.titanium.product.web.mapper.ProductWebMapper;

/**
 * 责任级保额档次配置 web 入口测试（G01/AC-04 的后台/端上通道）。
 *
 * <p>覆盖一读一写两条通道：</p>
 * <ol>
 *   <li><b>写</b>：{@code PUT /web/v1/products/{id}/coverage-sum-insured-configs} 经 {@code ProductWebMapper}
 *       译成 {@link UpdateCoverageSumInsuredConfigCommand} 并收敛到 application 薄门面；</li>
 *   <li><b>读</b>：{@code GET} 同路径读读模型并映射为对外响应。</li>
 * </ol>
 *
 * <p>🔴 <b>本类刻意不测「非 DRAFT 状态被拒」与「责任编码不属于本产品被拒」</b>：前者由<b>聚合根</b>裁决、
 * 后者由<b>应用层跨域校验</b>裁决，web 层不复制这两条判断——若在此用桩应用服务断言「被拒」，
 * 断言的是桩而非真实规则（守卫会守在一个无人可达的路径上）。二者的权威用例分别在
 * {@code InsuranceProductTest} 与 {@code ProductCommandAppServiceTest}（后者含跨域桩）。</p>
 *
 * <p>🔴 <b>本类同样不测「金额落在哪一档」</b>：选档/自由填的裁决唯一实现在写侧值对象
 * {@code CoverageSumInsuredConfig#resolveSelection}，本层只搬运结构。若在此复制一份裁决断言，
 * 就制造了第二份口径——正是该值对象 javadoc 明确禁止的漂移源。</p>
 *
 * <p>🔴 <b>被测类是独立控制器而非 {@code ProductController}</b>：产品主控制器端点已达
 * {@code controllersMustNotBeOverloaded} 棘轮基线（19 个，只许下调），档次配置这块独立资源按
 * §3.4.3／§3.4.13 自行成类。<b>HTTP 路径逐字节不变</b>，故本类全部路径断言与拆分前完全一致——
 * 这也正是「拆控制器不得改契约」的判据。</p>
 */
class ProductCoverageSumInsuredControllerTest {

    private ProductCommandAppService commandAppService;
    private ProductQueryAppService   queryAppService;
    private MockMvc                  mockMvc;

    @BeforeEach
    void setUp() {
        commandAppService = mock(ProductCommandAppService.class);
        queryAppService = mock(ProductQueryAppService.class);
        ProductCoverageSumInsuredController controller = new ProductCoverageSumInsuredController(
                commandAppService,
                queryAppService,
                Mappers.getMapper(ProductWebMapper.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new RequestContextInterceptor())
                .build();
    }

    @Test
    @DisplayName("PUT /web/v1/products/{id}/coverage-sum-insured-configs 整体替换档次，租户取自请求头且档位逐层映射")
    void shouldReplaceCoverageSumInsuredThroughWebEntry() throws Exception {
        mockMvc.perform(put("/web/v1/products/product-1/coverage-sum-insured-configs")
                        .header("X-Tenant-Id", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"coverageSumInsuredConfigs":[
                                  {"coverageCode":"HOSPITAL",
                                   "tiers":[
                                     {"tierCode":"T1","sumInsured":100000,"allowedTerms":[1],"sortNo":1},
                                     {"tierCode":"T2","sumInsured":300000,"allowedTerms":[1,5],"sortNo":2}],
                                   "minSumInsured":100000,"maxSumInsured":300000,
                                   "allowCustomAmount":true,"defaultTierCode":"T2"}]}
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
        assertThat(config.coverageCode()).isEqualTo("HOSPITAL");
        assertThat(config.allowCustomAmount()).isTrue();
        assertThat(config.defaultTierCode()).isEqualTo("T2");
        assertThat(config.minSumInsured()).isEqualByComparingTo("100000");
        assertThat(config.maxSumInsured()).isEqualByComparingTo("300000");
        // 档位逐层映射：数量与档位码、保额、允许期限、排序号都不得丢项（少映射一层就会静默丢档）
        assertThat(config.tiers()).hasSize(2);
        assertThat(config.tiers()).extracting(tier -> tier.tierCode()).containsExactly("T1", "T2");
        assertThat(config.tiers()).extracting(tier -> tier.sumInsured())
                .containsExactly(new BigDecimal("100000"), new BigDecimal("300000"));
        assertThat(config.tiers().get(1).allowedTerms()).containsExactly(1, 5);
        assertThat(config.tiers().get(1).sortNo()).isEqualTo(2);
    }

    @Test
    @DisplayName("PUT 空清单表示清空（合法），不得被前置拦截")
    void shouldAcceptEmptyListAsClear() throws Exception {
        mockMvc.perform(put("/web/v1/products/product-1/coverage-sum-insured-configs")
                        .header("X-Tenant-Id", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverageSumInsuredConfigs\":[]}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateCoverageSumInsuredConfigCommand> captor =
                ArgumentCaptor.forClass(UpdateCoverageSumInsuredConfigCommand.class);
        verify(commandAppService).updateCoverageSumInsuredConfig(captor.capture());
        // 🔴 空清单必须原样到达门面（清空语义），且不得被映射成 null——null 在聚合根会被判为非法输入
        assertThat(captor.getValue().coverageSumInsuredConfigs()).isEmpty();
    }

    @Test
    @DisplayName("PUT 缺少 allowCustomAmount 时以 400 拒绝，不得静默兜底为 false")
    void shouldRejectMissingAllowCustomAmount() throws Exception {
        mockMvc.perform(put("/web/v1/products/product-1/coverage-sum-insured-configs")
                        .header("X-Tenant-Id", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"coverageSumInsuredConfigs":[
                                  {"coverageCode":"HOSPITAL","tiers":[],
                                   "minSumInsured":100000,"maxSumInsured":300000,
                                   "defaultTierCode":"T1"}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /web/v1/products/{id}/coverage-sum-insured-configs 读读模型并按响应契约映射")
    void shouldQueryCoverageSumInsuredThroughWebEntry() throws Exception {
        when(queryAppService.queryCoverageSumInsuredConfigs("product-1")).thenReturn(List.of(queryResult()));

        mockMvc.perform(get("/web/v1/products/product-1/coverage-sum-insured-configs")
                        .header("X-Tenant-Id", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].coverageCode").value("HOSPITAL"))
                .andExpect(jsonPath("$.data[0].allowCustomAmount").value(true))
                .andExpect(jsonPath("$.data[0].defaultTierCode").value("T2"))
                .andExpect(jsonPath("$.data[0].tiers[0].tierCode").value("T1"))
                .andExpect(jsonPath("$.data[0].tiers[1].sumInsured").value(300000))
                .andExpect(jsonPath("$.data[0].tiers[1].allowedTerms[1]").value(5));
    }

    @Test
    @DisplayName("GET 未配置时返回空列表而非 404（「尚未配置」是正常状态）")
    void shouldReturnEmptyListWhenNotConfigured() throws Exception {
        when(queryAppService.queryCoverageSumInsuredConfigs("product-1")).thenReturn(List.of());

        mockMvc.perform(get("/web/v1/products/product-1/coverage-sum-insured-configs")
                        .header("X-Tenant-Id", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
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
