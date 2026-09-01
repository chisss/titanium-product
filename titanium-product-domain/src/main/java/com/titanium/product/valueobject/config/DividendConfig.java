package com.titanium.product.valueobject.config;

import java.io.Serializable;
import java.math.BigDecimal;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 分红配置值对象（分红险专属）
 * <p>
 * 定义分红险产品的红利分配方式与红利演示假设。监管要求分红险销售时须以低/中/高三档演示利率
 * 向客户展示红利利益，且不得承诺高档必然实现。本值对象作为 {@code ProductTemplate} 的行为配置字段，
 * 经 {@code UpdateProductTemplateCommand} 写入。
 * </p>
 *
 * @param distribution 红利分配方式（现金/累积生息/购买交清增额/抵缴保费）
 * @param lowDemoRate 低档演示利率（如 0.015 表示 1.5%）
 * @param midDemoRate 中档演示利率
 * @param highDemoRate 高档演示利率
 */
public record DividendConfig(ProductEnum.DividendDistribution distribution, BigDecimal lowDemoRate,
                             BigDecimal midDemoRate, BigDecimal highDemoRate)
        implements
            Serializable {

    /**
     * 校验演示利率的单调性：低档 ≤ 中档 ≤ 高档。三档任一为空时跳过对应校验（配置尚不完整）。
     *
     * @return true 表示三档演示利率满足低≤中≤高
     */
    public boolean isDemoRateMonotonic() {
        if (lowDemoRate == null || midDemoRate == null || highDemoRate == null) {
            return true;
        }
        return lowDemoRate.compareTo(midDemoRate) <= 0 && midDemoRate.compareTo(highDemoRate) <= 0;
    }
}
