package com.titanium.product.common.constant;

import com.titanium.metadata.topic.CrossDomainTopics;

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
    public static final String TOPIC_PRODUCT_CREATED = CrossDomainTopics.PRODUCT_CREATED;
    public static final String TOPIC_PRODUCT_AUDITED = CrossDomainTopics.PRODUCT_AUDITED;
    public static final String TOPIC_PRODUCT_INVALIDATED = CrossDomainTopics.PRODUCT_INVALIDATED;
    // 原 TOPIC_PRODUCT_REVISED（titanium.product.revised）自声明起从无发布点，已删除（m5-903）。
    // 本域实际外发仅上列三个主题（见 ProductKafkaEventPublisher）；ProductRevisedEvent 不出域，
    // 修订后的版本信息经修订产品重新审批时的 ProductAuditedEvent 间接到达下游。
    // 若未来确需独立主题，须与消费端**成对建设**后重新登记进跨域事件目录，勿单侧新增孤儿主题。

    // ====== 缓存Key前缀 ======
    public static final String CACHE_KEY_PREFIX = "titanium:product:";
    public static final String CACHE_KEY_PRODUCT = CACHE_KEY_PREFIX + "detail:";
    public static final String CACHE_KEY_CONFIG = CACHE_KEY_PREFIX + "config:";

    // ====== 生命周期保费差额行描述（纯文案，红线20：落库业务描述禁止写死中文） ======
    /** 兼容历史确认计算的差额行描述 */
    public static final String LEGACY_DIFFERENCE_LINE_DESCRIPTION = "兼容历史确认计算的客户应付差额";

    /** 冲正差额行描述前缀（与原差额逐项守恒） */
    public static final String REVERSED_DIFFERENCE_LINE_PREFIX = "冲正: ";

    /** 兼容旧版计费的基础保费计算行描述 */
    public static final String LEGACY_BASE_PREMIUM_DESCRIPTION = "兼容基础保费";

    // ====== 定价方案回归测试结果描述（纯文案，红线20：载荷业务描述禁止写死中文） ======
    /** 用例实际保费与期望值差异超过容差的失败描述 */
    public static final String PRICING_TEST_CASE_TOLERANCE_EXCEEDED = "实际保费与期望值差异超过容差";
}
