package com.titanium.product.web.dto.product;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新责任级保额档次配置请求（后台/端上 HTTP 入参）
 *
 * <p><b>整体替换</b>语义：提交的清单即替换后的全部配置，不是增量。此前「责任级保额档次」在领域侧
 * 无任何承载（保额范围只到产品模板级的 {@code LifeProductSpec.SumInsuredRange} 单个区间），
 * 本条把「每个责任各自一套档位」的写入入口补齐（web 侧）。</p>
 *
 * <p>🔴 空清单<b>是</b>合法的「清空」，{@code null} 才是非法输入：聚合根对 {@code null} 明确拒绝
 * （文档化的「清空请传 {@code []}」），故此处 {@code @NotNull} 与之对齐。这与条款绑定
 * （{@code UpdateProductClauseRelDTO} 用 {@code @NotEmpty}）刻意不同——绑定非空是聚合硬不变量，
 * 而档次配置允许清空。</p>
 *
 * <p>🔴 <b>金额用 {@code BigDecimal} 而非 {@code Money}</b>：对齐本域既有口径
 * （{@code LifeProductSpec.SumInsuredRange} 的上下界即 {@code BigDecimal}）。{@code Money} 承担
 * 币种语义，而本域保额档次的币种由产品/保单口径统一决定，在此承载会引入第二个币种来源。</p>
 *
 * <p>七条档次不变量一律由服务端（领域值对象 {@code CoverageSumInsuredConfig}）裁决，本层<b>不复制</b>；
 * 此处仅做「形状」级前置拦截，目的是让调用方拿到 400 而非 500。</p>
 */
@Data
public class UpdateCoverageSumInsuredConfigDTO {

    /** 替换后的全部责任档次配置（不得为 null；空清单表示清空） */
    @NotNull(message = "coverageSumInsuredConfigs 不得为 null，清空请传空清单")
    @Valid
    private List<CoverageSumInsuredConfigItem> coverageSumInsuredConfigs;

    /**
     * 单个责任的保额档次配置
     *
     * @param coverageCode      责任编码（须落在该产品已绑定条款的责任集合内，服务端向条款域求证）
     * @param tiers             保额档位清单
     * @param minSumInsured     保额区间下界（含）
     * @param maxSumInsured     保额区间上界（含）
     * @param allowCustomAmount 是否开放自由填；🔴 必填——领域侧为基本类型 {@code boolean}，
     *                          若缺省则映射器会静默兜底为 {@code false}（即「只许选档」），
     *                          把漏传变成一个看不见的业务语义默认值，故在此前置拒空
     * @param defaultTierCode   默认档位编码（须命中 {@code tiers} 中的某一项）
     */
    public record CoverageSumInsuredConfigItem(@NotBlank(message = "coverageCode 不得为空") String coverageCode,
                                               List<SumInsuredTierItem> tiers,
                                               BigDecimal minSumInsured,
                                               BigDecimal maxSumInsured,
                                               @NotNull(message = "allowCustomAmount 不得为 null")
                                               Boolean allowCustomAmount,
                                               String defaultTierCode) {
    }

    /**
     * 保额档位条目
     *
     * @param tierCode     档位编码
     * @param sumInsured   档位保额（须大于 0）
     * @param allowedTerms 该档位允许的保障期限
     * @param sortNo       展示排序号
     */
    public record SumInsuredTierItem(String tierCode, BigDecimal sumInsured, List<Integer> allowedTerms,
                                     Integer sortNo) {
    }
}
