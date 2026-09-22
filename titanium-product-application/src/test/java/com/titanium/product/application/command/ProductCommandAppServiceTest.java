package com.titanium.product.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.exception.CommandValidationException;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.command.ReviseProductCommand;
import com.titanium.product.command.UpdateProductClauseRelCommand;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.port.clause.ClauseBindingCheck;
import com.titanium.product.port.clause.ClauseExistencePort;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.query.service.ProductTemplateQueryService;

import feign.FeignException;

/**
 * 产品命令入口的编排单元测试
 *
 * <p>焦点是条款绑定写入侧的<b>拒绝语义</b>，四条不变量各由独立用例钉住：</p>
 * <ol>
 *   <li>条款域说「不存在」时必须以业务校验异常拒绝、且<b>不得派发命令</b>——拒绝写入才是这道校验的意义所在；</li>
 *   <li>条款存在但<b>未生效</b>时同样拒绝，且文案与上者<b>必须不同</b>（合并会让运营照「不存在」的提示
 *       去新建同编码条款，撞唯一约束）；</li>
 *   <li>条款服务不可用时原样冒泡，不得被表述成「条款不存在」；</li>
 *   <li>🔴 创建与<b>整体替换</b>两条路径共用同一道校验——替换路径此前只做聚合内纯领域不变量，
 *       <b>任何字符串都能一次性抹掉</b>原有全部合法绑定。</li>
 * </ol>
 */
class ProductCommandAppServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String TEMPLATE_ID = "template-1";
    private static final String PRODUCT_ID = "product-1";
    /** 修订产出的新版本产品ID */
    private static final String NEW_PRODUCT_ID = "product-1-v2";
    /** R7-01 的故障值：条款**编码**被当成条款**主键**使用 */
    private static final String CLAUSE_CODE = "CL-MED-ZYES-V2";

    private CommandGateway commandGateway;
    private ProductTemplateQueryService productTemplateQueryService;
    private ClauseExistencePort clauseExistencePort;
    private ProductCommandAppService appService;

    @BeforeEach
    void setUp() {
        commandGateway = mock(CommandGateway.class);
        productTemplateQueryService = mock(ProductTemplateQueryService.class);
        clauseExistencePort = mock(ClauseExistencePort.class);
        appService = new ProductCommandAppService(commandGateway, productTemplateQueryService, clauseExistencePort);
    }

    // ==================== 创建路径 ====================

    @Test
    @DisplayName("模板 ACTIVE 且条款全部可绑定时派发创建命令")
    void shouldDispatchWhenTemplateActiveAndClausesBindable() {
        givenActiveTemplate();
        when(clauseExistencePort.checkBindable(List.of("clause-a"), TENANT_ID))
                .thenReturn(ClauseBindingCheck.clean());
        when(commandGateway.sendAndWait(any(CreateProductCommand.class))).thenReturn(PRODUCT_ID);

        assertEquals(PRODUCT_ID, appService.createProduct(command(List.of("clause-a"))));
    }

    @Test
    @DisplayName("🔴 有条款在条款域不存在时以业务校验失败拒绝，且不派发命令")
    void shouldRejectAndNotDispatchWhenAnyClauseMissing() {
        givenActiveTemplate();
        when(clauseExistencePort.checkBindable(List.of("clause-a", CLAUSE_CODE), TENANT_ID))
                .thenReturn(new ClauseBindingCheck(List.of(CLAUSE_CODE), List.of()));

        CommandValidationException error = assertThrows(CommandValidationException.class,
                () -> appService.createProduct(command(List.of("clause-a", CLAUSE_CODE))));

        assertTrue(error.getMessage().contains(CLAUSE_CODE), "错误消息应点名缺失的条款引用: " + error.getMessage());
        assertTrue(error.getMessage().contains("clauseIds"), "错误消息应指向 clauseIds 字段: " + error.getMessage());
        assertTrue(error.getMessage().contains("不存在"), "应说「不存在」: " + error.getMessage());
        // 拒绝写入的关键：命令根本没被派发（否则坏值照样落库，校验成了一句空话）
        verify(commandGateway, never()).sendAndWait(any(CreateProductCommand.class));
    }

    @Test
    @DisplayName("🔴 条款存在但未生效时以业务校验失败拒绝，且不派发命令")
    void shouldRejectAndNotDispatchWhenAnyClauseNotActive() {
        givenActiveTemplate();
        when(clauseExistencePort.checkBindable(List.of("clause-a", "clause-b"), TENANT_ID))
                .thenReturn(new ClauseBindingCheck(List.of(), List.of("clause-b")));

        CommandValidationException error = assertThrows(CommandValidationException.class,
                () -> appService.createProduct(command(List.of("clause-a", "clause-b"))));

        assertTrue(error.getMessage().contains("clause-b"), "错误消息应点名未生效的条款: " + error.getMessage());
        assertTrue(error.getMessage().contains("未生效"), "应说「未生效」: " + error.getMessage());
        assertTrue(error.getMessage().contains("ACTIVE"), "应给出可执行的判据（需 ACTIVE）: " + error.getMessage());
        verify(commandGateway, never()).sendAndWait(any(CreateProductCommand.class));
    }

    @Test
    @DisplayName("🔴 两类拒绝的文案必须可区分——否则运营会照「不存在」去新建同编码条款")
    void shouldUseDistinctMessagesForMissingAndNotActive() {
        String missingMessage = rejectionMessage(new ClauseBindingCheck(List.of("clause-x"), List.of()));
        String notActiveMessage = rejectionMessage(new ClauseBindingCheck(List.of(), List.of("clause-x")));

        assertNotEquals(missingMessage, notActiveMessage);
        assertTrue(missingMessage.contains("不存在") && !missingMessage.contains("未生效"),
                "「不存在」文案不得夹带「未生效」: " + missingMessage);
        assertTrue(notActiveMessage.contains("未生效") && !notActiveMessage.contains("不存在"),
                "「未生效」文案不得夹带「不存在」: " + notActiveMessage);
    }

    @Test
    @DisplayName("🔴 条款服务不可用时原样冒泡，不表述成「条款不存在」")
    void shouldPropagateClauseServiceFailureInsteadOfRejecting() {
        givenActiveTemplate();
        FeignException serviceUnavailable = mock(FeignException.ServiceUnavailable.class);
        when(clauseExistencePort.checkBindable(List.of("clause-a"), TENANT_ID)).thenThrow(serviceUnavailable);

        FeignException thrown = assertThrows(FeignException.class,
                () -> appService.createProduct(command(List.of("clause-a"))));

        assertSame(serviceUnavailable, thrown);
        verify(commandGateway, never()).sendAndWait(any(CreateProductCommand.class));
    }

    @Test
    @DisplayName("模板未生效时拒绝，且不调用条款校验")
    void shouldRejectBeforeClauseCheckWhenTemplateNotActive() {
        when(productTemplateQueryService.getTemplateById(TEMPLATE_ID, TENANT_ID))
                .thenReturn(ProductTemplateQueryResult.builder().status(CommonStatus.INACTIVE).build());

        CommandValidationException error = assertThrows(CommandValidationException.class,
                () -> appService.createProduct(command(List.of("clause-a"))));

        assertTrue(error.getMessage().contains(TEMPLATE_ID), "错误消息应点名模板: " + error.getMessage());
        verifyNoInteractions(clauseExistencePort);
        verify(commandGateway, never()).sendAndWait(any(CreateProductCommand.class));
    }

    // ==================== 整体替换路径（R8-01：此前无跨域校验） ====================

    @Test
    @DisplayName("🔴 整体替换路径同样拒绝不存在的条款，且不派发命令")
    void shouldRejectAndNotDispatchWhenReplacingWithMissingClause() {
        when(clauseExistencePort.checkBindable(List.of(CLAUSE_CODE), TENANT_ID))
                .thenReturn(new ClauseBindingCheck(List.of(CLAUSE_CODE), List.of()));

        CommandValidationException error = assertThrows(CommandValidationException.class,
                () -> appService.updateProductClauseRel(relCommand(List.of(rel(CLAUSE_CODE, true)))));

        assertTrue(error.getMessage().contains(CLAUSE_CODE), "错误消息应点名条款: " + error.getMessage());
        assertTrue(error.getMessage().contains("newClauseRels"),
                "错误消息应指向 newClauseRels 字段: " + error.getMessage());
        verify(commandGateway, never()).sendAndWait(any(UpdateProductClauseRelCommand.class));
    }

    @Test
    @DisplayName("🔴 整体替换路径同样拒绝未生效的条款（绑定要求 ACTIVE）")
    void shouldRejectAndNotDispatchWhenReplacingWithNotActiveClause() {
        when(clauseExistencePort.checkBindable(List.of("clause-a"), TENANT_ID))
                .thenReturn(new ClauseBindingCheck(List.of(), List.of("clause-a")));

        CommandValidationException error = assertThrows(CommandValidationException.class,
                () -> appService.updateProductClauseRel(relCommand(List.of(rel("clause-a", true)))));

        assertTrue(error.getMessage().contains("未生效"), "应说「未生效」: " + error.getMessage());
        verify(commandGateway, never()).sendAndWait(any(UpdateProductClauseRelCommand.class));
    }

    @Test
    @DisplayName("整体替换路径：条款全部可绑定时派发更新命令")
    void shouldDispatchWhenReplacingWithBindableClauses() {
        when(clauseExistencePort.checkBindable(List.of("clause-a", "clause-b"), TENANT_ID))
                .thenReturn(ClauseBindingCheck.clean());

        appService.updateProductClauseRel(relCommand(List.of(rel("clause-a", true), rel("clause-b", false))));

        verify(commandGateway).sendAndWait(any(UpdateProductClauseRelCommand.class));
    }

    @Test
    @DisplayName("整体替换路径：newClauseRels 为 null 时不炸，交由聚合内的非空判据拒绝")
    void shouldTolerateNullClauseRelsAndLeaveRejectionToAggregate() {
        when(clauseExistencePort.checkBindable(List.of(), TENANT_ID)).thenReturn(ClauseBindingCheck.clean());

        appService.updateProductClauseRel(new UpdateProductClauseRelCommand(PRODUCT_ID, null, TENANT_ID));

        // 跨域校验无从判定「空」，非空是纯领域不变量，由聚合根裁决
        verify(commandGateway).sendAndWait(any(UpdateProductClauseRelCommand.class));
    }

    // ==================== 修订路径（R8-01：此前零门禁，且它是 UI 上唯一可达的改绑定入口） ====================

    @Test
    @DisplayName("🔴 修订携带的条款在条款域不存在时拒绝，且不派发命令")
    void shouldRejectAndNotDispatchWhenRevisingWithMissingClause() {
        when(clauseExistencePort.checkBindable(List.of(CLAUSE_CODE), TENANT_ID))
                .thenReturn(new ClauseBindingCheck(List.of(CLAUSE_CODE), List.of()));

        CommandValidationException error = assertThrows(CommandValidationException.class,
                () -> appService.reviseProduct(reviseCommand(List.of(rel(CLAUSE_CODE, true)))));

        assertTrue(error.getMessage().contains(CLAUSE_CODE), "错误消息应点名条款: " + error.getMessage());
        assertTrue(error.getMessage().contains("newClauseRels"),
                "错误消息应指向 newClauseRels 字段: " + error.getMessage());
        verify(commandGateway, never()).sendAndWait(any(ReviseProductCommand.class));
    }

    @Test
    @DisplayName("🔴 修订携带的条款未生效时拒绝（绑定要求 ACTIVE）")
    void shouldRejectAndNotDispatchWhenRevisingWithNotActiveClause() {
        when(clauseExistencePort.checkBindable(List.of("clause-a"), TENANT_ID))
                .thenReturn(new ClauseBindingCheck(List.of(), List.of("clause-a")));

        CommandValidationException error = assertThrows(CommandValidationException.class,
                () -> appService.reviseProduct(reviseCommand(List.of(rel("clause-a", true)))));

        assertTrue(error.getMessage().contains("未生效"), "应说「未生效」: " + error.getMessage());
        verify(commandGateway, never()).sendAndWait(any(ReviseProductCommand.class));
    }

    @Test
    @DisplayName("🔴 修订未携带条款块时不调用条款域——继承的是既有数据，重校验会锁死绑 DRAFT 条款的存量产品")
    void shouldNotCheckClauseDomainWhenReviseOmitsClauseBlock() {
        when(commandGateway.sendAndWait(any(ReviseProductCommand.class))).thenReturn(NEW_PRODUCT_ID);

        assertEquals(NEW_PRODUCT_ID, appService.reviseProduct(reviseCommand(null)));

        verifyNoInteractions(clauseExistencePort);
        verify(commandGateway).sendAndWait(any(ReviseProductCommand.class));
    }

    @Test
    @DisplayName("修订携带的条款全部可绑定时派发命令")
    void shouldDispatchWhenRevisingWithBindableClauses() {
        when(clauseExistencePort.checkBindable(List.of("clause-a", "clause-b"), TENANT_ID))
                .thenReturn(ClauseBindingCheck.clean());
        when(commandGateway.sendAndWait(any(ReviseProductCommand.class))).thenReturn(NEW_PRODUCT_ID);

        assertEquals(NEW_PRODUCT_ID,
                appService.reviseProduct(reviseCommand(List.of(rel("clause-a", true), rel("clause-b", false)))));

        verify(commandGateway).sendAndWait(any(ReviseProductCommand.class));
    }

    // ==================== 辅助 ====================

    /** 用给定结论触发一次创建，取回被抛出的文案（用于比较两类拒绝的措辞） */
    private String rejectionMessage(ClauseBindingCheck check) {
        givenActiveTemplate();
        when(clauseExistencePort.checkBindable(List.of("clause-x"), TENANT_ID)).thenReturn(check);
        CommandValidationException error = assertThrows(CommandValidationException.class,
                () -> appService.createProduct(command(List.of("clause-x"))));
        return error.getMessage();
    }

    private void givenActiveTemplate() {
        when(productTemplateQueryService.getTemplateById(TEMPLATE_ID, TENANT_ID))
                .thenReturn(ProductTemplateQueryResult.builder().status(CommonStatus.ACTIVE).build());
    }

    /**
     * 构造创建产品命令（仅填充本测试关心的字段，其余留空——应用层只校验 templateId 与条款引用，
     * 其余字段的业务校验在聚合根内，不属本编排测试的范围）。
     */
    private CreateProductCommand command(List<String> clauseIds) {
        return new CreateProductCommand(PRODUCT_ID, TEMPLATE_ID, "P-001", "测试产品", null,
                null, null, null,
                null, null, null,
                null, null, null, null,
                clauseIds, Map.of(), clauseIds.isEmpty() ? null : clauseIds.get(0),
                List.of(), List.of(),
                null, null, null, TENANT_ID,
                null, null, null, null, "tester", null);
    }

    /** 构造整体替换命令 */
    private UpdateProductClauseRelCommand relCommand(List<ProductClauseRel> rels) {
        return new UpdateProductClauseRelCommand(PRODUCT_ID, rels, TENANT_ID);
    }

    /** 构造修订命令（newClauseRels 为 null 表示未携带、由聚合继承原绑定） */
    private ReviseProductCommand reviseCommand(List<ProductClauseRel> newClauseRels) {
        return new ReviseProductCommand(PRODUCT_ID, NEW_PRODUCT_ID, "测试产品V2", null,
                null, null, null, null, null, null, newClauseRels, null, null, null, null, null,
                null, null, null, TENANT_ID);
    }

    /** 构造一条条款关联 */
    private ProductClauseRel rel(String clauseId, boolean main) {
        return new ProductClauseRel(clauseId, "V1.0", main);
    }
}
