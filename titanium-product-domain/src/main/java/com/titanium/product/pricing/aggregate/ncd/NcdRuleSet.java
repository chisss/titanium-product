package com.titanium.product.pricing.aggregate.ncd;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.ncd.NcdLookbackWindow;

import lombok.Getter;

/**
 * Product 版本化 NCD（无赔款优待）规则集
 * <p>
 * G11/g11-03 的**规则本体**：把「历史赔案次数 → NCD 系数」的反算口径做成可配置的独立规则表
 * （不写死在 policy、不复用费率表的 {@code PayoutRule}/{@code PremiumRule}），并按产品 + 版本 +
 * 有效期管理，供出单时按业务时点取用。
 * </p>
 * <p>
 * 🔴 <b>R-3 业务闸：本聚合只建结构与契约，不承载任何规则值。</b>档位切分（0/1/2/3+ 还是 0/1/2/3/4+）、
 * 系数取值、统计区间长度（{@code lookbackMonths}）、是否按车型分档（{@code vehicleType}）四项口径
 * 业务均未确认，故：不随 DDL 落任何种子数据、不在本类内设任何默认档位或默认系数。业务确认后由
 * 配置面写入，本类代码无须改动。
 * </p>
 * <p>
 * 🔴 <b>绝不回落默认系数</b>：{@link #resolve(int, String)} 在「无档位命中」与「多档位同时命中」
 * 两种情形下分别抛出显式错误，不返回任何兜底值（尤其不得返回 1.0）——1.0 是「无赔款优待不适用」
 * 这一**业务结论**，只能由配置了该档位的规则集给出，不能由代码在没有依据时替业务决定。
 * </p>
 * <p>
 * 生命周期与退保价值策略/保单贷款策略等同构：DRAFT → APPROVED（此时固化内容指纹）→ PUBLISHED
 * → RETIRED，复用 {@link ActuarialDefinitionStatus} 统一精算配置状态，不另立枚举。
 * </p>
 */
@Getter
public final class NcdRuleSet {

    private final String ruleSetId;
    private final String productId;
    private final String ruleSetCode;
    private final String ruleSetVersion;
    /**
     * 历史赔案统计区间长度（月）。
     * <p>
     * 🔴 具体口径（回看 12 个月 / 当前保单年度 / 固定自然年）属 R-3 待确认项，本聚合只承载
     * 「区间长度可配置」这一能力，不预设取值。
     * </p>
     */
    private final int lookbackMonths;
    private final List<NcdRule> rules;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final String tenantId;
    private ActuarialDefinitionStatus status;
    private String contentHash;

