package com.titanium.product.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.product.query.repository.ProductTemplateViewRepository;
import com.titanium.product.query.repository.ProductViewRepository;
import com.titanium.product.query.service.impl.ProductTemplateQueryServiceImpl;
import com.titanium.product.query.view.ProductTemplateView;

/**
 * 产品模板读模型历史 JSON 兼容测试。
 */
class ProductTemplateQueryServiceImplTest {

    private static final String TEMPLATE_ID = "TPL-001";
    private static final String TENANT_ID = "tenant-1";

    @Test
    @DisplayName("旧数据阶段字符串数组应转换为可展示的阶段对象")
    void shouldReadLegacyStageCodes() {
        ProductTemplateView view = new ProductTemplateView();
        view.setTemplateId(TEMPLATE_ID);
        view.setTenantId(TENANT_ID);
        view.setPolicyStagesJson("[\"APPLICATION\",\"UNDERWRITING\"]");

        ProductTemplateQueryServiceImpl service = serviceReturning(view);

        var result = service.getTemplateById(TEMPLATE_ID, TENANT_ID);

        assertNotNull(result);
        assertEquals(2, result.getPolicyStages().size());
        assertEquals("APPLICATION", result.getPolicyStages().get(0).stageCode());
        assertEquals("UNDERWRITING", result.getPolicyStages().get(1).stageName());
    }

    @Test
    @DisplayName("旧版出单流程对象中的 steps 应转换为阶段对象")
    void shouldReadLegacyIssuanceProcessObject() {
        ProductTemplateView view = new ProductTemplateView();
        view.setTemplateId(TEMPLATE_ID);
        view.setTenantId(TENANT_ID);
        view.setPolicyStagesJson("""
                {"issuanceMode":"TWO_STEP","steps":["APPLICATION_SUBMIT","POLICY_ISSUE"]}
                """);

        ProductTemplateQueryServiceImpl service = serviceReturning(view);

        var result = service.getTemplateById(TEMPLATE_ID, TENANT_ID);

        assertNotNull(result);
        assertEquals(2, result.getPolicyStages().size());
        assertEquals("APPLICATION_SUBMIT", result.getPolicyStages().get(0).stageCode());
        assertEquals("POLICY_ISSUE", result.getPolicyStages().get(1).stageCode());
    }

    @Test
    @DisplayName("损坏的阶段 JSON 不应导致模板详情接口失败")
    void shouldIgnoreMalformedStageJson() {
        ProductTemplateView view = new ProductTemplateView();
        view.setTemplateId(TEMPLATE_ID);
        view.setTenantId(TENANT_ID);
        view.setPolicyStagesJson("not-json");

        ProductTemplateQueryServiceImpl service = serviceReturning(view);

        var result = service.getTemplateById(TEMPLATE_ID, TENANT_ID);

        assertNotNull(result);
        assertEquals(List.of(), result.getPolicyStages());
        assertNull(result.getBillingConfig());
    }

    @Test
    @DisplayName("按险种查询（三级码）：同名可对映码按「权威列 ∪ 同名遗留码」双列查询，对外契约返回三级码（G10/g10-04）")
    void shouldQueryBothColumnsForAuthoritativeType() {
        ProductTemplateViewRepository templateRepository = mock(ProductTemplateViewRepository.class);
        ProductTemplateView view = new ProductTemplateView();
        view.setTemplateId(TEMPLATE_ID);
        view.setInsuranceType(InsuranceProductType.MEDICAL);
        when(templateRepository.findByAuthoritativeOrLegacyType(InsuranceProductType.MEDICAL, InsuranceType.MEDICAL,
                TENANT_ID)).thenReturn(List.of(view));

        ProductTemplateQueryServiceImpl service =
                new ProductTemplateQueryServiceImpl(templateRepository, mock(ProductViewRepository.class));
        var result = service.getTemplatesByInsuranceType(InsuranceProductType.MEDICAL, TENANT_ID);

        assertEquals(1, result.size());
        assertEquals(InsuranceProductType.MEDICAL, result.get(0).getInsuranceType(), "对外契约已切权威三级码");
        verify(templateRepository).findByAuthoritativeOrLegacyType(InsuranceProductType.MEDICAL, InsuranceType.MEDICAL,
                TENANT_ID);
    }

