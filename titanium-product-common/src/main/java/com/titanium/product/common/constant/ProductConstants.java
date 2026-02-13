package com.titanium.product.common.constant;

/**
 * 产品域常量定义
 */
public final class ProductConstants {

    private ProductConstants() {
    }

    /** 默认产品初始版本 */
    public static final String INITIAL_VERSION = "V1.0";

    /** 默认租户ID */
    public static final String DEFAULT_TENANT_ID = "default";

    // ====== Kafka Topic ======
    public static final String TOPIC_PRODUCT_CREATED = "titanium.product.created";
    public static final String TOPIC_PRODUCT_AUDITED = "titanium.product.audited";
    public static final String TOPIC_PRODUCT_INVALIDATED = "titanium.product.invalidated";
    public static final String TOPIC_PRODUCT_REVISED = "titanium.product.revised";

    // ====== 缓存Key前缀 ======
    public static final String CACHE_KEY_PREFIX = "titanium:product:";
    public static final String CACHE_KEY_PRODUCT = CACHE_KEY_PREFIX + "detail:";
    public static final String CACHE_KEY_CONFIG = CACHE_KEY_PREFIX + "config:";
}
