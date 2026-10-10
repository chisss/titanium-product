package com.titanium.product.valueobject.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.errorcode.ErrorCategory;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.DomainException;
import com.titanium.product.common.enums.SumInsuredSelectionMode;

/**
 * 责任级保额档次配置契约测试（G01/g01-03，AC-04）
 * <p>
 * <b>AC-04 正反成对</b>：
 * </p>
 * <ul>
 * <li><b>正</b>：某责任配置 3 档、区间 {@code [10万, 100万]}、允许自由填 ⇒ 可构造并可查回；</li>
 * <li><b>反</b>：七条不变量<b>各一条</b>拒绝用例。其中 ⑥「档位超出区间」与 ⑦「区间未与档位对齐」
 * <b>必须各自独立</b>——二者是「档位落在区间内」与「区间被档位撑满」两个方向不同的性质，
 * 合并实现会让 {区间 [10万,100万] + 档位 [20万,50万] + 不允许自由填} 这条配置漏网。</li>
 * </ul>
 * <p>
 * 🔴 <b>为何逐条断言错误码与消息片段而非只断言「抛异常」</b>：七条不变量共用同一个错误码
 * {@link ProductErrorCode#COVERAGE_SUM_INSURED_CONFIG_INVALID}，只断言异常类型的话，
 * 「所有用例其实都栽在同一条不变量上」也会全绿——判据必须指向<b>被验的那条语义</b>。
 * </p>
 * <p>
 * <b>金额类型</b>：{@code BigDecimal}（用户 2026-09-26 裁决，与本域 {@code LifeProductSpec.SumInsuredRange}
 * 对齐）。故用例一律用 {@code new BigDecimal("100000")} 而非 {@code double} 字面量——后者会把
 * 「同一个数值」表达成不同 scale，令 {@code compareTo} 之外的判据出现假红。
 * </p>
 */
class CoverageSumInsuredConfigTest {

    /** 万元常量，避免用例里散落 5 个零 */
    private static final BigDecimal WAN = new BigDecimal("10000");

    private static BigDecimal wan(String amount) {
        return new BigDecimal(amount).multiply(WAN);
    }

    /** AC-04 正向样例：责任 HOSPITAL 三档，区间 [10万, 100万]，允许自由填 */
    private static CoverageSumInsuredConfig threeTierConfig() {
        return new CoverageSumInsuredConfig("HOSPITAL",
                List.of(new SumInsuredTier("T1", wan("10"), List.of(1), 1),
                        new SumInsuredTier("T2", wan("30"), List.of(1, 5), 2),
                        new SumInsuredTier("T3", wan("100"), List.of(1, 5, 10), 3)),
                wan("10"), wan("100"), true, "T2");
    }

    /** 断言失败点落在指定语义上：错误码须为保额档次配置码，且消息含期望片段 */
    private static void assertRejectedWith(String expectedFragment, Runnable action) {
        DomainException exception = assertThrows(DomainException.class, action::run);
        assertEquals(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID, exception.getErrorCode(),
                "失败点须落在保额档次配置错误码上，实际=" + exception.getErrorCode());
        assertEquals(ErrorCategory.PARAMETER_INVALID, exception.getErrorCode().category(),
                "保额档次配置不合法属参数不合法，须映射 4xx 而非 500");
        assertTrue(exception.getMessage().contains(expectedFragment),
                "失败消息须指向被验语义「" + expectedFragment + "」，实际=" + exception.getMessage());
    }

    // ==================== 正向 ====================

    @Test
    @DisplayName("正向：责任三档 + 区间 [10万,100万] + 允许自由填，构造成功且可用")
    void acceptsThreeTierConfigWithCustomAmount() {
        CoverageSumInsuredConfig config = threeTierConfig();

        assertEquals("HOSPITAL", config.coverageCode());
        assertEquals(3, config.tiers().size());
        assertTrue(config.allowCustomAmount());
        assertEquals("T2", config.defaultTier().tierCode(), "默认档位须可按编码取回");
        assertEquals("[T1, T2, T3]", config.tierCodes());
    }

