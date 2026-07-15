package com.titanium.product.aggregate;

import java.math.BigDecimal;
import java.util.List;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.command.ActivateProductTemplateCommand;
import com.titanium.product.command.ConfigureLifeProductCommand;
import com.titanium.product.command.CreateProductTemplateCommand;
import com.titanium.product.command.DeactivateProductTemplateCommand;
import com.titanium.product.command.UpdateProductTemplateCommand;
import com.titanium.product.event.LifeProductConfiguredEvent;
import com.titanium.product.event.ProductTemplateActivatedEvent;
import com.titanium.product.event.ProductTemplateCreatedEvent;
import com.titanium.product.event.ProductTemplateDeactivatedEvent;
import com.titanium.product.event.ProductTemplateUpdatedEvent;
import com.titanium.product.exception.ProductStatusPreconditionException;
import com.titanium.product.valueobject.DividendConfig;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.LifeProductSpec;

/**
 * 产品模板聚合根测试
 *
 * <p>覆盖：创建 → 更新 → 停用 → 重复停用（拒绝）→ 激活 → 重复激活（幂等）
 * 与事件重放后行为配置字段的完整性。聚合根内部使用 {@link java.time.LocalDateTime#now()}
 * 生成时间戳，但更新/激活/停用事件不含时间戳字段，无需忽略字段。创建事件的 occurredAt
 * 为非确定值，涉及创建事件比对处统一忽略。
 */
class ProductTemplateTest {

    private FixtureConfiguration<ProductTemplate> fixture;

