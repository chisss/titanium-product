package com.titanium.product.application.orchestration.pricing.ncd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.application.model.pricing.ncd.NcdResolutionResult;
import com.titanium.product.command.pricing.ncd.ResolveNcdCommand;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.port.claim.ClaimHistoryItem;
import com.titanium.product.port.claim.ClaimHistoryPort;
import com.titanium.product.port.claim.ClaimHistoryQuery;
import com.titanium.product.pricing.aggregate.ncd.NcdRule;
import com.titanium.product.pricing.aggregate.ncd.NcdRuleSet;
import com.titanium.product.repository.NcdRuleSetRepository;

/**
 * NCD（无赔款优待）反算取数与规则契约（G11/g11-03，AC-03/AC-04）
 * <p>
 * 覆盖两条验收：<b>AC-03</b>「由赔案反算、取不到不得静默给 1.0」；<b>AC-04</b>「返回值携带取数依据
 * （条数/区间/规则版本）」。断言打在**编排器**上（而非仅聚合），因为「取不到历史」这一分支只有把
 * 取数端口一起纳入才可达——只测聚合会漏掉本缺陷的主路径。
 * </p>
 * <p>
 * 🔴 <b>测试数据里刻意不出现系数 1.0</b>：1.0 在本域是「无赔款优待不适用」的**业务结论**，只能由
 * 配置了该档位的规则集给出。本类既不配置它、也不期待它，从而「反算结果 = 1.0」在任何一条路径上
 * 都只能来自代码兜底——正是 AC-03 要禁的那件事。
 * </p>
 * <p>
 * 档位取值属 **R-3 待确认项**，本类中的阶梯仅为桩数据；生产库中两表为空（DDL 只建结构、不落规则值）。
 * </p>
 */
class NcdResolutionTest {

    private static final String TENANT_ID = "TENANT-1";
    private static final String PRODUCT_ID = "PRODUCT-1";
    private static final String VIN = "LVIN0000000000001";
    private static final String SUBJECT_ID = "SUBJECT-1";
    private static final String VEHICLE_TYPE = "SEDAN";
    private static final LocalDateTime BUSINESS_TIME = LocalDateTime.of(2026, 10, 10, 10, 0);

