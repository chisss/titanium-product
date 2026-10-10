package com.titanium.product;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.product.service.PremiumAdjustmentService;
import com.titanium.product.web.controller.product.ProductCoverageSumInsuredController;

/**
 * 产品服务组合根配置回归测试，防止运行时扫描和领域 Bean 装配被误删。
 */
class ProductApplicationConfigurationTest {

    @Test
    void shouldRegisterPremiumAdjustmentServiceAsSpringBean() throws Exception {
        Method beanMethod = ProductApplication.class.getDeclaredMethod("premiumAdjustmentService");

        assertTrue(beanMethod.isAnnotationPresent(Bean.class));
        assertNotNull(beanMethod.invoke(new ProductApplication()));
        assertTrue(PremiumAdjustmentService.class.isAssignableFrom(beanMethod.getReturnType()));
    }

    @Test
    @DisplayName("新增控制器必须真的落在组件扫描范围内且带 @RestController，否则路径在真机上不可达")
    void newControllerMustBeReachableByComponentScan() {
        ComponentScan scan = ProductApplication.class.getAnnotation(ComponentScan.class);
        assertNotNull(scan, "组合根必须显式声明 @ComponentScan");

        String pkg = ProductCoverageSumInsuredController.class.getPackageName();
        // 🔴 判据是「注解 + 包被扫到」两件事同时成立：类写对了但放错包（或漏 @RestController）时，
        // 编译、单测 standaloneSetup、ArchUnit 端点计数**全部照绿**，唯独真机上 404——
        // 这是纯静态门禁看不见的缝，故在此固化。
        assertTrue(ProductCoverageSumInsuredController.class.isAnnotationPresent(RestController.class),
                "档次配置控制器必须标注 @RestController，否则不会被注册为 MVC 处理器");
        assertTrue(Arrays.stream(scan.basePackages()).anyMatch(pkg::startsWith),
                "档次配置控制器所在包 " + pkg + " 不在组件扫描范围内，真机上路径不可达");
    }

    @Test
    void shouldScanInfrastructureEntitiesAndRepositories() {
        EntityScan entityScan = ProductApplication.class.getAnnotation(EntityScan.class);
        EnableJpaRepositories repositories = ProductApplication.class.getAnnotation(EnableJpaRepositories.class);

        assertTrue(Arrays.asList(entityScan.basePackages()).contains(
                "com.titanium.product.infrastructure.pricing.entity"));
        assertTrue(Arrays.asList(entityScan.basePackages()).contains(
                "com.titanium.product.infrastructure.maintenance.entity"));
        assertTrue(Arrays.asList(repositories.basePackages()).contains(
                "com.titanium.product.infrastructure.pricing.repository"));
        assertTrue(Arrays.asList(repositories.basePackages()).contains(
                "com.titanium.product.infrastructure.maintenance.repository"));
    }
}
