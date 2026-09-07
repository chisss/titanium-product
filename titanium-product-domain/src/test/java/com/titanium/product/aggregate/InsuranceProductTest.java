package com.titanium.product.aggregate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.metadata.exception.CommandValidationException;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.service.ProductDomainService;
import com.titanium.product.valueobject.config.InsureCondition;
import com.titanium.product.valueobject.pricing.pricing.PricingBasicRule;
import com.titanium.product.valueobject.pricing.pricing.PricingFactor;

/**
 * 产品聚合根测试（创建命令校验）。
 *
 * <p>覆盖：合法命令创建成功 / 产品级 factors 已废弃（G7 收口，配置即拒绝并引导去定价计划动态因子）。
 */
class InsuranceProductTest {

    private FixtureConfiguration<InsuranceProduct> fixture;

    private static final String PRODUCT_ID = "PROD_001";
    private static final String TEMPLATE_ID = "TPL_001";
    private static final String PRODUCT_CODE = "TEST-PROD";
    private static final String TENANT_ID = "TENANT_001";
    private static final String CREATED_BY = "tester";
    private static final String MAIN_CLAUSE_ID = "CLAUSE_001";

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(InsuranceProduct.class);
        fixture.registerInjectableResource(new ProductDomainService());
    }

    @Test
    @DisplayName("创建命令合法且无产品级 factors 时创建成功")
    void shouldCreateProductWithoutFactors() {
        fixture.givenNoPriorActivity()
                .when(baseCommand())
                .expectSuccessfulHandlerExecution();
    }

    @Test
    @DisplayName("产品级 factors 已废弃：配置非空即拒绝并引导到定价计划动态因子")
    void shouldRejectCreateWhenProductFactorsConfigured() {
        PricingFactor ncdFactor = new PricingFactor("NCD", "无赔款折扣系数",
                Map.of("0次", new BigDecimal("0.6"), "3次", BigDecimal.ONE));
        CreateProductCommand command = new CreateProductCommand(
                PRODUCT_ID, TEMPLATE_ID, PRODUCT_CODE, "测试产品", null,
                ProductEnum.ProductForm.INDIVIDUAL, InsuranceProductType.AUTO, null, null, null, null,
                new InsureCondition(18, 65, null, null, null, null, null, null, null, null, null, null, null, null),
                null, null,
                new PricingBasicRule(ProductEnum.PricingType.FACTOR, null, List.of(ncdFactor),
                        null, null),
                List.of(MAIN_CLAUSE_ID), Map.of(), MAIN_CLAUSE_ID,
                null, null, null, null, null,
                TENANT_ID, PricingMode.RATE_TABLE, null, null, null, CREATED_BY, null);

        fixture.givenNoPriorActivity()
                .when(command)
                .expectException(CommandValidationException.class)
                .expectExceptionMessage(Matchers.containsString(
                        "产品级 factors 系数不参与试算，请改为在定价计划中配置动态因子"));
    }

    /** 合法创建命令（无 factors，其余字段满足创建校验的最小集） */
    private CreateProductCommand baseCommand() {
        return new CreateProductCommand(
                PRODUCT_ID, TEMPLATE_ID, PRODUCT_CODE, "测试产品", null,
                ProductEnum.ProductForm.INDIVIDUAL, InsuranceProductType.AUTO, null, null, null, null,
                new InsureCondition(18, 65, null, null, null, null, null, null, null, null, null, null, null, null),
                null, null, null,
                List.of(MAIN_CLAUSE_ID), Map.of(), MAIN_CLAUSE_ID,
                null, null, null, null, null,
                TENANT_ID, PricingMode.RATE_TABLE, null, null, null, CREATED_BY, null);
    }
}
