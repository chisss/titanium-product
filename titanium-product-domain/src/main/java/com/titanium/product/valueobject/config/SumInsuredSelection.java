package com.titanium.product.valueobject.config;

import com.titanium.product.common.enums.SumInsuredSelectionMode;

/**
 * 保额录入判定结果值对象（产品侧给出的「这条录入算选档还是自由填」的裁决）
 * <p>
 * 由 {@link CoverageSumInsuredConfig#resolveSelection(java.math.BigDecimal)} 产出，供投保单录入侧
 * （消费方）决定后续动作：{@link SumInsuredSelectionMode#TIER} 须回填 {@link #tier()} 的档位编码，
 * {@link SumInsuredSelectionMode#CUSTOM} 无档位可回填、只受区间约束。
 * </p>
 * <p>
 * <b>裁决权归属</b>：判定逻辑只能有一份实现（{@code CoverageSumInsuredConfig#resolveSelection}）。
 * 承保链路的其它环节<b>不得</b>各自再判一次「是否命中档位」——重复实现必然漂移，
 * 出现「产品侧算命中档位、投保侧算自由填」这类两侧结论不一致。
 * </p>
 *
 * @param mode 录入方式（选档 / 自由填）
 * @param tier 命中的档位；{@link SumInsuredSelectionMode#CUSTOM} 时恒为 {@code null}
 */
public record SumInsuredSelection(SumInsuredSelectionMode mode, SumInsuredTier tier) {

    /**
     * 紧凑构造器：保证「模式与档位的搭配」自洽
     * <p>
     * {@code TIER} 必带档位、{@code CUSTOM} 必不带档位，二者不可组合错配——
     * 否则调用方会拿到一个「自称选档却没有档位」的结果而在回填时空指针。
     * </p>
     */
    public SumInsuredSelection {
        if (mode == null) {
            throw new IllegalArgumentException("保额录入方式不能为空");
        }
        if (mode == SumInsuredSelectionMode.TIER && tier == null) {
            throw new IllegalArgumentException("选档结果必须携带命中的档位");
        }
        if (mode == SumInsuredSelectionMode.CUSTOM && tier != null) {
            throw new IllegalArgumentException("自由填结果不得携带档位");
        }
    }

    /**
     * 构造「选档」结果
     *
     * @param tier 命中的档位（非空）
     * @return 选档判定结果
     */
    public static SumInsuredSelection ofTier(SumInsuredTier tier) {
        return new SumInsuredSelection(SumInsuredSelectionMode.TIER, tier);
    }

    /**
     * 构造「自由填」结果
     *
     * @return 自由填判定结果
     */
    public static SumInsuredSelection ofCustom() {
        return new SumInsuredSelection(SumInsuredSelectionMode.CUSTOM, null);
    }
}
