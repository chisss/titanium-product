package com.titanium.product.infrastructure.adapter.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessResourceFailureException;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.port.claim.ClaimHistoryItem;
import com.titanium.product.port.claim.ClaimHistoryQuery;
import com.titanium.product.query.repository.ClaimHistoryViewRepository;
import com.titanium.product.query.view.ClaimHistoryView;
import com.titanium.product.valueobject.pricing.ncd.NcdLookbackWindow;

/**
 * 历史赔案取数适配器测试（m27-04/PKG-04，本地投影版）
 *
 * <p>核心判据是 g11-03 的失败关闭语义：<b>空列表 = 确无赔款；取不到 = 抛
 * {@code NCD_HISTORY_UNAVAILABLE}</b>。本地投影版新增两条：VIN 优先于标的ID，
 * 以及读模型查询失败必须失败关闭（不得降级为空列表）。</p>
 */
class ClaimHistoryAdapterTest {

    private static final String TENANT_ID = "1";

    private static final LocalDateTime FROM = LocalDateTime.of(2025, 10, 19, 0, 0);

    private static final LocalDateTime TO = LocalDateTime.of(2026, 10, 19, 0, 0);

    private static final NcdLookbackWindow WINDOW = new NcdLookbackWindow(FROM, TO);

    private ClaimHistoryQuery vinQuery() {
        return new ClaimHistoryQuery("VIN-9", "SUB-9", WINDOW);
    }

    private ClaimHistoryView view(String claimId, String status, LocalDateTime incidentDate) {
        ClaimHistoryView view = new ClaimHistoryView();
        view.setClaimId(claimId);
        view.setTenantId(TENANT_ID);
        view.setStatus(status);
        view.setIncidentDate(incidentDate);
        return view;
    }

    @Test
    @DisplayName("VIN 与标的ID并存：走 VIN 主路径，并复刻理赔域过滤语义（租户+区间+排除拒赔）")
    void shouldQueryByVinFirst() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        Mockito.when(repository
                .findByTenantIdAndVinAndIncidentDateGreaterThanEqualAndIncidentDateLessThanAndStatusNot(
                        TENANT_ID, "VIN-9", FROM, TO, "REJECTED"))
                .thenReturn(List.of(view("CLM-1", "APPROVED", LocalDateTime.of(2026, 3, 1, 8, 30))));
        ClaimHistoryAdapter adapter = new ClaimHistoryAdapter(repository);

        List<ClaimHistoryItem> items = adapter.findIncidents(vinQuery(), TENANT_ID);

        assertEquals(1, items.size());
        assertEquals("CLM-1", items.get(0).claimId());
        assertEquals(LocalDateTime.of(2026, 3, 1, 8, 30), items.get(0).incidentDate());
        assertEquals("APPROVED", items.get(0).claimStatus());
        Mockito.verify(repository, Mockito.never())
                .findByTenantIdAndSubjectIdAndIncidentDateGreaterThanEqualAndIncidentDateLessThanAndStatusNot(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("VIN 为空：回落标的ID兜底路径")
    void shouldFallbackToSubjectIdWhenVinMissing() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        Mockito.when(repository
                .findByTenantIdAndSubjectIdAndIncidentDateGreaterThanEqualAndIncidentDateLessThanAndStatusNot(
                        TENANT_ID, "SUB-9", FROM, TO, "REJECTED"))
                .thenReturn(List.of(view("CLM-2", "PAID", LocalDateTime.of(2026, 4, 1, 0, 0))));
        ClaimHistoryAdapter adapter = new ClaimHistoryAdapter(repository);

        List<ClaimHistoryItem> items = adapter.findIncidents(new ClaimHistoryQuery(" ", "SUB-9", WINDOW), TENANT_ID);

        assertEquals(1, items.size());
        assertEquals("CLM-2", items.get(0).claimId());
    }

    @Test
    @DisplayName("空列表是合法业务事实（确无赔款），原样返回空列表而不抛异常")
    void shouldReturnEmptyListWhenNoIncidents() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        Mockito.when(repository
                .findByTenantIdAndVinAndIncidentDateGreaterThanEqualAndIncidentDateLessThanAndStatusNot(
                        TENANT_ID, "VIN-9", FROM, TO, "REJECTED"))
                .thenReturn(List.of());
        ClaimHistoryAdapter adapter = new ClaimHistoryAdapter(repository);

        assertTrue(adapter.findIncidents(vinQuery(), TENANT_ID).isEmpty());
    }

    @Test
    @DisplayName("读模型查询失败：失败关闭（NCD_HISTORY_UNAVAILABLE），绝不降级为空列表")
    void shouldFailClosedWhenRepositoryFails() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        Mockito.when(repository
                .findByTenantIdAndVinAndIncidentDateGreaterThanEqualAndIncidentDateLessThanAndStatusNot(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new DataAccessResourceFailureException("数据库不可用"));
        ClaimHistoryAdapter adapter = new ClaimHistoryAdapter(repository);

        PricingDomainException thrown = assertThrows(PricingDomainException.class,
                () -> adapter.findIncidents(vinQuery(), TENANT_ID));
        assertEquals(ProductErrorCode.NCD_HISTORY_UNAVAILABLE, thrown.getErrorCode());
    }

    @Test
    @DisplayName("VIN 与标的ID皆空：拒绝查询（NCD_SUBJECT_IDENTIFIER_REQUIRED），不得退化成 IS NULL 查询")
    void shouldRejectWhenBothIdentifiersMissing() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        ClaimHistoryAdapter adapter = new ClaimHistoryAdapter(repository);

        PricingDomainException thrown = assertThrows(PricingDomainException.class,
                () -> adapter.findIncidents(new ClaimHistoryQuery(null, " ", WINDOW), TENANT_ID));
        assertEquals(ProductErrorCode.NCD_SUBJECT_IDENTIFIER_REQUIRED, thrown.getErrorCode());
        Mockito.verifyNoInteractions(repository);
    }
}
