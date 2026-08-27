package com.titanium.product;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.titanium.product.service.PremiumAdjustmentService;

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
