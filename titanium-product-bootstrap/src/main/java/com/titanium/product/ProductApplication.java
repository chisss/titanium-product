package com.titanium.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 产品服务启动类
 * 产品服务的入口点，负责启动Spring Boot应用
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.titanium.product.api")
@EntityScan(basePackages = "com.titanium.product.infrastructure.entity")
@EnableJpaRepositories(basePackages = "com.titanium.product.infrastructure.repository")
public class ProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}