    private static final String TEMPLATE_ID = "TPL_001";
    private static final String TEMPLATE_CODE = "TPL_CAR_BASIC";
    private static final String TEMPLATE_NAME = "车险基础模板";
    private static final String TENANT_ID = "TENANT_001";
    private static final String CREATED_BY = "tester";

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(ProductTemplate.class);
        // 各事件内部以 now() 生成 occurredAt，为非确定值，事件比对时统一忽略
        fixture.registerIgnoredField(ProductTemplateCreatedEvent.class, "occurredAt");
        fixture.registerIgnoredField(ProductTemplateUpdatedEvent.class, "occurredAt");
        fixture.registerIgnoredField(ProductTemplateActivatedEvent.class, "occurredAt");
        fixture.registerIgnoredField(ProductTemplateDeactivatedEvent.class, "occurredAt");
    }

    /** 出单流程配置最小实例（创建命令唯一强校验的非空字段） */
    private IssuanceProcessConfig issuanceProcessConfig() {
        return new IssuanceProcessConfig(ProductEnum.IssuanceMode.ONE_STEP, null, false, false, false, null, null);
    }

    private CreateProductTemplateCommand createCommand() {
        return new CreateProductTemplateCommand(TEMPLATE_ID, TEMPLATE_CODE, TEMPLATE_NAME,
                InsuranceType.CAR, "车险标准模板", issuanceProcessConfig(),
                null, null, null, null, null, null, null, TENANT_ID, CREATED_BY);
    }

    private ProductTemplateCreatedEvent createdEvent() {
        return new ProductTemplateCreatedEvent(TEMPLATE_ID, TEMPLATE_CODE, TEMPLATE_NAME,
                InsuranceType.CAR, "车险标准模板", issuanceProcessConfig(),
                null, null, null, null, null, null, null,
                CommonStatus.ACTIVE, TENANT_ID, CREATED_BY, null);
    }

    @Test
    @DisplayName("创建模板：发布创建事件且状态为 ACTIVE")
    void shouldCreateTemplate() {
        fixture.givenNoPriorActivity()
                .when(createCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(createdEvent());
    }

    @Test
    @DisplayName("更新模板：发布更新事件，行为配置字段随事件回放到聚合")
    void shouldUpdateTemplate() {
        UpdateProductTemplateCommand cmd = new UpdateProductTemplateCommand(TEMPLATE_ID, "车险模板V2",
                ProductEnum.IssuanceMode.TWO_STEP, List.of(), null, null, null, null, null, null, null, TENANT_ID);

        fixture.given(createdEvent())
                .when(cmd)
                .expectSuccessfulHandlerExecution()
                .expectEvents(new ProductTemplateUpdatedEvent(TEMPLATE_ID, "车险模板V2",
                        ProductEnum.IssuanceMode.TWO_STEP, List.of(), null, null, null, null, null, null, null,
                        TENANT_ID, null));
    }

    @Test
    @DisplayName("分红险模板：更新事件携带分红配置（红利分配方式+三档演示利率）并回放到聚合")
    void shouldUpdateTemplateWithDividendConfig() {
        DividendConfig dividendConfig = new DividendConfig(ProductEnum.DividendDistribution.CASH,
                new BigDecimal("0.015"), new BigDecimal("0.035"), new BigDecimal("0.045"));
        UpdateProductTemplateCommand cmd = new UpdateProductTemplateCommand(TEMPLATE_ID, "分红寿险模板",
                ProductEnum.IssuanceMode.ONE_STEP, List.of(), null, null, null, null, null, null, dividendConfig,
                TENANT_ID);

        fixture.given(createdEvent())
                .when(cmd)
                .expectSuccessfulHandlerExecution()
                .expectEvents(new ProductTemplateUpdatedEvent(TEMPLATE_ID, "分红寿险模板",
                        ProductEnum.IssuanceMode.ONE_STEP, List.of(), null, null, null, null, null, null, dividendConfig,
                        TENANT_ID, null));
    }

    @Test
    @DisplayName("停用模板：ACTIVE 态可停用，发布停用事件")
    void shouldDeactivateWhenActive() {
        fixture.given(createdEvent())
                .when(new DeactivateProductTemplateCommand(TEMPLATE_ID, TENANT_ID))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new ProductTemplateDeactivatedEvent(TEMPLATE_ID, TENANT_ID, null));
    }

    @Test
    @DisplayName("重复停用：非 ACTIVE 态停用抛前置条件异常")
    void shouldRejectDeactivateWhenNotActive() {
        fixture.given(createdEvent(), new ProductTemplateDeactivatedEvent(TEMPLATE_ID, TENANT_ID, null))
                .when(new DeactivateProductTemplateCommand(TEMPLATE_ID, TENANT_ID))
                .expectException(ProductStatusPreconditionException.class)
                .expectNoEvents();
    }

    @Test
    @DisplayName("激活模板：停用后可重新激活，发布激活事件")
    void shouldActivateWhenInactive() {
        fixture.given(createdEvent(), new ProductTemplateDeactivatedEvent(TEMPLATE_ID, TENANT_ID, null))
                .when(new ActivateProductTemplateCommand(TEMPLATE_ID, TENANT_ID))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new ProductTemplateActivatedEvent(TEMPLATE_ID, TENANT_ID, null));
    }

    @Test
    @DisplayName("重复激活：已 ACTIVE 态激活幂等，不产生事件")
    void shouldBeIdempotentWhenAlreadyActive() {
        fixture.given(createdEvent())
                .when(new ActivateProductTemplateCommand(TEMPLATE_ID, TENANT_ID))
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();
    }

    @Test
    @DisplayName("停用后仍可更新：INACTIVE 态更新放行")
    void shouldAllowUpdateWhenInactive() {
        UpdateProductTemplateCommand cmd = new UpdateProductTemplateCommand(TEMPLATE_ID, "停用后改名",
                ProductEnum.IssuanceMode.ONE_STEP, List.of(), null, null, null, null, null, null, null, TENANT_ID);

        fixture.given(createdEvent(), new ProductTemplateDeactivatedEvent(TEMPLATE_ID, TENANT_ID, null))
                .when(cmd)
                .expectSuccessfulHandlerExecution()
                .expectEvents(new ProductTemplateUpdatedEvent(TEMPLATE_ID, "停用后改名",
                        ProductEnum.IssuanceMode.ONE_STEP, List.of(), null, null, null, null, null, null, null,
                        TENANT_ID, null));
    }

    @Test
    @DisplayName("配置寿险产品规格：发布规格配置事件")
    void shouldConfigureLifeProductSpec() {
        LifeProductSpec spec = LifeProductSpec.of(InsuranceProductType.TERM_LIFE, 18, 60,
                new BigDecimal("100000"), new BigDecimal("3000000"));
        fixture.given(createdEvent())
                .when(new ConfigureLifeProductCommand(TEMPLATE_ID, spec, TENANT_ID))
                .expectSuccessfulHandlerExecution()
                .expectEventsMatching(org.axonframework.test.matchers.Matchers
                        .payloadsMatching(org.axonframework.test.matchers.Matchers.exactSequenceOf(
                                org.hamcrest.CoreMatchers.instanceOf(LifeProductConfiguredEvent.class))));
    }

    @Test
    @DisplayName("寿险规格为空被拒绝")
    void shouldRejectNullLifeProductSpec() {
        fixture.given(createdEvent())
                .when(new ConfigureLifeProductCommand(TEMPLATE_ID, null, TENANT_ID))
                .expectException(com.titanium.metadata.exception.CommandValidationException.class);
    }

    @Test
    @DisplayName("寿险规格重建：事件重放后 lifeProductSpec 字段完整")
    void shouldReplayLifeProductSpec() {
        LifeProductSpec spec = LifeProductSpec.of(InsuranceProductType.WHOLE_LIFE, 0, 70,
                new BigDecimal("50000"), new BigDecimal("5000000"));
        fixture.given(createdEvent(), new LifeProductConfiguredEvent(TEMPLATE_ID, spec, TENANT_ID, null))
                .when(new ActivateProductTemplateCommand(TEMPLATE_ID, TENANT_ID))
                .expectSuccessfulHandlerExecution()
                .expectState(t -> {
                    if (t.getLifeProductSpec() == null
                            || t.getLifeProductSpec().productType() != InsuranceProductType.WHOLE_LIFE) {
                        throw new AssertionError("事件重放后寿险规格应完整恢复");
                    }
                });
    }
}
