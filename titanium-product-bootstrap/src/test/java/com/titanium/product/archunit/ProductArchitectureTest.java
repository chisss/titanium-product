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
     * 产品域 api 层已弃用 DTO：写入参 {@code CreateProductRequest}/{@code AuditProductRequest}
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
     * 批次 2（契约/门面包）：{@code web/dto/pricing} 按业务主题拆 11 个子包（basicrule/calculation/commission/
     * dynamicfactor/factor/pricingplan/ratetable/ruleartifact/surrender/tax/testcase）；{@code api/request} 按
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

    /**
     * 启用「web.controller 按业务主题拆子包、顶层清零」（分包规则·批次 2）。
     * <p>
     * 产品域 web.controller 6 类五分：{@code controller.product}（产品主控制器）、
     * {@code controller.template}（产品模板）、{@code controller.pricing}（定价方案 + 费率表）、
     * {@code controller.actuarial}（精算工作台）、{@code controller.maintenance}（保全供给），顶层零类。
     * 主题命名与既有 {@code application.command} 的 {@code pricing}/{@code maintenance} 子包一致。
     * </p>
     * <p>
     * <b>测试目录同迁 / 跨主题补 import 并存</b>：三个单主题 controller 测试已随迁
     * （{@code ProductPricingPlanControllerTest}/{@code ProductRateTableControllerTest} → {@code controller.pricing}，
     * {@code ProductMaintenanceOfferingControllerTest} → {@code controller.maintenance}）。
     * </p>
     */
    @Test
    @Override
    protected void webControllerShouldNotContainFlatClasses() {
        super.webControllerShouldNotContainFlatClasses();
    }

    /**
     * 启用「web.dto 按业务主题拆子包、顶层清零」（分包规则·批次 2）。
     * <p>
     * 产品域 web.dto 8 类三分：{@code dto.product}（创建 / 修订 / 审核 / 寿险配置 / 投保条件）
     * 5 类；{@code dto.template}（创建 / 更新产品模板）2 类；定价基础规则 1 类落新建
     * {@code dto.pricing.basicrule}，顶层零类。
     * </p>
     * <p>
     * 🔴 <b>迁入前须先查目标包断言是否已启用</b>：{@code dto.pricing} 的 {@code webDtoPricingShouldNotContainFlatClasses}
     * 断言本域**早已启用**，意味着该包本身已固化为中间层、其下须再分二级子包——把 {@code PricingBasicRuleDTO}
     * 直接放顶层会立即 RED；该包此前为空，靠 {@code allowEmptyShould(true)} 长期"假绿"，本次迁入才暴露。
     * 故定价基础规则落 {@code dto.pricing.basicrule}（与同包既有 calculation/factor 等 10 个二级子包同构）。
     * </p>
     * <p>
     * 🔴 <b>跨子包引用需补 import</b>：{@code dto.product} 的 {@code CreateProductDTO} /
     * {@code ReviseProductDTO} 引用 {@code dto.pricing.PricingBasicRuleDTO}，原为同包免 import，
     * 拆入不同子包后必须显式 import——与 policy 域同批的同类易漏点。
     * 跨主题测试 {@code ProductNestedInputDTOContractTest}（同时守护 product 与 pricing 两子包 DTO）
     * 无法归单一主题，留在 {@code web.dto} 测试包并补 2 处 import。
     * </p>
     */
    @Test
    @Override
    protected void webDtoShouldNotContainFlatClasses() {
        super.webDtoShouldNotContainFlatClasses();
    }

    // 注：域内其余契约/门面包（application.command 顶层 2 类、application.model 顶层 1 类）顶层 ≤2 类，
    // 未构成「≥3 个可区分功能主题组」，按《包结构分包规范与执行方案-2026-09》§一判据表豁免，不启用对应断言。
}