    private NcdRuleSet(
            String ruleSetId,
            String productId,
            String ruleSetCode,
            String ruleSetVersion,
            int lookbackMonths,
            List<NcdRule> rules,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        this.ruleSetId = requireText(ruleSetId, "NCD 规则集ID");
        this.productId = requireText(productId, "产品ID");
        this.ruleSetCode = requireText(ruleSetCode, "NCD 规则集编码").toUpperCase(Locale.ROOT);
        this.ruleSetVersion = requireText(ruleSetVersion, "NCD 规则集版本");
        if (lookbackMonths < 1) {
            throw invalid("统计区间长度必须至少为1个月");
        }
        this.lookbackMonths = lookbackMonths;
        if (rules == null || rules.isEmpty()) {
            // 🔴 空规则集不是「无 NCD 适用」，而是「永远反算不出结果」的配置——失败前置到构造期，
            // 不让它流到 resolve 时再以「档位未命中」暴露（那会误导运营去补档位而非换规则集）
            throw invalid("NCD 规则集必须至少包含一个档位");
        }
        this.rules = List.copyOf(rules);
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.tenantId = requireText(tenantId, "租户ID");
        this.status = Objects.requireNonNull(status, "生命周期状态不能为空");
        this.contentHash = contentHash == null ? "" : contentHash;
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid("失效时间必须晚于生效时间");
        }
    }

    public static NcdRuleSet createDraft(
            String ruleSetId,
            String productId,
            String ruleSetCode,
            String ruleSetVersion,
            int lookbackMonths,
            List<NcdRule> rules,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId) {
        return new NcdRuleSet(ruleSetId, productId, ruleSetCode, ruleSetVersion, lookbackMonths, rules,
                effectiveFrom, effectiveTo, tenantId, ActuarialDefinitionStatus.DRAFT, "");
    }

    public static NcdRuleSet restore(
            String ruleSetId,
            String productId,
            String ruleSetCode,
            String ruleSetVersion,
            int lookbackMonths,
            List<NcdRule> rules,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        return new NcdRuleSet(ruleSetId, productId, ruleSetCode, ruleSetVersion, lookbackMonths, rules,
                effectiveFrom, effectiveTo, tenantId, status, contentHash);
    }

    public String approve() {
        requireStatus(ActuarialDefinitionStatus.DRAFT, "只有草稿 NCD 规则集可以审批");
        contentHash = hash(canonicalContent());
        status = ActuarialDefinitionStatus.APPROVED;
        return contentHash;
    }

    public void publish() {
        requireStatus(ActuarialDefinitionStatus.APPROVED, "只有已审批 NCD 规则集可以发布");
        status = ActuarialDefinitionStatus.PUBLISHED;
    }

    public void retire() {
        requireStatus(ActuarialDefinitionStatus.PUBLISHED, "只有已发布 NCD 规则集可以退役");
        status = ActuarialDefinitionStatus.RETIRED;
    }

    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return status == ActuarialDefinitionStatus.PUBLISHED && businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }

    /**
     * 按业务日派生历史赔案统计区间（日粒度半开区间 {@code [from, to)}）。
     * <p>
     * {@code from = 业务日.minusMonths(lookbackMonths).atStartOfDay()}（含），
     * {@code to = 业务日.plusDays(1).atStartOfDay()}（不含）——业务日当天发生的赔案计入统计。
     * </p>
     *
     * @param businessDate 业务日（出单/试算的业务时点所在日）
     */
    public NcdLookbackWindow lookbackWindow(LocalDate businessDate) {
        LocalDate date = Objects.requireNonNull(businessDate, "业务日不能为空");
        return new NcdLookbackWindow(date.minusMonths(lookbackMonths).atStartOfDay(),
                date.plusDays(1).atStartOfDay());
    }

    /**
     * 按赔案次数与车型反算命中的档位。
     * <p>
     * 命中判据 = {@code 档位适用该车型} ∧ {@code 次数落在档位闭区间内}。三种结局：
     * </p>
     * <ul>
     *   <li><b>恰好一条命中</b>：返回该档位，其系数即为反算结果；</li>
     *   <li><b>零条命中</b>：抛 {@code NCD_BAND_NOT_MATCHED}——规则配置存在缺口，
     *       <b>不回落任何默认系数</b>，须补齐档位；</li>
     *   <li><b>多条命中</b>：抛 {@code NCD_RULE_CONFLICT}——同车型档内区间重叠，配置自相矛盾。
     *       <b>不按声明顺序取首个命中</b>：那会让结果依赖录入顺序，同一条数据换序即换系数。</li>
     * </ul>
     *
     * @param claimCount  统计区间内的历史赔案次数（≥ 0）
     * @param vehicleType 车型标识；{@code null} 表示不按车型取档（此时只有通用档可命中）
     * @return 命中的档位
     */
    public NcdRule resolve(int claimCount, String vehicleType) {
        if (claimCount < 0) {
            throw invalid("历史赔案次数不能为负数");
        }
        List<NcdRule> matched = new ArrayList<>();
        for (NcdRule rule : rules) {
            if (rule.appliesTo(vehicleType) && rule.covers(claimCount)) {
                matched.add(rule);
            }
        }
        if (matched.isEmpty()) {
            throw new PricingDomainException(ProductErrorCode.NCD_BAND_NOT_MATCHED,
                    "赔案次数 " + claimCount + " 未命中规则集 " + ruleSetCode + "/" + ruleSetVersion
                            + " 的任何档位（车型=" + (vehicleType == null ? "未指定" : vehicleType)
                            + "），须补齐档位，不得回落默认系数");
        }
        if (matched.size() > 1) {
            throw new PricingDomainException(ProductErrorCode.NCD_RULE_CONFLICT,
                    "赔案次数 " + claimCount + " 命中规则集 " + ruleSetCode + "/" + ruleSetVersion
                            + " 的 " + matched.size() + " 个档位（车型=" + (vehicleType == null ? "未指定" : vehicleType)
                            + "），档位区间重叠，须消歧后再反算");
        }
        return matched.get(0);
    }

    /**
     * 规则集内容指纹的规范化文本。
     * <p>
     * 🔴 档位按（车型、下界、上界、系数）**排序后**拼接，与声明顺序无关：同一条规则集换行序
     * 不应产生不同指纹，否则「内容未变却判为新版本」会让出单快照的规则版本失去可比性。
     * </p>
     */
    private String canonicalContent() {
        List<String> ruleTexts = new ArrayList<>();
        for (NcdRule rule : rules) {
            ruleTexts.add(rule.bandKey() + "," + rule.coefficient().stripTrailingZeros().toPlainString());
        }
        ruleTexts.sort(Comparator.naturalOrder());
        return String.join("|", productId, ruleSetCode, ruleSetVersion, Integer.toString(lookbackMonths),
                String.join(";", ruleTexts),
                effectiveFrom.toString(), effectiveTo == null ? "*" : effectiveTo.toString(), tenantId);
    }

    private void requireStatus(ActuarialDefinitionStatus expected, String message) {
        if (status != expected) {
            throw invalid(message);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + "不能为空");
        }
        return value.trim();
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持SHA-256", exception);
        }
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.NCD_RULE_INVALID, detail);
    }
}
