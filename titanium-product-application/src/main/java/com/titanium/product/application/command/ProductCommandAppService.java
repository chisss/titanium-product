package com.titanium.product.application.command;

import com.titanium.product.domain.command.AuditProductCommand;
import com.titanium.product.domain.command.CreateProductCommand;
import com.titanium.product.domain.command.InvalidateProductCommand;
import com.titanium.product.domain.command.ReviseProductCommand;
import com.titanium.product.domain.command.UpdateProductClauseRelCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产品命令应用服务 处理产品相关的命令操作
 */
@Service
@Transactional
public class ProductCommandAppService {
    private final CommandGateway commandGateway;

    /**
     * 构造函数
     * 
     * @param commandGateway 命令网关
     */
    public ProductCommandAppService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    /**
     * 创建产品
     * 
     * @param command 创建产品命令
     * @return 产品ID
     */
    public String createProduct(CreateProductCommand command) {
        return commandGateway.sendAndWait(command);
    }

    /**
     * 审核产品
     * 
     * @param command 审核产品命令
     */
    public void auditProduct(AuditProductCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 修订产品
     * 
     * @param command 修订产品命令
     */
    public void reviseProduct(ReviseProductCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 下架产品
     * 
     * @param command 下架产品命令
     */
    public void invalidateProduct(InvalidateProductCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 更新产品条款关联
     * 
     * @param command 更新产品条款关联命令
     */
    public void updateProductClauseRel(UpdateProductClauseRelCommand command) {
        commandGateway.sendAndWait(command);
    }
}
