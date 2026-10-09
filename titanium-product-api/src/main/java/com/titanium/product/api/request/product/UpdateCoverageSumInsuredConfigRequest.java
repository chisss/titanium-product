package com.titanium.product.api.request.product;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新责任级保额档次配置远程契约（上游 → product）
 *
 * <p><b>整体替换</b>语义：提交的清单即替换后的全部配置，不是增量。供其它微服务程序化改写时调用
 * （后台/端上的同名能力走 web 层 {@code PUT /web/v1/products/{productId}/coverage-sum-insured-configs}，
 * 两条入口平行收敛到同一个 application 薄门面，互不依赖）。</p>
 *
 * <p>api 层自包含：档次结构在此以内嵌 record 承载，不复用 web 层类型（项目 CLAUDE.md §3.4.11 ①）。</p>
 *
 * <p>🔴 <b>本契约刻意不承载「选档 / 自由填」的裁决结果</b>：裁决由领域值对象
 * {@code CoverageSumInsuredConfig#resolveSelection} 依档位与区间算出，属派生态而非输入态。
 * 若在此要求调用方传结果，就会出现「调用方算一套、服务端算一套」的两套口径。</p>
 */
@Data
public class UpdateCoverageSumInsuredConfigRequest {

    /**
     * 替换后的全部责任档次配置
     * <p>
     * 🔴 {@code @NotNull} 而非 {@code @NotEmpty}：<b>空清单是合法的「清空」</b>，{@code null} 才是非法输入
     * （清空请显式传 {@code []}）。聚合根对 {@code null} 也拒，此处前置拦截只为让调用方拿到 400 而非 500。
     * </p>
     */
    @NotNull(message = "coverageSumInsuredConfigs 不得为 null，清空请传空清单")
    @Valid
    private List<CoverageSumInsuredConfigItem> coverageSumInsuredConfigs;

    /**
     * 单个责任的保额档次配置
     *
     * <p>七条不变量（档位非空 / 区间合法 / 档位严格递增 / 档位不越界 / 区间与档位对齐 / 默认档位合法 /
     * 责任编码不重复）一律由服务端裁决，本契约<b>不重复表达</b>——同一判断两处各写一份必然漂移
     * （项目 CLAUDE.md §3.4.11）。</p>
     *
     * @param coverageCode      责任编码（须落在该产品已绑定条款的责任集合内，服务端向条款域求证）
     * @param tiers             保额档位清单
     * @param minSumInsured     保额区间下界（含）
     * @param maxSumInsured     保额区间上界（含）
     * @param allowCustomAmount 是否开放自由填；🔴 <b>必填</b>——领域侧为基本类型 {@code boolean}，
     *                          若缺省则映射器会静默兜底为 {@code false}（即「只许选档」），
     *                          把调用方的漏传变成一个看不见的业务语义默认值，故在此前置拒空
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
