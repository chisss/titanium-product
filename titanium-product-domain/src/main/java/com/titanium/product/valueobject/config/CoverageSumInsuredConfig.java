package com.titanium.product.valueobject.config;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.DomainException;

/**
 * 责任级保额档次配置值对象
 * <p>
 * 表达「某产品的某条保障责任，投保时保额可以怎么填」：可选档位集合 + 允许的保额区间 +
 * 是否开放自由填 + 默认档位。这是承保前置链路的输入契约——投保单录入侧据本配置裁决一条
 * 录入保额是「命中档位」还是「自由填」，见 {@link #resolveSelection(BigDecimal)}。
 * </p>
 * <p>
 * <b>为何按责任（coverageCode）而非按产品配置</b>：同一产品的不同责任保额量级天然不同
 * （如住院医疗 1 万起、重疾 10 万起），全产品共用一套档位会逼出「用不上的档位」与
 * 「不该允许的自由填」。产品级只保留「是否允许自定保额」这一开关的兜底语义。
 * </p>
 *
 * <h3>不变量（七条，全部内聚于本类与聚合根，不散落 application）</h3>
 * <ol>
 *   <li><b>责任唯一</b>：同一产品下 {@code coverageCode} 不得重复——由
 *       {@link #assertDistinctCoverageCodes(List)} 守，调用点是聚合根命令处理器
 *       （跨配置性质，单个值对象自身判不了）。</li>
 *   <li><b>档位非空</b>：{@code tiers} 不得为空——档位是「怎么填」的唯一定义，
 *       空档位等于没有配置，应改为不配置本责任的档次。</li>
 *   <li><b>档位单调</b>：{@code tiers} 须按 {@code sumInsured} <b>严格递增</b>且无重复
 *       （列表给定顺序即语义顺序，不接受乱序后自动排序——排序会掩盖录入侧的顺序错乱）。</li>
 *   <li><b>默认档位唯一存在</b>：{@code defaultTierCode} 必填，且<b>恰好</b>命中一个档位
 *       （蕴含 {@code tierCode} 在 {@code tiers} 内唯一）。</li>
 *   <li><b>区间自洽</b>：{@code minSumInsured} / {@code maxSumInsured} 均须大于 0，且 min ≤ max。</li>
 *   <li><b>档位落在区间内</b>：每个档位的保额 ∈ [min, max]（闭区间）——
 *       否则会出现「可选档位却填不进去」这种自相矛盾的配置。</li>
 *   <li><b>无自由空间时区间须与档位对齐</b>：{@code allowCustomAmount=false} 时，
 *       min 必须等于最小档、max 必须等于最大档——不开放自由填却留出档位之外的区间，
 *       会让调用方误以为那些值可以填，实际必被拒。</li>
 * </ol>
 * <p>
 * 🔴 六、七两条<b>必须分列</b>：⑥ 判「档位是否落在区间内」，⑦ 判「区间是否被档位撑满」。
 * 合法区间 {10万,100万} 配档位 {20万,50万} 时 ⑥ 通过而 ⑦ 失败，合并实现会让这条配置漏网。
 * </p>
 *
 * <h3>校验顺序</h3>
 * <p>
 * ② → ⑤ → ③ → ⑥ → ⑦ → ④。顺序非随意：⑤ 是 ⑥⑦ 的前提（区间自身不自洽时后两条无意义），
 * ③ 是 ⑦「最小/最大档」取首位与末位元素的前提。每条失败都有独立且可分辨的中文文案，
 * 便于运营按提示定位，也让「哪条不变量被触发」可被测试逐条断言。
 * </p>
 *
 * @param coverageCode       责任编码（同一产品下唯一）
 * @param tiers              档位清单（按保额严格递增，非空）
 * @param minSumInsured      允许的保额区间下界（大于 0）
 * @param maxSumInsured      允许的保额区间上界（≥ 下界）
 * @param allowCustomAmount  是否允许录入档位之外、区间之内的保额
 * @param defaultTierCode    默认档位编码（必填，须命中 {@code tiers} 中恰好一个）
 */
