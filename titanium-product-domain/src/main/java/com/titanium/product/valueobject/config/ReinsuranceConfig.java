package com.titanium.product.valueobject.config;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 再保险配置值对象 定义产品的自动分保策略
 *
 * @param autoReinsurance 是否自动分保
 * @param retentionLimit 自留保额上限
 * @param defaultContractCode 默认再保合约编码
 */
public record ReinsuranceConfig(boolean autoReinsurance, BigDecimal retentionLimit,
                                String defaultContractCode)
        implements
            Serializable {
}
