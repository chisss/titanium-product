package com.titanium.product.archunit;

import org.junit.jupiter.api.Test;

import com.titanium.buildtools.archunit.AbstractArchitectureGuardTest;

/**
 * 产品域架构守护测试：继承共享基类，仅提供本域根包。
 * 全部 DDD 分层/命名/依赖注入规则由 {@link AbstractArchitectureGuardTest} 提供，
 * 规则一处维护、各域复用，杜绝测试代码复制粘贴漂移。
 */
class ProductArchitectureTest extends AbstractArchitectureGuardTest {

    @Override
    protected String basePackage() {
        return "com.titanium.product";
    }

    /**
     * 启用 api/web 边界断言：product 域已完成 api/web 两层整改——
     * api/request 迁为 api/dto、契约实现下沉 web/provider 命名 XxxApiProvider、
     * Controller 不再 implements Api、契约接口以 Api 结尾。故 @Override 去掉基类 @Disabled，本域真跑这 4 条规则。
     *
     * <p>注意：本方案 web 层需依赖 domain command（DTO/Request → Command 翻译在 web/mapper 完成），
     * 故不启用 {@code webShouldNotDependOnDomainCommandsOrAggregates}（保持基类 @Disabled）。</p>
     */
    @Test
    @Override
    protected void applicationMustNotDependOnApiDto() {
        super.applicationMustNotDependOnApiDto();
    }

    @Test
    @Override
    protected void apiContractImplMustResideInProviderPackage() {
        super.apiContractImplMustResideInProviderPackage();
    }

    @Test
    @Override
    protected void controllerMustNotImplementApi() {
        super.controllerMustNotImplementApi();
    }

    @Test
    @Override
    protected void apiInterfacesMustBeNamedByAggregate() {
        super.apiInterfacesMustBeNamedByAggregate();
    }

    /**
     * 启用「api 层使用 Request/Response 而非 DTO」（2026-07-19 命名新规）。
     * <p>
     * 产品域 api 层已弃用 DTO：写入参 {@code CreateProductRequest}/{@code AuditProductRequest}/{@code InsureConditionRequest}
     * 落 {@code product.api.request}，读出参 {@code ProductResponse}/{@code ProductTemplateResponse}/{@code PricingBasicRuleResponse}
     * 落 {@code product.api.response}，api 层无 DTO 后缀顶层类型（嵌套配置类如 UnderwritingConfigDTO 为静态内嵌，不受约束）。
     * </p>
     */
    @Test
    @Override
    protected void apiLayerUsesRequestResponseNotDto() {
        super.apiLayerUsesRequestResponseNotDto();
    }

    /**
     * 启用「web 层使用 DTO/VO 而非 Request/Response」（2026-07-19 命名新规）。
     * <p>
     * 产品域 web 层前端入参已改名 {@code CreateProductDTO}/{@code AuditProductDTO} 等落 {@code product.web.dto}，
     * web 层无 Request/Response 后缀类型。
     * </p>
     */
    @Test
    @Override
    protected void webLayerUsesDtoVoNotRequest() {
        super.webLayerUsesDtoVoNotRequest();
    }

    /**
     * 启用「包拆分顶层清零」断言（批次 1 + 批次 2，2026-09）。
     * <p>
     * 批次 1（domain 出口侧）：{@code domain/port} 按对端域拆子包（{@code port/pricing}），顶层清零；
     * {@code infrastructure/adapter} 与 {@code infrastructure/client} 本域按主题组织于
     * {@code infrastructure/pricing/adapter} 之下，顶层无类（断言天然通过）。
     * </p>
     * <p>
     * 批次 2（契约/门面包）：{@code web/dto/pricing} 按业务主题拆 9 个子包（calculation/commission/
     * dynamicfactor/factor/pricingplan/ratetable/ruleartifact/tax/testcase）；{@code api/request} 按
     * config/premium/product、{@code api/response} 按 calculation/clause/config/document/maintenance/
     * premium/pricing/product 拆子包，顶层清零。
     * </p>
     */
    @Test
    @Override
    protected void portShouldNotContainFlatClasses() {
        super.portShouldNotContainFlatClasses();
    }

    @Test
    @Override
    protected void adapterShouldNotContainFlatClasses() {
        super.adapterShouldNotContainFlatClasses();
    }

    @Test
    @Override
    protected void clientShouldNotContainFlatClasses() {
        super.clientShouldNotContainFlatClasses();
    }

    @Test
    @Override
    protected void webDtoPricingShouldNotContainFlatClasses() {
        super.webDtoPricingShouldNotContainFlatClasses();
    }

    @Test
    @Override
    protected void apiRequestShouldNotContainFlatClasses() {
        super.apiRequestShouldNotContainFlatClasses();
    }

    @Test
    @Override
    protected void apiResponseShouldNotContainFlatClasses() {
        super.apiResponseShouldNotContainFlatClasses();
    }
}
