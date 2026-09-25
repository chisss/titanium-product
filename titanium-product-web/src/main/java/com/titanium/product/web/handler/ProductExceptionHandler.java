package com.titanium.product.web.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.titanium.common.boundary.ExceptionResponseFactory;
import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.SystemErrorCode;
import com.titanium.metadata.exception.DomainException;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.exception.ProductMaintenanceOfferingException;

import lombok.extern.slf4j.Slf4j;

/**
 * Product Web 统一异常处理器。
 * <p>
 * 业务异常的状态与错误码<b>一律取自异常自身</b>：状态由 {@code BaseErrorCode.category()} 的语义类别决定
 * （经 {@link ExceptionResponseFactory} 走全系统唯一的「类别 → 状态」表），响应体回显异常携带的错误码。
 * </p>
 * <p>
 * 🔴 <b>改造前本类持有两张手写的「常量 → 状态」表（审查报告 EXC-02）</b>：
 * </p>
 * <ol>
 *   <li>{@code statusFor(ProductErrorCode)}——25 个常量逐个列出，其余落 {@code default -> 400}。
 *       残缺的后果是可量化的：与类别表比对后 <b>9 处冲突</b>，其中 5 处把「请求参数不合法」
 *       （币种不匹配、定价模式不支持、方案校验失败、调整项非法）报成 <b>422</b> 而非 400，
 *       3 处把「资源当前状态不允许」报成 422 而非 <b>409</b>，1 处把「费率行多重匹配」报成 409 而非 422；
 *       </li>
 *   <li>{@code statusFor(ProductMaintenanceOfferingFailureReason)}——按 7 个 reason 手工映射。
 *       该表<b>与错误码一一对应</b>（{@code ProductMaintenanceOfferingException} 的 errorCode
 *       是 reason 的函数），故属纯冗余的第二份事实来源，已随 offering 错误码补上 {@code category()} 而删除。
 *       </li>
 * </ol>
 * <p>
 * 🔴 <b>5xx 不回显自定义文案</b>：{@link ExceptionResponseFactory} 对 5xx 一律改用错误码内置文案，
 * 因为内部消息（连接串、类名、主键）对调用方没有可行动价值，只增加信息泄露面（CWE-209）。
 * 本域受影响的是跨域依赖失败分支（{@code PRICING_DEPENDENCY_*}，类别 {@code DEPENDENCY_FAILURE} → 502）：
 * 其带下游细节的自定义文案此前会回显给调用方，现改为固定文案，细节仍完整进日志。
 * </p>
 * <p>
 * <b>已删除的死码</b>：{@code handleValidationException(MethodArgumentNotValidException)}
 * 与 {@code handleMissingRequestParameter} 一类的 MVC 框架异常处理器——自 m30-05 起
 * {@code titanium-common} 的边界处理器（{@code BoundaryExceptionHandler}，序为
 * {@code HIGHEST_PRECEDENCE + 20}）按可赋值性先接管这 20 类框架异常，域内同名处理器<b>永不派发</b>。
 * 其字段级明细由边界处理器等价承接（{@code clientText} 保留默认文案）。业务异常的兜底不可省：
 * 边界处理器<b>刻意不声明</b> {@code Exception.class}，故本域需自行兜底。
 * </p>
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.titanium.product.web")
public class ProductExceptionHandler {

    /**
     * 处理应用层业务异常（{@link BusinessException}）。
     * <p>
     * 本域该异常的抛出点<b>全部携带强类型 {@code BaseErrorCode}</b>（无裸串构造器用法），
     * 故不存在「取不到码」的分支，直接按码的类别定状态。
     * 改造前走「按码串反查本域枚举」，未命中即落 {@code systemError} 的<b>无条件 500</b>——
     * 于是 {@code SystemErrorCode} 里那些本就属调用方错误的码（如 {@code PARAM_INVALID}）
     * 也会被报成 500。
     * </p>
     *
     * @param exception 应用层业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        log.warn("[产品] 业务异常: errorCode={}, message={}",
                exception.getErrorCodeValue(), exception.getMessage());
        return ExceptionResponseFactory.of(exception);
    }

    /**
     * 处理领域规则异常（含聚合根/值对象抛出的各 {@code ProductXxxException}）。
     * <p>
     * {@link ProductMaintenanceOfferingException} 是本类的一个子类，其状态由 offering 错误码的语义类别
     * 决定（404 / 409 / 422 按 reason 分组），与改造前那张按 reason 的手写表<b>逐项等价</b>，
     * 故不再单列处理器——同一条语义只留一个出口。
     * </p>
     * <p>
     * 改造前的实现按「是否 {@code instanceof ProductErrorCode}」分两路，非产品码一路先按码串在本域枚举里
     * 反查、未命中则落 <b>422 + PARAM_INVALID</b>（状态与码自相矛盾：参数无效却是 422）。
     * 现由码的类别统一裁决，分支与反查一并消失。
     * </p>
     *
     * @param exception 领域异常
     * @return 统一错误响应
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException exception) {
        log.warn("[产品] 领域异常: errorCode={}, message={}",
                exception.getErrorCodeValue(), exception.getMessage());
        return ExceptionResponseFactory.of(exception);
    }

    /**
     * 兜底处理未预期异常（500 + 固定文案；原始消息与堆栈只进日志，不回显给调用方）。
     * <p>
     * 改造前本域<b>没有兜底处理器</b>，未预期异常的响应体是 Spring Boot 默认的
     * {@code {timestamp,status,error,path}}——<b>没有业务错误码</b>，且与其它域的 {@code ApiResponse}
     * 信封不一致，调用方（含 admin BFF）只能按域猜信封（审查报告 EXC-02）。
     * 边界处理器（common）接管的是 Spring MVC 框架异常，其类型集合不含 {@code Exception}，
     * 故本兜底不可省。
     * </p>
     *
     * @param exception 未预期异常
     * @return 500 + 通用系统错误码的统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("[产品] 未处理异常，请求处理失败", exception);
        return ExceptionResponseFactory.of(SystemErrorCode.SYSTEM_ERROR);
    }
}
