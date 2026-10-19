package com.titanium.product.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.titanium.product.query.repository.ClaimHistoryViewRepository;
import com.titanium.product.query.view.ClaimHistoryView;

/**
 * 历史赔案投影监听器测试（m27-04/PKG-04）
 *
 * <p>覆盖：创建建档 / 重投幂等 / 状态变更 / 拒赔 / 跨主题乱序（状态先于创建）/
 * 更新事件 / 缺主键跳过 / 畸形报文不抛。判据重点不是「字段写进去了」，而是
 * <b>乱序与重投下状态不被错误覆盖</b>——那是本地投影替代同步调用后唯一新增的风险面。</p>
 */
class ClaimHistoryEventListenerTest {

    private static final String CLAIM_ID = "CLM-0001";

    private static final String TENANT_ID = "1";

    /** 理赔域 claim-created 的真实线格式：ClaimId record → {"value":"..."}，LocalDateTime → ISO 串 */
    private static final String CREATED_PAYLOAD = """
            {"claimId":{"value":"CLM-0001"},"customerId":{"value":"C1"},"policyId":{"value":"P1"},
            "claimNumber":"CN-1","claimType":"MEDICAL","incidentDate":"2026-03-01T08:30:00",
            "incidentDescription":"门诊","claimAmount":{"value":100.00,"currency":"CNY"},
            "createdAt":"2026-03-02T09:00:00","tenantId":"1","subjectId":"SUB-9","vin":"VIN-9"}""";

    private ClaimHistoryEventListener listener(ClaimHistoryViewRepository repository) {
        return new ClaimHistoryEventListener(repository);
    }

    @Test
    @DisplayName("赔案创建：建档并落地 NCD 取数四要素，状态缺省 PENDING")
    void shouldCreateProjectionWhenClaimCreated() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        Mockito.when(repository.findById(CLAIM_ID)).thenReturn(Optional.empty());
        ClaimHistoryEventListener listener = listener(repository);

        listener.onClaimCreated(CREATED_PAYLOAD);