    private NcdRuleSetRepository ncdRuleSetRepository;
    private ClaimHistoryPort claimHistoryPort;
    private NcdResolutionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        ncdRuleSetRepository = mock(NcdRuleSetRepository.class);
        claimHistoryPort = mock(ClaimHistoryPort.class);
        orchestrator = new NcdResolutionOrchestrator(ncdRuleSetRepository, claimHistoryPort);
    }

    // ---------- AC-03：0 / 1 / 2 / 3+ 次与规则表系数输出可预期 ----------

    @Test
    @DisplayName("AC-03：0 次赔案命中 0 次档位，返回该档系数")
    void shouldResolveZeroClaimBandForNoIncidents() {
        givenPublishedRuleSet(ladder());
        givenIncidents(List.of());

        NcdResolutionResult result = orchestrator.resolve(command());

        assertEquals(0, new BigDecimal("0.70").compareTo(result.coefficient()));
        assertEquals(0, result.claimCount());
    }

    @Test
    @DisplayName("AC-03：1 次赔案命中 1 次档位，返回该档系数")
    void shouldResolveOneClaimBand() {
        givenPublishedRuleSet(ladder());
        givenIncidents(incidents(1));

        NcdResolutionResult result = orchestrator.resolve(command());

        assertEquals(0, new BigDecimal("0.85").compareTo(result.coefficient()));
        assertEquals(1, result.claimCount());
    }

    @Test
    @DisplayName("AC-03：2 次赔案命中 2 次档位，返回该档系数")
    void shouldResolveTwoClaimBand() {
        givenPublishedRuleSet(ladder());
        givenIncidents(incidents(2));

        NcdResolutionResult result = orchestrator.resolve(command());

        assertEquals(0, new BigDecimal("1.05").compareTo(result.coefficient()));
        assertEquals(2, result.claimCount());
    }

    @Test
    @DisplayName("AC-03：3 次及以上（无上界档）命中 3+ 档位，3 次与 7 次同档")
    void shouldResolveOpenEndedBandForThreeOrMoreClaims() {
        givenPublishedRuleSet(ladder());

        givenIncidents(incidents(3));
        NcdResolutionResult three = orchestrator.resolve(command());
        givenIncidents(incidents(7));
        NcdResolutionResult seven = orchestrator.resolve(command());

        assertEquals(0, new BigDecimal("1.25").compareTo(three.coefficient()));
        assertEquals(0, new BigDecimal("1.25").compareTo(seven.coefficient()));
        assertEquals(3, three.claimCount());
        assertEquals(7, seven.claimCount());
    }

    @Test
    @DisplayName("AC-03：次数档位是闭区间——上界次数命中本档，上界+1 才落下一档")
    void shouldTreatCountBandAsClosedInterval() {
        givenPublishedRuleSet(published(rules(List.of(
                new NcdRule(0, 2, null, new BigDecimal("0.70")),
                new NcdRule(3, null, null, new BigDecimal("1.25"))))));

        // 2 次恰为「0~2 次档」的上界：闭区间口径下必须命中 0.70。
        // 若误用半开区间 [0,2)，2 次会落空 —— 而这是离散计数，半开正是本域刻意避开的写法。
        givenIncidents(incidents(2));
        assertEquals(0, new BigDecimal("0.70").compareTo(orchestrator.resolve(command()).coefficient()));

        // 3 次越出上界，落「3 次及以上」档
        givenIncidents(incidents(3));
        assertEquals(0, new BigDecimal("1.25").compareTo(orchestrator.resolve(command()).coefficient()));
    }

    // ---------- AC-03：取不到历史不得静默给 1.0 ----------

    @Test
    @DisplayName("AC-03：历史赔案取不到 → 显式拒绝（NCD_HISTORY_UNAVAILABLE），绝不带结果返回")
    void shouldRejectWhenClaimHistoryUnavailable() {
        givenPublishedRuleSet(ladder());
        when(claimHistoryPort.findIncidents(any(), eq(TENANT_ID)))
                .thenThrow(new PricingDomainException(ProductErrorCode.NCD_HISTORY_UNAVAILABLE,
                        "历史赔案查询不可达（桩）"));

        PricingDomainException exception = assertThrows(PricingDomainException.class,
                () -> orchestrator.resolve(command()));

        assertEquals(ProductErrorCode.NCD_HISTORY_UNAVAILABLE.getCode(), exception.getErrorCodeValue());
    }

    @Test
    @DisplayName("AC-03：🔴「取不到历史」与「确无赔款」必须可区分——前者拒绝，后者正常命中 0 次档位")
    void shouldDistinguishUnavailableHistoryFromNoClaims() {
        givenPublishedRuleSet(ladder());

        // ① 确无赔款：端口如实返回空列表 ⇒ 合法参与反算，命中 0 次档位
        givenIncidents(List.of());
        NcdResolutionResult noClaims = orchestrator.resolve(command());
        assertEquals(0, noClaims.claimCount());
        assertEquals(0, new BigDecimal("0.70").compareTo(noClaims.coefficient()));

        // ② 取不到历史：端口抛错 ⇒ 拒绝。两者在「有没有结果」这一层就分开，
        //    不存在「都当 0 次处理」的合并路径（合并即静默少收保费）。
        when(claimHistoryPort.findIncidents(any(), eq(TENANT_ID)))
                .thenThrow(new PricingDomainException(ProductErrorCode.NCD_HISTORY_UNAVAILABLE, "不可达（桩）"));
        assertThrows(PricingDomainException.class, () -> orchestrator.resolve(command()));
    }

    @Test
    @DisplayName("AC-03：无已发布规则集 → 显式拒绝（NCD_RULE_NOT_CONFIGURED），不按无优待兜底")
    void shouldRejectWhenNoPublishedRuleSet() {
        when(ncdRuleSetRepository.findPublished(TENANT_ID, PRODUCT_ID, BUSINESS_TIME))
                .thenReturn(Optional.empty());

        PricingDomainException exception = assertThrows(PricingDomainException.class,
                () -> orchestrator.resolve(command()));

        assertEquals(ProductErrorCode.NCD_RULE_NOT_CONFIGURED.getCode(), exception.getErrorCodeValue());
        // 缺规则即止：不去打对端的取数端口（无规则时取回条数也无处可用）
        verify(claimHistoryPort, never()).findIncidents(any(), any());
    }

    @Test
    @DisplayName("AC-03：缺标的标识（VIN 与标的ID 皆空）→ 显式拒绝，不按无赔款处理")
    void shouldRejectWhenSubjectIdentifierMissing() {
        givenPublishedRuleSet(ladder());

        PricingDomainException exception = assertThrows(PricingDomainException.class,
                () -> orchestrator.resolve(new ResolveNcdCommand(TENANT_ID, PRODUCT_ID, null, " ", VEHICLE_TYPE,
                        BUSINESS_TIME)));

        assertEquals(ProductErrorCode.NCD_SUBJECT_IDENTIFIER_REQUIRED.getCode(), exception.getErrorCodeValue());
        // 无车辆身份即止：凭空按无赔款给优待，比不给更糟
        verify(ncdRuleSetRepository, never()).findPublished(any(), any(), any());
        verify(claimHistoryPort, never()).findIncidents(any(), any());
    }

    @Test
    @DisplayName("AC-03：次数未命中任何档位 → 显式拒绝（NCD_BAND_NOT_MATCHED），不回落默认系数")
    void shouldRejectWhenNoBandMatched() {
        // 档位只覆盖 0~1 次，缺口在 2 次及以上
        givenPublishedRuleSet(published(rules(List.of(
                new NcdRule(0, 0, null, new BigDecimal("0.70")),
                new NcdRule(1, 1, null, new BigDecimal("0.85"))))));
        givenIncidents(incidents(4));

        PricingDomainException exception = assertThrows(PricingDomainException.class,
                () -> orchestrator.resolve(command()));

        assertEquals(ProductErrorCode.NCD_BAND_NOT_MATCHED.getCode(), exception.getErrorCodeValue());
    }

    @Test
    @DisplayName("AC-03：同车型档内区间重叠 → 显式拒绝（NCD_RULE_CONFLICT），不按声明顺序取首个命中")
    void shouldRejectWhenBandsOverlap() {
        // 两条通用档同时覆盖 2 次：0~2 与 2~5
        givenPublishedRuleSet(published(rules(List.of(
                new NcdRule(0, 2, null, new BigDecimal("0.70")),
                new NcdRule(2, 5, null, new BigDecimal("1.05"))))));
        givenIncidents(incidents(2));

        PricingDomainException exception = assertThrows(PricingDomainException.class,
                () -> orchestrator.resolve(command()));

        assertEquals(ProductErrorCode.NCD_RULE_CONFLICT.getCode(), exception.getErrorCodeValue());
    }

    @Test
    @DisplayName("AC-03：只有车型专属档时不按车型取档 → 拒绝（缺通用档），不取任一档位")
    void shouldRejectWhenOnlyVehicleSpecificBandExists() {
        givenPublishedRuleSet(published(rules(List.of(
                new NcdRule(0, null, "SUV", new BigDecimal("0.70"))))));
        givenIncidents(incidents(1));

        PricingDomainException exception = assertThrows(PricingDomainException.class,
                () -> orchestrator.resolve(command()));

        assertEquals(ProductErrorCode.NCD_BAND_NOT_MATCHED.getCode(), exception.getErrorCodeValue());
    }

    @Test
    @DisplayName("AC-03：🔴 三条取不到路径全部抛错 ⇒ 「返回结果」与「拒绝」之间无第三态")
    void shouldNeverProduceResultOnUnresolvablePaths() {
        // 规则集缺位
        when(ncdRuleSetRepository.findPublished(any(), any(), any())).thenReturn(Optional.empty());
        assertRejected(ProductErrorCode.NCD_RULE_NOT_CONFIGURED, command());

        // 历史缺位
        givenPublishedRuleSet(ladder());
        when(claimHistoryPort.findIncidents(any(), eq(TENANT_ID)))
                .thenThrow(new PricingDomainException(ProductErrorCode.NCD_HISTORY_UNAVAILABLE, "不可达（桩）"));
        assertRejected(ProductErrorCode.NCD_HISTORY_UNAVAILABLE, command());

        // 档位缺位
        givenPublishedRuleSet(published(rules(List.of(new NcdRule(0, 0, null, new BigDecimal("0.70"))))));
        givenIncidents(incidents(9));
        assertRejected(ProductErrorCode.NCD_BAND_NOT_MATCHED, command());
    }

    // ---------- AC-04：返回值携带取证字段 ----------

    @Test
    @DisplayName("AC-04：返回值携带赔案条数、统计区间、规则集编码/版本/内容指纹")
    void shouldCarryEvidenceFields() {
        NcdRuleSet ruleSet = givenPublishedRuleSet(ladder());
        givenIncidents(incidents(2));

        NcdResolutionResult result = orchestrator.resolve(command());

        assertEquals(PRODUCT_ID, result.productId());
        assertEquals(2, result.claimCount());
        assertEquals(LocalDateTime.of(2025, 10, 10, 0, 0), result.periodFrom());
        assertEquals(LocalDateTime.of(2026, 10, 11, 0, 0), result.periodTo());
        assertEquals("NCD-AUTO", result.ruleSetCode());
        assertEquals("V1.0", result.ruleSetVersion());
        assertEquals(ruleSet.getContentHash(), result.ruleSetContentHash());
    }

    @Test
    @DisplayName("AC-04：统计区间按规则集 lookbackMonths 派生，并以半开区间下发给取数端口")
    void shouldDeriveLookbackWindowFromRuleSetAndPassItToPort() {
        givenPublishedRuleSet(ladder());
        givenIncidents(List.of());

        orchestrator.resolve(command());

        ArgumentCaptor<ClaimHistoryQuery> captor = ArgumentCaptor.forClass(ClaimHistoryQuery.class);
        verify(claimHistoryPort, times(1)).findIncidents(captor.capture(), eq(TENANT_ID));
        ClaimHistoryQuery query = captor.getValue();
        assertEquals(VIN, query.vin());
        assertEquals(SUBJECT_ID, query.subjectId());
        // 半开 [from, to)：from 含、to 不含，业务日当天赔案计入统计
        assertEquals(LocalDateTime.of(2025, 10, 10, 0, 0), query.window().from());
        assertEquals(LocalDateTime.of(2026, 10, 11, 0, 0), query.window().to());
    }

    @Test
    @DisplayName("AC-04：统计区间长度取自规则集（改 lookbackMonths 即改区间），不由代码写死")
    void shouldUseRuleSetLookbackMonths() {
        givenPublishedRuleSet(published(ruleSet(ladder(), 24)));
        givenIncidents(List.of());

        NcdResolutionResult result = orchestrator.resolve(command());

        assertEquals(LocalDateTime.of(2024, 10, 10, 0, 0), result.periodFrom());
        assertEquals(LocalDateTime.of(2026, 10, 11, 0, 0), result.periodTo());
    }

    @Test
    @DisplayName("AC-04：规则集内容不变时内容指纹稳定（换行序不改变指纹）")
    void shouldKeepContentHashStableRegardlessOfRuleOrder() {
        List<NcdRule> ordered = new ArrayList<>(ladder());
        List<NcdRule> reversed = new ArrayList<>(ordered);
        Collections.reverse(reversed);

        NcdRuleSet forward = published(ruleSet(ordered, 12));
        NcdRuleSet backward = published(ruleSet(reversed, 12));

        assertEquals(forward.getContentHash(), backward.getContentHash());
    }

    // ---------- 测试夹具 ----------

    /**
     * 通用档阶梯（无车型专属档）：0 次 / 1 次 / 2 次 / 3 次及以上，区间互不重叠。
     * <p>
     * 档位值仅为**测试桩数据**，不代表任何业务口径——档位切分与系数取值属 R-3 待确认项。
     * </p>
     */
    private List<NcdRule> ladder() {
        return List.of(
                new NcdRule(0, 0, null, new BigDecimal("0.70")),
                new NcdRule(1, 1, null, new BigDecimal("0.85")),
                new NcdRule(2, 2, null, new BigDecimal("1.05")),
                new NcdRule(3, null, null, new BigDecimal("1.25")));
    }

    private NcdRuleSet givenPublishedRuleSet(List<NcdRule> rules) {
        return givenPublishedRuleSet(published(rules(rules)));
    }

    private NcdRuleSet givenPublishedRuleSet(NcdRuleSet ruleSet) {
        when(ncdRuleSetRepository.findPublished(TENANT_ID, PRODUCT_ID, BUSINESS_TIME))
                .thenReturn(Optional.of(ruleSet));
        return ruleSet;
    }

    private NcdRuleSet rules(List<NcdRule> rules) {
        return ruleSet(rules, 12);
    }

    private NcdRuleSet ruleSet(List<NcdRule> rules, int lookbackMonths) {
        return NcdRuleSet.createDraft("NCD-SET-1", PRODUCT_ID, "ncd-auto", "V1.0", lookbackMonths, rules,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, TENANT_ID);
    }

    private NcdRuleSet published(NcdRuleSet ruleSet) {
        ruleSet.approve();
        ruleSet.publish();
        return ruleSet;
    }

    private void givenIncidents(List<ClaimHistoryItem> incidents) {
        when(claimHistoryPort.findIncidents(any(), eq(TENANT_ID))).thenReturn(incidents);
    }

    private List<ClaimHistoryItem> incidents(int count) {
        List<ClaimHistoryItem> items = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            items.add(new ClaimHistoryItem("CLAIM-" + index,
                    LocalDateTime.of(2026, 3, 1, 0, 0).plusDays(index), "CLOSED"));
        }
        return items;
    }

    private ResolveNcdCommand command() {
        return new ResolveNcdCommand(TENANT_ID, PRODUCT_ID, VIN, SUBJECT_ID, VEHICLE_TYPE, BUSINESS_TIME);
    }

    private void assertRejected(ProductErrorCode expected, ResolveNcdCommand command) {
        PricingDomainException exception = assertThrows(PricingDomainException.class,
                () -> orchestrator.resolve(command));
        assertEquals(expected.getCode(), exception.getErrorCodeValue());
    }
}
