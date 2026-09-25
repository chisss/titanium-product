package com.titanium.product.common.errorcode;

import com.titanium.metadata.errorcode.BaseErrorCode;
import com.titanium.metadata.errorcode.ErrorCategory;

import lombok.Getter;

/** Product 保全 Offering 对外业务错误码。 */
@Getter
public enum ProductMaintenanceOfferingErrorCode implements BaseErrorCode {
    NOT_FOUND("60000200", "保全Offering不存在", "不存在匹配产品、计划和业务时点的已发布Offering"),
    ALREADY_EXISTS("60000201", "保全Offering版本已存在", "同一产品和计划下的Offering版本必须唯一"),
    VERSION_MISMATCH("60000202", "保全Offering版本不匹配", "产品版本或定价计划版本与Offering不一致"),
    NOT_APPLICABLE("60000203", "保全Offering不适用", "保单状态或受理渠道不在Offering允许范围内"),
    PERIOD_CONFLICT("60000204", "保全Offering有效期冲突", "同一产品和计划的已发布Offering有效期不能重叠"),
    STATUS_INVALID("60000205", "保全Offering状态错误", "当前Offering生命周期状态不允许此操作"),
    CONTRACT_INVALID("60000206", "保全Offering契约无效", "Offering配置或权威响应不完整");

    private final String code;
    private final String message;
    private final String description;

    ProductMaintenanceOfferingErrorCode(String code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    /**
     * 错误码的语义类别 → HTTP 状态的唯一依据（{@code ErrorCategoryHttpStatus} 按此映射）。
     * <p>
     * 🔴 <b>本枚举此前未覆写本方法</b>，即全部继承 {@code BaseErrorCode} 的默认值
     * {@link ErrorCategory#INTERNAL_FAILURE}——凡进入统一边界/工厂的路径都会把「Offering 不存在」
     * 「版本冲突」这类调用方错误一律渲染成 <b>500</b>，且文案被 5xx 规则替换成码表文案，
     * 调用方既拿不到状态语义、也看不到细节。改造前之所以没有暴露，是因为 Web 层另写了一张
     * 按 {@code reason} 的手写状态表兜住了它——那张表属第二份事实来源，已随本方法落地删除。
     * </p>
     * <p>
     * 映射与删除前的手写表<b>逐项等价</b>：NOT_FOUND→404；ALREADY_EXISTS / PERIOD_CONFLICT /
     * STATUS_INVALID→409；VERSION_MISMATCH / NOT_APPLICABLE / CONTRACT_INVALID→422。
     * 刻意不写 {@code default}：本枚举新增常量而不表态类别即编译失败。
     * </p>
     *
     * @return 语义类别
     */
    @Override
    public ErrorCategory category() {
        return switch (this) {
            case NOT_FOUND -> ErrorCategory.RESOURCE_NOT_FOUND;
            case ALREADY_EXISTS, PERIOD_CONFLICT, STATUS_INVALID -> ErrorCategory.STATE_CONFLICT;
            case VERSION_MISMATCH, NOT_APPLICABLE, CONTRACT_INVALID -> ErrorCategory.BUSINESS_RULE_VIOLATION;
        };
    }
}