    @Test
    @DisplayName("正向：档位允许期限为空列表（不按档位限制期限）")
    void acceptsTierWithoutAllowedTerms() {
        CoverageSumInsuredConfig config = new CoverageSumInsuredConfig("HOSPITAL",
                List.of(new SumInsuredTier("T1", wan("10"), null, 1)),
                wan("10"), wan("10"), false, "T1");

        assertEquals(List.of(), config.tiers().get(0).allowedTerms(), "null 期限须归一为空列表");
    }

    // ==================== 反向：七条不变量各一条 ====================

    @Test
    @DisplayName("① 同产品下责任编码重复 ⇒ 拒绝")
    void rejectsDuplicateCoverageCode() {
        CoverageSumInsuredConfig first = threeTierConfig();
        CoverageSumInsuredConfig second = new CoverageSumInsuredConfig("HOSPITAL",
                List.of(new SumInsuredTier("T1", wan("20"), null, 1)),
                wan("20"), wan("20"), false, "T1");

        assertRejectedWith("责任编码重复",
                () -> CoverageSumInsuredConfig.assertDistinctCoverageCodes(List.of(first, second)));
    }

    @Test
    @DisplayName("② 档位清单为空 ⇒ 拒绝")
    void rejectsEmptyTiers() {
        assertRejectedWith("档位不得为空",
                () -> new CoverageSumInsuredConfig("HOSPITAL", List.of(), wan("10"), wan("100"), true, "T1"));
    }

    @Test
    @DisplayName("③ 档位保额非严格递增（重复 / 递减）⇒ 拒绝")
    void rejectsNonIncreasingTiers() {
        assertRejectedWith("严格递增",
                () -> new CoverageSumInsuredConfig("HOSPITAL",
                        List.of(new SumInsuredTier("T1", wan("30"), null, 1),
                                new SumInsuredTier("T2", wan("30"), null, 2)),
                        wan("10"), wan("100"), true, "T1"));
        assertRejectedWith("严格递增",
                () -> new CoverageSumInsuredConfig("HOSPITAL",
                        List.of(new SumInsuredTier("T1", wan("50"), null, 1),
                                new SumInsuredTier("T2", wan("20"), null, 2)),
                        wan("10"), wan("100"), true, "T1"));
    }

    @Test
    @DisplayName("④ 默认档位缺失 / 不在档位清单中 ⇒ 拒绝")
    void rejectsInvalidDefaultTier() {
        assertRejectedWith("默认档位",
                () -> new CoverageSumInsuredConfig("HOSPITAL",
                        List.of(new SumInsuredTier("T1", wan("20"), null, 1)),
                        wan("10"), wan("100"), true, null));
        assertRejectedWith("默认档位",
                () -> new CoverageSumInsuredConfig("HOSPITAL",
                        List.of(new SumInsuredTier("T1", wan("20"), null, 1)),
                        wan("10"), wan("100"), true, "T9"));
    }

    @Test
    @DisplayName("⑤ 区间下界非正 / 下界大于上界 ⇒ 拒绝")
    void rejectsInvalidRange() {
        assertRejectedWith("上下界均须大于 0",
                () -> new CoverageSumInsuredConfig("HOSPITAL",
                        List.of(new SumInsuredTier("T1", wan("20"), null, 1)),
                        BigDecimal.ZERO, wan("100"), true, "T1"));
        assertRejectedWith("下界不得大于上界",
                () -> new CoverageSumInsuredConfig("HOSPITAL",
                        List.of(new SumInsuredTier("T1", wan("20"), null, 1)),
                        wan("100"), wan("10"), true, "T1"));
    }

    @Test
    @DisplayName("⑥ 档位保额超出区间 ⇒ 拒绝（🔴 独立用例，不与⑤合并）")
    void rejectsTierOutsideRange() {
        // 区间 [10万,100万] 自洽（⑤ 通过）、档位单调（③ 通过），仅「5 万档落在区间外」这一条被触发
        assertRejectedWith("档位保额超出保额区间",
                () -> new CoverageSumInsuredConfig("HOSPITAL",
                        List.of(new SumInsuredTier("T1", wan("5"), null, 1),
                                new SumInsuredTier("T2", wan("20"), null, 2)),
                        wan("10"), wan("100"), true, "T1"));
        assertRejectedWith("档位保额超出保额区间",
                () -> new CoverageSumInsuredConfig("HOSPITAL",
                        List.of(new SumInsuredTier("T1", wan("20"), null, 1),
                                new SumInsuredTier("T2", wan("200"), null, 2)),
                        wan("10"), wan("100"), true, "T1"));
    }

