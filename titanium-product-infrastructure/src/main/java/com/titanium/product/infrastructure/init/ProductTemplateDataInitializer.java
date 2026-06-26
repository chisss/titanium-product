package com.titanium.product.infrastructure.init;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 产品模板数据初始化器（已停用）
 * <p>
 * 原实现在启动时预置 19 个险种产品模板，但其调用的 {@code CreateProductTemplateCommand} /
 * {@code UnderwritingConfig} / {@code PolicyStructureConfig} / {@code BillingConfig} /
 * {@code PolicyStage} 等均为<b>已被重构移除的旧领域模型 API</b>（构造器参数列表与现行 record 不匹配），
 * 导致整模块无法编译。该类仅为开发期演示种子数据，非生产逻辑。
 * </p>
 * <p>
 * 现暂停用，保留 Bean 占位。待《领域设计指导文档》§四 产品模板模型（含 PolicyForm/险种差异化配置）
 * 落地后，应基于新版 {@code CreateProductTemplateCommand} 重写各险种模板种子数据。
 * </p>
 */
@Slf4j
@Component
public class ProductTemplateDataInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // 旧版种子数据已停用：依赖的产品模板领域模型 API 已重构，待新模型落地后重写。
        log.info("ProductTemplateDataInitializer 已停用（旧模型 API 已移除），跳过产品模板预置初始化");
    }
}
