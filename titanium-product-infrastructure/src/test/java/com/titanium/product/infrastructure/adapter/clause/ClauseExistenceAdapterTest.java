package com.titanium.product.infrastructure.adapter.clause;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
import com.titanium.common.feign.TitaniumRemoteException;
import com.titanium.metadata.enums.clause.ClauseEnum;
import com.titanium.metadata.errorcode.ClauseErrorCode;
import com.titanium.metadata.errorcode.SystemErrorCode;
import com.titanium.metadata.exception.DomainException;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.port.clause.ClauseBindingCheck;

import feign.FeignException;
import feign.Request;

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
        when(clauseApi.getClauseById("clause-a")).thenReturn(activeClause());
        when(clauseApi.getClauseById("clause-b")).thenThrow(notExistViaUnifiedDecoder());
        when(clauseApi.getClauseById("clause-c")).thenThrow(notFound());

        ClauseBindingCheck check = adapter.checkBindable(List.of("clause-a", "clause-b", "clause-c"), TENANT_ID);

        assertThat(check.missingClauseIds()).containsExactly("clause-b", "clause-c");
        assertThat(check.nonActiveClauseIds()).isEmpty();
    }

    @Test
    @DisplayName("条款域响应体为 null 时判为缺失，不放行")
    void shouldTreatNullResponseAsMissing() {
        // Provider 端不存在时抛 404，不会返回 null；此兜底防的是「Feign 解码异常却不抛错」的下游变更
        when(clauseApi.getClauseById("clause-a")).thenReturn(null);

        assertThat(adapter.checkBindable(List.of("clause-a"), TENANT_ID).missingClauseIds())
                .containsExactly("clause-a");
    }

    @Test
    @DisplayName("条款全部 ACTIVE 时结论为可绑定")
    void shouldBeCleanWhenEveryClauseIsActive() {
        when(clauseApi.getClauseById("clause-a")).thenReturn(activeClause());
        when(clauseApi.getClauseById("clause-b")).thenReturn(activeClause());

        ClauseBindingCheck check = adapter.checkBindable(List.of("clause-a", "clause-b"), TENANT_ID);

        assertThat(check.isBindable()).isTrue();
        assertThat(check.missingClauseIds()).isEmpty();
        assertThat(check.nonActiveClauseIds()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = ClauseEnum.ClauseStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("🔴 除 ACTIVE 外的任一状态都不可绑定，且记入 nonActive 而非 missing")
    void shouldRejectEveryStatusOtherThanActiveIntoItsOwnBucket(ClauseEnum.ClauseStatus status) {
        when(clauseApi.getClauseById("clause-a")).thenReturn(successWithStatus(status));

        ClauseBindingCheck check = adapter.checkBindable(List.of("clause-a"), TENANT_ID);

        assertThat(check.nonActiveClauseIds()).containsExactly("clause-a");
        assertThat(check.missingClauseIds()).isEmpty();
    }

    @Test
    @DisplayName("🔴 两类拒绝同时出现时各归各的桶，不得合并")
    void shouldKeepMissingAndNonActiveApart() {
        when(clauseApi.getClauseById("clause-a")).thenReturn(activeClause());
        when(clauseApi.getClauseById("clause-b")).thenThrow(notExistViaUnifiedDecoder());
        when(clauseApi.getClauseById("clause-c"))
                .thenReturn(successWithStatus(ClauseEnum.ClauseStatus.DRAFT));

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
        when(clauseApi.getClauseById("clause-a")).thenThrow(serviceUnavailable);

        assertThatThrownBy(() -> adapter.checkBindable(List.of("clause-a"), TENANT_ID))
                .isSameAs(serviceUnavailable);
    }

    /** 两种失败形态在信封化后新增，必须与「缺失」分桶 */
    @Test
    @DisplayName("🔴 成功信封但 data 为 null 时判为缺失，不放行")
    void shouldTreatNullDataAsMissing() {
        when(clauseApi.getClauseById("clause-a")).thenReturn(ApiResponse.success(null));

        assertThat(adapter.checkBindable(List.of("clause-a"), TENANT_ID).missingClauseIds())
                .containsExactly("clause-a");
    }

    @Test
    @DisplayName("🔴 错误信封（2xx + 错误码）必须上抛，绝不记成「条款不存在」")
    void shouldPropagateErrorEnvelopeInsteadOfReportingMissing() {
        when(clauseApi.getClauseById("clause-a"))
                .thenReturn(ApiResponse.error(SystemErrorCode.DEPENDENCY_SERVICE_ERROR));

        assertThatThrownBy(() -> adapter.checkBindable(List.of("clause-a"), TENANT_ID))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("🔴 判据是业务码不是 HTTP 状态：同为 404 但码非 CLAUSE_NOT_EXIST，必须上抛")
    void shouldNotJudgeMissingByHttpStatusAlone() {
        // 一律按 status==404 判缺失是另一种会误报的走法——404 只说明「这个 URL 下没有该资源」，
        // 下游若用 404 承载别的业务语义（本域 40 段里 STATE_CONFLICT 类都可能被渲染成 4xx），
        // 照搬就会把「条款状态无效」说成「条款不存在」，与 R7-01 是同一类错误。
        when(clauseApi.getClauseById("clause-a"))
                .thenThrow(remoteError(404, ClauseErrorCode.CLAUSE_INVALID_STATUS));

        TitaniumRemoteException thrown = catchThrowableOfType(TitaniumRemoteException.class,
                () -> adapter.checkBindable(List.of("clause-a"), TENANT_ID));

        // 🔴 这一行不是多余的：catchThrowableOfType 在「压根没抛」时返回 null 而不 fail，
        // 直接解引用会得到一句 NPE（并被 JUnit 记成 error 而非 failure），失败原因不可读。
        assertThat(thrown).as("非 CLAUSE_NOT_EXIST 的远程业务码必须上抛，不得判为缺失").isNotNull();
        assertThat(thrown.getRemoteCode()).isEqualTo(ClauseErrorCode.CLAUSE_INVALID_STATUS.getCode());
    }

    @Test
    @DisplayName("兜底：下游未走统一解码器的原生 404，仍判为缺失")
    void shouldStillTreatRawNotFoundAsMissing() {
        when(clauseApi.getClauseById("clause-a")).thenThrow(notFound());

        assertThat(adapter.checkBindable(List.of("clause-a"), TENANT_ID).missingClauseIds())
                .containsExactly("clause-a");
    }

    /**
     * 条款域回答「不存在」时，经**统一解码器**（m21-07 {@code TitaniumFeignErrorDecoder}）抛出的异常。
     *
     * <p>🔴 这才是生产上的**主路径**：条款域 {@code ClauseExceptionHandler} 依
     * {@code ErrorCategory.RESOURCE_NOT_FOUND} 把 {@code CLAUSE_NOT_EXIST} 渲染成 404 + 错误信封，
     * 解码器见「非 2xx + 信封 + 非空业务码」即包成本类。它 {@code extends FeignException}，与
     * {@link FeignException.NotFound} 是<b>兄弟类而非其子类</b>——G01-R1-F2 的缺陷正是原实现只 catch 了
     * 后者，于是「条款不存在」一路落到 {@code catch (FeignException)}，被外层表达成「依赖服务错误」，
     * 把输入问题报成基础设施故障。下面这个 {@link #notFound()} 对应的分支是兜底，不再是主路径。</p>
     */
    private TitaniumRemoteException notExistViaUnifiedDecoder() {
        return remoteError(404, ClauseErrorCode.CLAUSE_NOT_EXIST);
    }

    /** 构造携带指定下游业务码的远程异常（status 与 code 分开给，便于验证「判码不判状态」）。 */
    private TitaniumRemoteException remoteError(int status, ClauseErrorCode errorCode) {
        return new TitaniumRemoteException(status, errorCode.getMessage(), dummyRequest(), new byte[0],
                Map.of(), errorCode.getCode());
    }

    /**
     * 构造供 {@link TitaniumRemoteException} 携带的占位请求。
     * <p>用 6 参 {@code (HttpMethod, url, headers, byte[], Charset, RequestTemplate)} 重载：它是
     * feign 13.6.1 里<b>唯一未标 {@code @Deprecated}</b> 的 create 族重载（5 参版与
     * {@code (…, Body, RequestTemplate)} 版均已废弃），且 6 参形态与 Body 版不构成重载歧义，
     * 末位可直接传 null。</p>
     */
    private static Request dummyRequest() {
        return Request.create(Request.HttpMethod.GET, "http://clause-service/api/v1/clauses/x",
                Map.<String, Collection<String>>of(), new byte[0], StandardCharsets.UTF_8, null);
    }

    /** 条款域回 404（ClauseNotFoundException → CLAUSE_NOT_EXIST）时 Feign 抛的**原生**异常——兜底路径 */
    private FeignException notFound() {
        return mock(FeignException.NotFound.class);
    }

    /** 可绑定的条款成功信封（ACTIVE） */
    private ApiResponse<ClauseResponse> activeClause() {
        return successWithStatus(ClauseEnum.ClauseStatus.ACTIVE);
    }

    /** 构造指定状态的条款成功信封（m24-01a 起契约统一返回 ApiResponse） */
    private ApiResponse<ClauseResponse> successWithStatus(ClauseEnum.ClauseStatus status) {
        ClauseResponse response = new ClauseResponse();
        response.setStatus(status);
        return ApiResponse.success(response);
    }
}