    @Test
    @DisplayName("⑦ 不开放自由填但区间未与档位对齐 ⇒ 拒绝（🔴 独立用例，不与⑤⑥合并）")
    void rejectsRangeNotAlignedWithTiersWhenCustomAmountForbidden() {
        // 档位 [20万,50万] 全部落在区间 [10万,100万] 内 ⇒ ⑥ 通过；仅「区间未被档位撑满」这一条被触发
        assertRejectedWith("区间必须与档位对齐",
                () -> new CoverageSumInsuredConfig("HOSPITAL",
                        List.of(new SumInsuredTier("T1", wan("20"), null, 1),
                                new SumInsuredTier("T2", wan("50"), null, 2)),
                        wan("10"), wan("100"), false, "T1"));
    }

    @Test
    @DisplayName("⑦ 对照组：同档位同区间，开放自由填 ⇒ 通过（证明失败点确实是「未对齐」而非「档位越界」）")
    void acceptsSameTiersWhenCustomAmountAllowed() {
        CoverageSumInsuredConfig config = new CoverageSumInsuredConfig("HOSPITAL",
                List.of(new SumInsuredTier("T1", wan("20"), null, 1),
                        new SumInsuredTier("T2", wan("50"), null, 2)),
                wan("10"), wan("100"), true, "T1");

        assertTrue(config.allowCustomAmount(), "同一档位与区间在开放自由填时须合法——否则 ⑦ 的失败点无法归因");
    }

    // ==================== 录入侧判定（契约，供 G02 消费） ====================

    @Test
    @DisplayName("录入判定：精确命中档位 ⇒ TIER 且回填该档位（开放自由填时也优先选档）")
    void resolveSelectionPrefersTierHit() {
        SumInsuredSelection selection = threeTierConfig().resolveSelection(wan("30"));

        assertEquals(SumInsuredSelectionMode.TIER, selection.mode());
        assertEquals("T2", selection.tier().tierCode());
    }

    @Test
    @DisplayName("录入判定：开放自由填且落在区间内 ⇒ CUSTOM 且不带档位")
    void resolveSelectionReturnsCustomWithinRange() {
        SumInsuredSelection selection = threeTierConfig().resolveSelection(wan("45"));

        assertEquals(SumInsuredSelectionMode.CUSTOM, selection.mode());
        assertNull(selection.tier(), "自由填无档位可回填");
    }

    @Test
    @DisplayName("录入判定：开放自由填但超出区间 ⇒ 拒绝")
    void resolveSelectionRejectsOutOfRange() {
        assertRejectedWith("超出允许区间", () -> threeTierConfig().resolveSelection(wan("101")));
    }

    @Test
    @DisplayName("录入判定：不开放自由填且未命中档位 ⇒ 拒绝，文案须点明「未命中档位」而非「超出区间」")
    void resolveSelectionRejectsTierMissWhenCustomAmountForbidden() {
        CoverageSumInsuredConfig config = new CoverageSumInsuredConfig("HOSPITAL",
                List.of(new SumInsuredTier("T1", wan("20"), null, 1),
                        new SumInsuredTier("T2", wan("50"), null, 2)),
                wan("20"), wan("50"), false, "T1");

        assertRejectedWith("未命中任何档位", () -> config.resolveSelection(wan("20.5")));
    }

    @Test
    @DisplayName("录入判定：保额为空或非正 ⇒ 拒绝")
    void resolveSelectionRejectsNonPositive() {
        assertRejectedWith("必须大于 0", () -> threeTierConfig().resolveSelection(null));
        assertRejectedWith("必须大于 0", () -> threeTierConfig().resolveSelection(BigDecimal.ZERO));
    }

    // ==================== 判定结果自洽 ====================

    @Test
    @DisplayName("判定结果：模式与档位搭配错配 ⇒ 拒绝（防止下游拿到「自称选档却无档位」的结果）")
    void selectionResultRejectsMismatchedMode() {
        assertThrows(IllegalArgumentException.class,
                () -> new SumInsuredSelection(SumInsuredSelectionMode.TIER, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SumInsuredSelection(SumInsuredSelectionMode.CUSTOM,
                        new SumInsuredTier("T1", wan("10"), null, 1)));
    }
}
