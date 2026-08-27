package com.titanium.product.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    private ProductTemplateQueryServiceImpl serviceReturning(ProductTemplateView view) {
        ProductTemplateViewRepository templateRepository = mock(ProductTemplateViewRepository.class);
        when(templateRepository.findByTemplateIdAndTenantId(TEMPLATE_ID, TENANT_ID))
                .thenReturn(Optional.of(view));
        return new ProductTemplateQueryServiceImpl(templateRepository, mock(ProductViewRepository.class));
    }
}
