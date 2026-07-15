package com.titanium.product.application.command;

import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import com.titanium.product.command.ActivateProductTemplateCommand;
import com.titanium.product.command.ConfigureLifeProductCommand;
import com.titanium.product.command.CreateProductTemplateCommand;
import com.titanium.product.command.DeactivateProductTemplateCommand;
import com.titanium.product.command.UpdateProductTemplateCommand;

import lombok.RequiredArgsConstructor;

/**
 * 产品模板命令应用服务（写用例入口门面）
 *
 * <p>入参统一为 domain 领域命令：Request → Command 的翻译由 web 层
 * {@code ProductTemplateWebMapper} 承担，本层只做 {@code commandGateway} 派发，
 * 不依赖 api 契约细节、不写业务规则（项目规约 3.4.8/六边形隔离）。</p>
 */
@Service
@RequiredArgsConstructor
public class ProductTemplateCommandAppService {

    private final CommandGateway commandGateway;

    /**
     * 创建产品模板：派发创建命令。
     *
     * @param command 创建产品模板命令
     * @return 模板ID
     */
    public String createTemplate(CreateProductTemplateCommand command) {
        CompletableFuture<String> future = commandGateway.send(command);
        return future.join();
    }

    /**
     * 更新产品模板。
     *
     * @param command 更新命令
     */
    public void updateTemplate(UpdateProductTemplateCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 配置寿险产品规格（投保年龄/保额范围/缴费期/保障期）。
     *
     * @param command 寿险产品规格配置命令
     */
    public void configureLifeProduct(ConfigureLifeProductCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 激活产品模板。
     *
     * @param templateId 模板ID
     * @param tenantId   租户ID
     */
    public void activateTemplate(String templateId, String tenantId) {
        commandGateway.sendAndWait(new ActivateProductTemplateCommand(templateId, tenantId));
    }

    /**
     * 停用产品模板。
     *
     * @param templateId 模板ID
     * @param tenantId   租户ID
     */
    public void deactivateTemplate(String templateId, String tenantId) {
        commandGateway.sendAndWait(new DeactivateProductTemplateCommand(templateId, tenantId));
    }
}