    @Test
    @DisplayName("按险种查询（三级码）：一对多成员按 legacyOf 反向码回落查询——确定性优先级，不猜值")
    void shouldQueryWithLegacyFallbackForOneToManyMember() {
        ProductTemplateViewRepository templateRepository = mock(ProductTemplateViewRepository.class);
        when(templateRepository.findByAuthoritativeOrLegacyType(InsuranceProductType.TERM_LIFE, InsuranceType.LIFE,
                TENANT_ID)).thenReturn(List.of(new ProductTemplateView()));

        ProductTemplateQueryServiceImpl service =
                new ProductTemplateQueryServiceImpl(templateRepository, mock(ProductViewRepository.class));
        var result = service.getTemplatesByInsuranceType(InsuranceProductType.TERM_LIFE, TENANT_ID);

        assertEquals(1, result.size());
        verify(templateRepository).findByAuthoritativeOrLegacyType(InsuranceProductType.TERM_LIFE, InsuranceType.LIFE,
                TENANT_ID);
    }

    @Test
    @DisplayName("按险种查询（三级码）：三级独有码无遗留对映，只查权威列（legacy 传 null）——不猜值")
    void shouldQueryAuthoritativeOnlyForThreeLevelOnlyType() {
        ProductTemplateViewRepository templateRepository = mock(ProductTemplateViewRepository.class);
        when(templateRepository.findByAuthoritativeOrLegacyType(InsuranceProductType.DISABILITY_INCOME, null,
                TENANT_ID)).thenReturn(List.of(new ProductTemplateView()));

        ProductTemplateQueryServiceImpl service =
                new ProductTemplateQueryServiceImpl(templateRepository, mock(ProductViewRepository.class));
        var result = service.getTemplatesByInsuranceType(InsuranceProductType.DISABILITY_INCOME, TENANT_ID);

        assertEquals(1, result.size());
        verify(templateRepository).findByAuthoritativeOrLegacyType(InsuranceProductType.DISABILITY_INCOME, null,
                TENANT_ID);
    }

    @Test
    @DisplayName("存量回落：权威列为空的迁移前行，遗留码唯一可解析时回读三级码（resolve 不猜值）")
    void shouldFallbackToResolvableLegacyTypeWhenAuthoritativeNull() {
        ProductTemplateView view = new ProductTemplateView();
        view.setTemplateId(TEMPLATE_ID);
        view.setTenantId(TENANT_ID);
        view.setLegacyInsuranceType(InsuranceType.CAR);

        ProductTemplateQueryServiceImpl service = serviceReturning(view);
        var result = service.getTemplateById(TEMPLATE_ID, TENANT_ID);

        assertNotNull(result);
        assertEquals(InsuranceProductType.AUTO, result.getInsuranceType(), "CAR 唯一可解析为 AUTO");
    }

    @Test
    @DisplayName("存量回落：一对多遗留值不得猜值——权威列空且遗留不可唯一解析时三级码为空")
    void shouldNotGuessForOneToManyLegacyWhenAuthoritativeNull() {
        ProductTemplateView view = new ProductTemplateView();
        view.setTemplateId(TEMPLATE_ID);
        view.setTenantId(TENANT_ID);
        view.setLegacyInsuranceType(InsuranceType.LIFE);

        ProductTemplateQueryServiceImpl service = serviceReturning(view);
        var result = service.getTemplateById(TEMPLATE_ID, TENANT_ID);

        assertNotNull(result);
        assertNull(result.getInsuranceType(), "一对多不得猜值");
    }

    private ProductTemplateQueryServiceImpl serviceReturning(ProductTemplateView view) {
        ProductTemplateViewRepository templateRepository = mock(ProductTemplateViewRepository.class);
        when(templateRepository.findByTemplateIdAndTenantId(TEMPLATE_ID, TENANT_ID))
                .thenReturn(Optional.of(view));
        return new ProductTemplateQueryServiceImpl(templateRepository, mock(ProductViewRepository.class));
    }
}
