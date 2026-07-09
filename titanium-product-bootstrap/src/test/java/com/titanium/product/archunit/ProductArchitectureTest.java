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
}