        ArgumentCaptor<ClaimHistoryView> captor = ArgumentCaptor.forClass(ClaimHistoryView.class);
        Mockito.verify(repository).save(captor.capture());
        ClaimHistoryView view = captor.getValue();
        assertEquals(CLAIM_ID, view.getClaimId());
        assertEquals(TENANT_ID, view.getTenantId());
        assertEquals("VIN-9", view.getVin());
        assertEquals("SUB-9", view.getSubjectId());
        assertEquals(LocalDateTime.of(2026, 3, 1, 8, 30), view.getIncidentDate());
        assertEquals("PENDING", view.getStatus());
        assertNotNull(view.getCreateTime(), "首次投影必须写创建时间（BaseView 无 JPA Auditing）");
        assertNotNull(view.getUpdateTime());
    }

    @Test
    @DisplayName("创建事件重投：复用既有行，且不得用缺省 PENDING 覆盖已落地的后续状态")
    void shouldNotOverwriteStatusWhenCreatedRedelivered() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        ClaimHistoryView existing = new ClaimHistoryView();
        existing.setClaimId(CLAIM_ID);
        existing.setStatus("APPROVED");
        existing.setCreateTime(LocalDateTime.of(2026, 3, 2, 9, 0));
        Mockito.when(repository.findById(CLAIM_ID)).thenReturn(Optional.of(existing));
        ClaimHistoryEventListener listener = listener(repository);

        listener.onClaimCreated(CREATED_PAYLOAD);

        ArgumentCaptor<ClaimHistoryView> captor = ArgumentCaptor.forClass(ClaimHistoryView.class);
        Mockito.verify(repository).save(captor.capture());
        assertSame(existing, captor.getValue(), "重复消费必须更新既有行，主键幂等");
        assertEquals("APPROVED", captor.getValue().getStatus(),
                "创建事件可能迟到（跨主题乱序）：不得把已落地的状态改回 PENDING");
        assertEquals(LocalDateTime.of(2026, 3, 2, 9, 0), captor.getValue().getCreateTime(),
                "创建时间只在首次投影时写入");
        assertEquals("VIN-9", captor.getValue().getVin(), "迟到/重投的创建事件仍补齐要素");
    }

    @Test
    @DisplayName("状态变更先于创建到达：先落状态桩（不丢弃），创建后仍需保留该状态")
    void shouldCreateStatusStubWhenStatusArrivesFirst() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        Mockito.when(repository.findById(CLAIM_ID)).thenReturn(Optional.empty());
        ClaimHistoryEventListener listener = listener(repository);

        listener.onClaimStatusChanged("""
                {"claimId":{"value":"CLM-0001"},"oldStatus":"PENDING","newStatus":"APPROVED",
                "reason":"核赔通过","changedAt":"2026-03-05T10:00:00","tenantId":"1"}""");

        ArgumentCaptor<ClaimHistoryView> captor = ArgumentCaptor.forClass(ClaimHistoryView.class);
        Mockito.verify(repository).save(captor.capture());
        ClaimHistoryView stub = captor.getValue();
        assertEquals(CLAIM_ID, stub.getClaimId());
        assertEquals(TENANT_ID, stub.getTenantId());
        assertEquals("APPROVED", stub.getStatus());
        assertNull(stub.getVin(), "状态桩没有车辆要素，不会命中 NCD 区间查询，待创建事件补齐");
        assertNull(stub.getIncidentDate());
    }

    @Test
    @DisplayName("拒赔事件：置 REJECTED（赔付路径只发拒赔事件、不发状态变更事件，故不可省略）")
    void shouldMarkRejected() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        ClaimHistoryView existing = new ClaimHistoryView();
        existing.setClaimId(CLAIM_ID);
        existing.setStatus("PENDING");
        Mockito.when(repository.findById(CLAIM_ID)).thenReturn(Optional.of(existing));
        ClaimHistoryEventListener listener = listener(repository);

        listener.onClaimRejected("""
                {"claimId":{"value":"CLM-0001"},"policyId":"P1","customerId":"C1",
                "reason":"EXCLUDED","comment":"既往症","rejectedAt":"2026-03-06T10:00:00","tenantId":"1"}""");

        assertEquals("REJECTED", existing.getStatus());
        Mockito.verify(repository).save(existing);
    }

    @Test
    @DisplayName("赔案更新：仅同步可更正的出险时间，不影响状态")
    void shouldUpdateIncidentDateWhenClaimUpdated() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        ClaimHistoryView existing = new ClaimHistoryView();
        existing.setClaimId(CLAIM_ID);
        existing.setStatus("APPROVED");
        existing.setIncidentDate(LocalDateTime.of(2026, 3, 1, 8, 30));
        Mockito.when(repository.findById(CLAIM_ID)).thenReturn(Optional.of(existing));
        ClaimHistoryEventListener listener = listener(repository);

        listener.onClaimUpdated("""
                {"claimId":{"value":"CLM-0001"},"claimType":"MEDICAL","incidentDate":"2026-03-02T00:00:00",
                "incidentDescription":"修正出险日","claimAmount":{"value":100.00,"currency":"CNY"},
                "updatedAt":"2026-03-07T10:00:00","tenantId":"1"}""");

        assertEquals(LocalDateTime.of(2026, 3, 2, 0, 0), existing.getIncidentDate());
        assertEquals("APPROVED", existing.getStatus(), "更新事件不改变状态");
        Mockito.verify(repository).save(existing);
    }

    @Test
    @DisplayName("缺 claimId/tenantId：跳过投影，不写库也不抛出")
    void shouldSkipWhenIdentityMissing() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        ClaimHistoryEventListener listener = listener(repository);

        assertDoesNotThrow(() -> listener.onClaimCreated("""
                {"incidentDate":"2026-03-01T08:30:00","tenantId":"1"}"""));
        assertDoesNotThrow(() -> listener.onClaimStatusChanged("""
                {"claimId":{"value":"CLM-0001"},"newStatus":"APPROVED"}"""));

        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
        Mockito.verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("畸形报文/空报文：记 warn 后跳过，不阻塞消费位点")
    void shouldNotThrowWhenPayloadMalformed() {
        ClaimHistoryViewRepository repository = Mockito.mock(ClaimHistoryViewRepository.class);
        ClaimHistoryEventListener listener = listener(repository);

        assertDoesNotThrow(() -> listener.onClaimCreated("not-a-json"));
        assertDoesNotThrow(() -> listener.onClaimUpdated("null"));
        assertDoesNotThrow(() -> listener.onClaimRejected(""));

        Mockito.verifyNoInteractions(repository);
    }
}
