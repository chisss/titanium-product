package com.titanium.product.application.command;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.domain.command.AuditProductCommand;
import com.titanium.product.domain.command.CreateProductCommand;
import com.titanium.product.domain.command.InvalidateProductCommand;
import com.titanium.product.domain.command.RejectProductAuditCommand;
import com.titanium.product.domain.command.ReviseProductCommand;
import com.titanium.product.domain.command.SubmitProductForAuditCommand;
import com.titanium.product.domain.command.UpdateAttachProductCommand;
import com.titanium.product.domain.command.UpdateProductClauseRelCommand;
import com.titanium.product.domain.command.UpdateSalesChannelCommand;

/**
 * 产品命令应用服务
 * 处理产品相关的命令操作
 */
@Service
@Transactional
public class ProductCommandAppService {
    private final CommandGateway commandGateway;

    public ProductCommandAppService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    public String createProduct(CreateProductCommand command) {
        return commandGateway.sendAndWait(command);
    }

    public void submitForAudit(SubmitProductForAuditCommand command) {
        commandGateway.sendAndWait(command);
    }

    public void auditProduct(AuditProductCommand command) {
        commandGateway.sendAndWait(command);
    }

    public void rejectAudit(RejectProductAuditCommand command) {
        commandGateway.sendAndWait(command);
    }

    public void reviseProduct(ReviseProductCommand command) {
        commandGateway.sendAndWait(command);
    }

    public void invalidateProduct(InvalidateProductCommand command) {
        commandGateway.sendAndWait(command);
    }

    public void updateProductClauseRel(UpdateProductClauseRelCommand command) {
        commandGateway.sendAndWait(command);
    }

    public void updateSalesChannel(UpdateSalesChannelCommand command) {
        commandGateway.sendAndWait(command);
    }

    public void updateAttachProduct(UpdateAttachProductCommand command) {
        commandGateway.sendAndWait(command);
    }
}
