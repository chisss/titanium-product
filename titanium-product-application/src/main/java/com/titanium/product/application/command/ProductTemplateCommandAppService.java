package com.titanium.product.application.command;


import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import com.titanium.product.domain.command.ActivateProductTemplateCommand;
import com.titanium.product.domain.command.CreateProductTemplateCommand;
import com.titanium.product.domain.command.DeactivateProductTemplateCommand;
import com.titanium.product.domain.command.UpdateProductTemplateCommand;

import lombok.RequiredArgsConstructor;

/**
 * 产品模板命令应用服务
 */
@Service
@RequiredArgsConstructor
public class ProductTemplateCommandAppService {

    private final CommandGateway commandGateway;

    /**
     * 创建产品模板
     */
    public String createTemplate(CreateProductTemplateCommand command) {
        CompletableFuture<String> future = commandGateway.send(command);
        return future.join();
    }

    /**
     * 更新产品模板
     */
    public void updateTemplate(UpdateProductTemplateCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 激活产品模板
     */
    public void activateTemplate(ActivateProductTemplateCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 停用产品模板
     */
    public void deactivateTemplate(DeactivateProductTemplateCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 生成模板ID
     */
    public static String generateTemplateId() {
        return "TPL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
