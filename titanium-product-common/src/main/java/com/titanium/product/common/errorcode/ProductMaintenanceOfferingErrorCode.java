package com.titanium.product.common.errorcode;

import com.titanium.metadata.errorcode.BaseErrorCode;

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
}
