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
import com.titanium.product.command.AuditProductCommand;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.command.ReviseProductCommand;
import com.titanium.product.command.SubmitProductForAuditCommand;
import com.titanium.product.command.UpdateProductClauseRelCommand;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.exception.ProductStatusPreconditionException;
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
                TENANT_ID, PricingMode.RATE_TABLE, null, null, null, CREATED_BY, null, null);

        fixture.givenNoPriorActivity()
                .when(command)
                .expectException(CommandValidationException.class)
                .expectExceptionMessage(Matchers.containsString(
                        "产品级 factors 系数不参与试算，请改为在定价计划中配置动态因子"));
    }

    @Test
    @DisplayName("修订前置校验：非 EFFECTIVE（DRAFT）产品拒绝修订")
    void shouldRejectReviseWhenProductNotEffective() {
        // 创建即 DRAFT（givenCommands 以命令产出事件作为给定历史），未审核通过前不可修订
        fixture.givenCommands(baseCommand())
                .when(reviseCommand())
                .expectException(ProductStatusPreconditionException.class)
                .expectExceptionMessage(Matchers.containsString("修订"));
    }

    @Test
    @DisplayName("创建时条款重复即拒绝：relId 由「条款ID + 版本」派生，重复条目会产生同 relId 的两条关联")
    void shouldRejectCreateWhenClauseIdsDuplicated() {
        CreateProductCommand command = new CreateProductCommand(
                PRODUCT_ID, TEMPLATE_ID, PRODUCT_CODE, "测试产品", null,
                ProductEnum.ProductForm.INDIVIDUAL, InsuranceProductType.AUTO, null, null, null, null,
                new InsureCondition(18, 65, null, null, null, null, null, null, null, null, null, null, null, null),
                null, null, null,
                List.of(MAIN_CLAUSE_ID, MAIN_CLAUSE_ID), Map.of(), MAIN_CLAUSE_ID,
                null, null, null, null, null,
                TENANT_ID, PricingMode.RATE_TABLE, null, null, null, CREATED_BY, null, null);

        fixture.givenNoPriorActivity()
                .when(command)
                .expectException(CommandValidationException.class)
                .expectExceptionMessage(Matchers.containsString("条款不得重复绑定"));
    }

    @Test
    @DisplayName("创建时条款列表为空即拒绝（与替换路径共用同一判据）")
    void shouldRejectCreateWhenClauseIdsEmpty() {
        fixture.givenNoPriorActivity()
                .when(command(List.of(), MAIN_CLAUSE_ID))
                .expectException(CommandValidationException.class)
                .expectExceptionMessage(Matchers.containsString("至少一条条款"));
    }

    @Test
    @DisplayName("创建时条款列表为 null 即拒绝且不抛 NPE——钉住两个判据的先后（非空须先于去重，后者对 null 会 NPE）")
    void shouldRejectCreateWhenClauseIdsNull() {
        fixture.givenNoPriorActivity()
                .when(command(null, MAIN_CLAUSE_ID))
                .expectException(CommandValidationException.class)
                .expectExceptionMessage(Matchers.containsString("至少一条条款"));
    }

    @Test
    @DisplayName("更新条款关联：列表为空即拒绝（该处理器此前零校验）")
    void shouldRejectClauseRelUpdateWhenListEmpty() {
        fixture.givenCommands(baseCommand())
                .when(new UpdateProductClauseRelCommand(PRODUCT_ID, List.of(), TENANT_ID))
                .expectException(CommandValidationException.class)
                .expectExceptionMessage(Matchers.containsString("至少一条条款"));
    }

    @Test
    @DisplayName("更新条款关联：主条款条数不等于 1 即拒绝")
    void shouldRejectClauseRelUpdateWhenMainClauseCountIsNotOne() {
        fixture.givenCommands(baseCommand())
                .when(new UpdateProductClauseRelCommand(PRODUCT_ID, List.of(
                        new ProductClauseRel("CLAUSE_001", "V1.0", true),
                        new ProductClauseRel("CLAUSE_002", "V1.0", true)), TENANT_ID))
                .expectException(CommandValidationException.class)
                .expectExceptionMessage(Matchers.containsString("只能指定一条主条款"));
    }

    @Test
    @DisplayName("更新条款关联：合法列表（恰一条主条款、无重复）替换成功")
    void shouldUpdateClauseRelsWhenListValid() {
        fixture.givenCommands(baseCommand())
                .when(new UpdateProductClauseRelCommand(PRODUCT_ID, List.of(
                        new ProductClauseRel("CLAUSE_001", "V1.0", true),
                        new ProductClauseRel("CLAUSE_002", "V1.0", false)), TENANT_ID))
                .expectSuccessfulHandlerExecution();
    }

    // ==================== 修订路径的条款绑定（R8-01：此前零校验） ====================

    @Test
    @DisplayName("🔴 修订未携带条款块时继承原绑定（而非静默清空）——此前 null 会原样落进新版本")
    void shouldInheritClauseRelsWhenReviseOmitsThem() {
        // 本用例钉住的是「解析后的列表非空且合规」：若处理器不再回退到 this.clauseRels，
        // 解析结果就是 null，validateClauseRels 的非空判据会立刻拒绝，本用例即变红。
        fixture.givenCommands(baseCommand(), submitCommand(), auditCommand())
                .when(reviseCommand(null))
                .expectSuccessfulHandlerExecution();
    }

    @Test
    @DisplayName("修订携带空条款列表即拒绝（绑定非空是硬不变量）")
    void shouldRejectReviseWhenClauseRelsEmpty() {
        fixture.givenCommands(baseCommand(), submitCommand(), auditCommand())
                .when(reviseCommand(List.of()))
                .expectException(CommandValidationException.class)
                .expectExceptionMessage(Matchers.containsString("至少一条条款"));
    }

    @Test
    @DisplayName("修订携带零主条款即拒绝")
    void shouldRejectReviseWhenNoMainClause() {
        fixture.givenCommands(baseCommand(), submitCommand(), auditCommand())
                .when(reviseCommand(List.of(
                        new ProductClauseRel("CLAUSE_001", "V1.0", false),
                        new ProductClauseRel("CLAUSE_002", "V1.0", false))))
                .expectException(CommandValidationException.class)
                .expectExceptionMessage(Matchers.containsString("只能指定一条主条款"));
    }

    @Test
    @DisplayName("修订携带重复条款即拒绝（relId 由「条款ID + 版本」派生，重复会产出同 relId 的两条关联）")
    void shouldRejectReviseWhenClauseIdsDuplicated() {
        fixture.givenCommands(baseCommand(), submitCommand(), auditCommand())
                .when(reviseCommand(List.of(
                        new ProductClauseRel("CLAUSE_001", "V1.0", true),
                        new ProductClauseRel("CLAUSE_001", "V1.0", false))))
                .expectException(CommandValidationException.class)
                .expectExceptionMessage(Matchers.containsString("条款不得重复绑定"));
    }

    @Test
    @DisplayName("修订携带合法条款列表时成功（新版本按新绑定落地）")
    void shouldAcceptReviseWithValidClauseRels() {
        fixture.givenCommands(baseCommand(), submitCommand(), auditCommand())
                .when(reviseCommand(List.of(
                        new ProductClauseRel("CLAUSE_001", "V1.0", true),
                        new ProductClauseRel("CLAUSE_002", "V1.0", false))))
                .expectSuccessfulHandlerExecution();
    }

    /** 提交审核（DRAFT → AUDITING），用于构造 EFFECTIVE 前置状态 */
    private SubmitProductForAuditCommand submitCommand() {
        return new SubmitProductForAuditCommand(PRODUCT_ID, "tester", "提交人");
    }

    /** 审核通过（AUDITING → EFFECTIVE），用于构造 EFFECTIVE 前置状态 */
    private AuditProductCommand auditCommand() {
        return new AuditProductCommand(PRODUCT_ID, "auditor", "审核人", "同意",
                ProductEnum.AuditResult.PASS);
    }

    /** 修订命令：新版本全量配置最小集（条款关联继承创建时的主条款） */
    private ReviseProductCommand reviseCommand() {
        return reviseCommand(null);
    }

    /** 修订命令，可替换条款关联块（null 表示未携带、继承原绑定） */
    private ReviseProductCommand reviseCommand(List<ProductClauseRel> newClauseRels) {
        return new ReviseProductCommand(PRODUCT_ID, "PROD_001_V2", "测试产品V2", null,
                ProductEnum.ProductForm.INDIVIDUAL, InsuranceProductType.AUTO, null,
                new InsureCondition(18, 65, null, null, null, null, null, null, null, null, null, null, null, null),
                null, null, newClauseRels, null, null, null, null, null,
                PricingMode.RATE_TABLE, null, null, TENANT_ID);
    }

    /** 合法创建命令（无 factors，其余字段满足创建校验的最小集） */
    private CreateProductCommand baseCommand() {
        return command(List.of(MAIN_CLAUSE_ID), MAIN_CLAUSE_ID);
    }

    /** 合法创建命令，可替换条款列表与主条款（条款相关用例复用，避免复制整段构造器） */
    private CreateProductCommand command(List<String> clauseIds, String mainClauseId) {
        return new CreateProductCommand(
                PRODUCT_ID, TEMPLATE_ID, PRODUCT_CODE, "测试产品", null,
                ProductEnum.ProductForm.INDIVIDUAL, InsuranceProductType.AUTO, null, null, null, null,
                new InsureCondition(18, 65, null, null, null, null, null, null, null, null, null, null, null, null),
                null, null, null,
                clauseIds, Map.of(), mainClauseId,
                null, null, null, null, null,
                TENANT_ID, PricingMode.RATE_TABLE, null, null, null, CREATED_BY, null, null);
    }
}
