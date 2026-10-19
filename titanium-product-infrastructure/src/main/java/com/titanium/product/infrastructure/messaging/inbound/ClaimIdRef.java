package com.titanium.product.infrastructure.messaging.inbound;

/**
 * 赔案ID线格式承接（防腐）
 *
 * <p>理赔域出站事件中的 {@code ClaimId} 是值对象 record {@code record ClaimId(String value)}，
 * fastjson2 序列化为 {@code {"value":"..."}}。本 record 只承接线格式，<b>不依赖理赔域类型</b>
 * （防腐层的意义即在与对端的报文形态耦合、与对端代码解耦）。</p>
 *
 * @param value 赔案ID字面值
 */
public record ClaimIdRef(String value) {
}
