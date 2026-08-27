package com.titanium.product.application.orchestration.pricing;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.SystemErrorCode;

/**
 * 定价输入与结果证据的确定性规范化和哈希服务。
 */
@Component
public class PricingEvidenceHasher {

    public String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(
                    digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException("运行环境不支持SHA-256", SystemErrorCode.SYSTEM_ERROR, exception);
        }
    }

    public String canonicalValue(Object value) {
        if (value == null) {
            return "*";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Map<?, ?> map) {
            StringJoiner joiner = new StringJoiner(",", "{", "}");
            map.entrySet().stream()
                    .sorted((left, right) -> String.valueOf(left.getKey()).compareTo(String.valueOf(right.getKey())))
                    .forEach(entry -> joiner.add(
                            String.valueOf(entry.getKey()) + ':' + canonicalValue(entry.getValue())));
            return joiner.toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            iterable.forEach(item -> joiner.add(canonicalValue(item)));
            return joiner.toString();
        }
        return String.valueOf(value);
    }
}
