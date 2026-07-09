package com.titanium.product.application.command;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.command.AuditProductCommand;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.command.InvalidateProductCommand;
import com.titanium.product.command.RejectProductAuditCommand;
import com.titanium.product.command.ReviseProductCommand;
import com.titanium.product.command.SubmitProductForAuditCommand;
import com.titanium.product.command.UpdateAttachProductCommand;
import com.titanium.product.command.UpdateProductClauseRelCommand;
import com.titanium.product.command.UpdateSalesChannelCommand;

import lombok.RequiredArgsConstructor;

/**
 * 产品命令应用服务（写用例入口门面）
 *
 * <p>入参统一为 domain 领域命令：Request/DTO → Command 的翻译由 web 层
 * {@code ProductWebMapper} 承担，本层只做事务边界与 {@code commandGateway.sendAndWait} 派发，
 * 不依赖 api 契约细节、不写业务规则（规则内聚聚合根）。（项目规约 3.4.8/六边形隔离）</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProductCommandAppService {

    private final CommandGateway commandGateway;

    /**
     * 创建产品：派发创建命令。
     *
     * @param command 创建产品命令
     * @return 产品ID
     */
    public String createProduct(CreateProductCommand command) {
        return commandGateway.sendAndWait(command);
    }

    /**
     * 提交产品审核。
     *
     * @param productId 产品ID
     */
    public void submitForAudit(String productId) {
        commandGateway.sendAndWait(new SubmitProductForAuditCommand(productId, "system", "system"));
    }

    /**
     * 审核通过产品：派发审核命令。
     *
     * @param command 审核产品命令
     */
    public void auditProduct(AuditProductCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 驳回产品审核：派发驳回命令。
     *
     * @param command 驳回产品审核命令
     */
    public void rejectAudit(RejectProductAuditCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 修订产品。
     *
     * @param command 修订命令
     */
    public void reviseProduct(ReviseProductCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 下架产品。
     *
     * @param productId 产品ID
     */
    public void invalidateProduct(String productId) {
        commandGateway.sendAndWait(new InvalidateProductCommand(productId));
    }

    /**
     * 更新产品条款关系。
     *
     * @param command 更新命令
     */
    public void updateProductClauseRel(UpdateProductClauseRelCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 更新销售渠道。
     *
     * @param command 更新命令
     */
    public void updateSalesChannel(UpdateSalesChannelCommand command) {
        commandGateway.sendAndWait(command);
    }

    /**
     * 更新附加险。
     *
     * @param command 更新命令
     */
    public void updateAttachProduct(UpdateAttachProductCommand command) {
        commandGateway.sendAndWait(command);
    }
}
