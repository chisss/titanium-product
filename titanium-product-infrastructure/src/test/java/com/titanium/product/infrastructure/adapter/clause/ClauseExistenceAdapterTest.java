package com.titanium.product.infrastructure.adapter.clause;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.titanium.clause.api.ClauseApi;
import com.titanium.clause.api.response.clause.ClauseResponse;
import com.titanium.metadata.enums.clause.ClauseEnum;
import com.titanium.product.port.clause.ClauseBindingCheck;

import feign.FeignException;

/**
 * {@link ClauseExistenceAdapter} 单元测试
 *
 * <p>覆盖本适配器<b>唯一</b>的职责分野——「可绑定 / 明确不存在（404）/ 存在但未生效 / 问不到（其它失败）」
 * 四态。两个反面对照分别守住两条不同的失效通道：</p>
 * <ul>
 *   <li>「问不到」若被误判成「不存在」，用户会看到「条款不存在: &lt;一个完全合法的ID&gt;」，
 *       基础设施故障被伪装成输入错误、排查方向被带偏（与 R7-01「失败被说成资源不存在」同源）；</li>
 *   <li>「存在但未生效」若被并进「不存在」，运营会照提示去新建同编码条款，撞上编码唯一约束
 *       —— 两类拒绝必须分桶。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ClauseExistenceAdapterTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private ClauseApi clauseApi;

    @InjectMocks
    private ClauseExistenceAdapter adapter;

    @Test
    @DisplayName("只报出条款域明确回答 404 的那些，且保持入参顺序")
    void shouldReportOnlyClausesTheDomainAnswersNotFound() {
        when(clauseApi.getClauseById("clause-a", TENANT_ID)).thenReturn(activeClause());
        when(clauseApi.getClauseById("clause-b", TENANT_ID)).thenThrow(notFound());
        when(clauseApi.getClauseById("clause-c", TENANT_ID)).thenThrow(notFound());

        ClauseBindingCheck check = adapter.checkBindable(List.of("clause-a", "clause-b", "clause-c"), TENANT_ID);

        assertThat(check.missingClauseIds()).containsExactly("clause-b", "clause-c");
        assertThat(check.nonActiveClauseIds()).isEmpty();
    }

    @Test
    @DisplayName("条款域响应体为 null 时判为缺失，不放行")
    void shouldTreatNullResponseAsMissing() {
        // Provider 端不存在时抛 404，不会返回 null；此兜底防的是「Feign 解码异常却不抛错」的下游变更
        when(clauseApi.getClauseById("clause-a", TENANT_ID)).thenReturn(null);

        assertThat(adapter.checkBindable(List.of("clause-a"), TENANT_ID).missingClauseIds())
                .containsExactly("clause-a");
    }

    @Test
    @DisplayName("条款全部 ACTIVE 时结论为可绑定")
    void shouldBeCleanWhenEveryClauseIsActive() {
        when(clauseApi.getClauseById("clause-a", TENANT_ID)).thenReturn(activeClause());
        when(clauseApi.getClauseById("clause-b", TENANT_ID)).thenReturn(activeClause());

        ClauseBindingCheck check = adapter.checkBindable(List.of("clause-a", "clause-b"), TENANT_ID);

        assertThat(check.isBindable()).isTrue();
        assertThat(check.missingClauseIds()).isEmpty();
        assertThat(check.nonActiveClauseIds()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = ClauseEnum.ClauseStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("🔴 除 ACTIVE 外的任一状态都不可绑定，且记入 nonActive 而非 missing")
    void shouldRejectEveryStatusOtherThanActiveIntoItsOwnBucket(ClauseEnum.ClauseStatus status) {
        when(clauseApi.getClauseById("clause-a", TENANT_ID)).thenReturn(clauseWithStatus(status));

        ClauseBindingCheck check = adapter.checkBindable(List.of("clause-a"), TENANT_ID);

        assertThat(check.nonActiveClauseIds()).containsExactly("clause-a");
        assertThat(check.missingClauseIds()).isEmpty();
    }

    @Test
    @DisplayName("🔴 两类拒绝同时出现时各归各的桶，不得合并")
    void shouldKeepMissingAndNonActiveApart() {
        when(clauseApi.getClauseById("clause-a", TENANT_ID)).thenReturn(activeClause());
        when(clauseApi.getClauseById("clause-b", TENANT_ID)).thenThrow(notFound());
        when(clauseApi.getClauseById("clause-c", TENANT_ID))
                .thenReturn(clauseWithStatus(ClauseEnum.ClauseStatus.DRAFT));

        ClauseBindingCheck check = adapter.checkBindable(List.of("clause-a", "clause-b", "clause-c"), TENANT_ID);

        assertThat(check.missingClauseIds()).containsExactly("clause-b");
        assertThat(check.nonActiveClauseIds()).containsExactly("clause-c");
        assertThat(check.isBindable()).isFalse();
    }

    @Test
    @DisplayName("入参为空时不调用条款域")
    void shouldNotCallDomainForBlankInput() {
        assertThat(adapter.checkBindable(null, TENANT_ID).isBindable()).isTrue();
        assertThat(adapter.checkBindable(List.of(), TENANT_ID).isBindable()).isTrue();

        verifyNoInteractions(clauseApi);
    }

    @Test
    @DisplayName("🔴 条款服务不可用时原样上抛，绝不记成「条款不存在」")
    void shouldPropagateServiceFailureInsteadOfReportingMissing() {
        FeignException serviceUnavailable = mock(FeignException.ServiceUnavailable.class);
        when(clauseApi.getClauseById("clause-a", TENANT_ID)).thenThrow(serviceUnavailable);

        assertThatThrownBy(() -> adapter.checkBindable(List.of("clause-a"), TENANT_ID))
                .isSameAs(serviceUnavailable);
    }

    /** 可绑定的条款响应（ACTIVE） */
    private ClauseResponse activeClause() {
        return clauseWithStatus(ClauseEnum.ClauseStatus.ACTIVE);
    }

    /** 构造指定状态的条款响应 */
    private ClauseResponse clauseWithStatus(ClauseEnum.ClauseStatus status) {
        ClauseResponse response = new ClauseResponse();
        response.setStatus(status);
        return response;
    }

    /** 条款域回 404（ClauseNotFoundException → CLAUSE_NOT_EXIST）时 Feign 抛的异常 */
    private FeignException notFound() {
        return mock(FeignException.NotFound.class);
    }
}