public record CoverageSumInsuredConfig(String coverageCode,
                                       List<SumInsuredTier> tiers,
                                       BigDecimal minSumInsured,
                                       BigDecimal maxSumInsured,
                                       boolean allowCustomAmount,
                                       String defaultTierCode) {

    /** 不变量 ②：档位非空 */
    private static final String MSG_TIERS_EMPTY = "保额档位不得为空";
    /** 不变量 ⑤：区间自洽 */
    private static final String MSG_RANGE_INVALID = "保额区间不合法";
    /** 不变量 ③：档位单调 */
    private static final String MSG_TIER_NOT_INCREASING = "档位保额必须严格递增且不得重复";
    /** 不变量 ⑥：档位落在区间内 */
    private static final String MSG_TIER_OUT_OF_RANGE = "档位保额超出保额区间";
    /** 不变量 ⑦：区间与档位对齐 */
    private static final String MSG_RANGE_NOT_ALIGNED = "未开放自由保额时保额区间必须与档位对齐";
    /** 不变量 ④：默认档位 */
    private static final String MSG_DEFAULT_TIER_INVALID = "默认档位不合法";
    /** 不变量 ①：责任唯一 */
    private static final String MSG_DUPLICATE_COVERAGE = "同一产品下责任编码重复配置保额档次";

    /**
     * 紧凑构造器：校验不变量 ②③④⑤⑥⑦
     * <p>
     * 错误码统一 {@link ProductErrorCode#COVERAGE_SUM_INSURED_CONFIG_INVALID}（类别
     * {@code PARAMETER_INVALID} → HTTP 400），消息以 {@code coverageCode} 开头便于定位到责任。
     * </p>
     */
    public CoverageSumInsuredConfig {
        if (coverageCode == null || coverageCode.isBlank()) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    "责任编码不能为空");
        }
        tiers = tiers == null ? null : List.copyOf(tiers);
        String prefix = "责任 " + coverageCode + " 的保额档次配置不合法：";

        // ② 档位非空
        if (tiers == null || tiers.isEmpty()) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    prefix + MSG_TIERS_EMPTY);
        }
        // ⑤ 区间自洽（⑥⑦ 的前提）
        if (minSumInsured == null || maxSumInsured == null
                || minSumInsured.compareTo(BigDecimal.ZERO) <= 0
                || maxSumInsured.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    prefix + MSG_RANGE_INVALID + "：上下界均须大于 0，min=" + minSumInsured + "，max=" + maxSumInsured);
        }
        if (minSumInsured.compareTo(maxSumInsured) > 0) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    prefix + MSG_RANGE_INVALID + "：下界不得大于上界，min=" + minSumInsured + "，max=" + maxSumInsured);
        }
        // ③ 档位严格递增（⑦ 取首末元素的前提）
        for (int i = 1; i < tiers.size(); i++) {
            if (tiers.get(i).sumInsured().compareTo(tiers.get(i - 1).sumInsured()) <= 0) {
                throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                        prefix + MSG_TIER_NOT_INCREASING + "：" + tiers.get(i - 1).tierCode()
                                + "=" + tiers.get(i - 1).sumInsured() + " 之后出现 "
                                + tiers.get(i).tierCode() + "=" + tiers.get(i).sumInsured());
            }
        }
        // ⑥ 每档落在 [min, max] 内
        for (SumInsuredTier tier : tiers) {
            if (tier.sumInsured().compareTo(minSumInsured) < 0 || tier.sumInsured().compareTo(maxSumInsured) > 0) {
                throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                        prefix + MSG_TIER_OUT_OF_RANGE + "：档位 " + tier.tierCode() + "=" + tier.sumInsured()
                                + " 不在 [" + minSumInsured + ", " + maxSumInsured + "] 内");
            }
        }
        // ⑦ 不开放自由填 ⇒ 区间须恰好由档位撑满
        if (!allowCustomAmount) {
            BigDecimal lowest = tiers.get(0).sumInsured();
            BigDecimal highest = tiers.get(tiers.size() - 1).sumInsured();
            if (minSumInsured.compareTo(lowest) != 0 || maxSumInsured.compareTo(highest) != 0) {
                throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                        prefix + MSG_RANGE_NOT_ALIGNED + "：区间为 [" + minSumInsured + ", " + maxSumInsured
                                + "]，档位区间为 [" + lowest + ", " + highest + "]");
            }
        }
        // ④ 默认档位必填且恰命中一档（蕴含 tierCode 唯一）
        if (defaultTierCode == null || defaultTierCode.isBlank()) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    prefix + MSG_DEFAULT_TIER_INVALID + "：默认档位编码不能为空");
        }
        long matched = tiers.stream().filter(tier -> defaultTierCode.equals(tier.tierCode())).count();
        if (matched != 1) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    prefix + MSG_DEFAULT_TIER_INVALID + "：默认档位 " + defaultTierCode
                            + " 在档位清单中命中 " + matched + " 次（须恰好 1 次），档位编码=" + joinTierCodes(tiers));
        }
    }

    /**
     * 不变量 ①：同一产品下责任编码不得重复
     * <p>
     * 跨配置性质，值对象自身判不了，故由聚合根在命令处理时调用本方法。空清单合法——
     * 「本产品暂不配置任何责任的保额档次」是允许的中间状态（配置项可分批录入）。
     * </p>
     *
     * @param configs 该产品下的全部责任保额档次配置（可为 null / 空）
     */
    public static void assertDistinctCoverageCodes(List<CoverageSumInsuredConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (CoverageSumInsuredConfig config : configs) {
            if (!seen.add(config.coverageCode())) {
                throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                        MSG_DUPLICATE_COVERAGE + "：coverageCode=" + config.coverageCode());
            }
        }
    }

    /**
     * 裁决一条录入保额属于「选档」还是「自由填」（保额录入侧判定的唯一实现）
     * <p>
     * 判定次序：先精确匹配档位（命中即选档，无论是否开放自由填）→ 未命中且不开放自由填则拒 →
     * 未命中且开放自由填则再判是否落在区间内。先判档位再判区间，是为了让「不开放自由填」场景下的
     * 报错点明「未命中任何档位」而非笼统的「超出区间」——前者才能指导运营去选档。
     * </p>
     *
     * @param sumInsured 投保单录入的保额
     * @return 判定结果（选档带档位 / 自由填无档位）
     * @throws DomainException 保额为空或非正、未命中档位且不开放自由填、或超出允许区间
     */
    public SumInsuredSelection resolveSelection(BigDecimal sumInsured) {
        if (sumInsured == null || sumInsured.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    "责任 " + coverageCode + " 录入保额必须大于 0，实际=" + sumInsured);
        }
        for (SumInsuredTier tier : tiers) {
            if (tier.sumInsured().compareTo(sumInsured) == 0) {
                return SumInsuredSelection.ofTier(tier);
            }
        }
        if (!allowCustomAmount) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    "责任 " + coverageCode + " 未开放自由保额，录入保额 " + sumInsured
                            + " 未命中任何档位，可选档位=" + tierCodes());
        }
        if (sumInsured.compareTo(minSumInsured) < 0 || sumInsured.compareTo(maxSumInsured) > 0) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    "责任 " + coverageCode + " 录入保额 " + sumInsured + " 超出允许区间 ["
                            + minSumInsured + ", " + maxSumInsured + "]");
        }
        return SumInsuredSelection.ofCustom();
    }

    /**
     * 取默认档位。
     *
     * @return 默认档位（构造器已保证存在且唯一）
     */
    public SumInsuredTier defaultTier() {
        return tiers.stream()
                .filter(tier -> defaultTierCode.equals(tier.tierCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("默认档位缺失，构造器不变量被绕过：" + defaultTierCode));
    }

    /**
     * 档位编码清单（用于错误文案与日志取证，不含保额）。
     *
     * @return 形如 {@code [T1, T2, T3]} 的编码清单
     */
    public String tierCodes() {
        return joinTierCodes(this.tiers);
    }

    /**
     * 拼接档位编码清单。
     * <p>
     * 🔴 必须是<b>静态</b>方法：紧凑构造器体内 record 的字段尚未赋值（编译器在构造器末尾才把形参写入字段），
     * 此时调 {@link #tierCodes()} 会读到 {@code null} 字段并抛 NPE，把「配置不合法」的领域异常
     * 伪造成 NPE。构造器内一律用形参，故此处按形参入参。
     * </p>
     *
     * @param tiers 档位清单（可为 null）
     * @return 形如 {@code [T1, T2, T3]} 的编码清单；null 时返回 {@code []}
     */
    private static String joinTierCodes(List<SumInsuredTier> tiers) {
        if (tiers == null) {
            return "[]";
        }
        return tiers.stream().map(SumInsuredTier::tierCode).collect(Collectors.joining(", ", "[", "]"));
    }
}
