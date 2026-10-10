package com.titanium.product.application.orchestration.product;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.errorcode.ErrorCategory;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.CommandValidationException;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.port.clause.ClauseBindingCheck;
import com.titanium.product.port.clause.ClauseExistencePort;
import com.titanium.product.pricing.aggregate.CalculationModelDefinition;
import com.titanium.product.pricing.aggregate.ChargeComponentDefinition;
import com.titanium.product.pricing.aggregate.PricingPlanDefinition;
import com.titanium.product.pricing.aggregate.RateTableDefinition;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductClauseQueryService;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.CalculationModelRepository;
import com.titanium.product.repository.ChargeComponentRepository;
import com.titanium.product.repository.PricingPlanRepository;
import com.titanium.product.repository.RateTableManagementRepository;
import com.titanium.product.valueobject.rate.RateTableRef;

/**
 * 产品上架完整性校验器单测（`G01/AC-01`）。
 *
 * <p>🔴 <b>判据全部经 2026-09-26 真机数据校准</b>：定价侧一律<b>方案优先</b>——有已发布定价方案即放行，
 * 与产品声明的定价模式无关（运行期 {@code quote} 即 <i>findEffective → quoteWithPricingPlan，
 * 未命中才 orElseGet(quoteWithLegacyRateTable)</i>）。见 {@code collectPricingDeficiencies}。</p>
 *
 * <p>🔴 <b>本类的 {@link #unpublishedRateTableShouldBeRejected()} 是 `G01/AC-02` 反向注入探针的靶点</b>：
 * 注释掉 {@link ProductPublishValidator} 中任一校验调用（如 `collectRateTableDeficiencies`），
 * 本用例必须变红且 <b>{@code Failures >= 1}（不是 {@code Errors}）</b>——判据是「断言如期失败」，
 * 不是「命令返回非零」，后者会把「注入把代码改坏了」误判为「判据有牙」。</p>
 */
class ProductPublishValidatorTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String PRODUCT_ID = "product-1";

    private ProductQueryService productQueryService;
    private ProductClauseQueryService productClauseQueryService;
    private ClauseExistencePort clauseExistencePort;
    private RateTableManagementRepository rateTableRepository;
    private PricingPlanRepository pricingPlanRepository;
    private CalculationModelRepository calculationModelRepository;
    private ChargeComponentRepository chargeComponentRepository;

    private ProductPublishValidator validator;

    @BeforeEach
    void setUp() {
        productQueryService = mock(ProductQueryService.class);
        productClauseQueryService = mock(ProductClauseQueryService.class);
        clauseExistencePort = mock(ClauseExistencePort.class);
        rateTableRepository = mock(RateTableManagementRepository.class);
        pricingPlanRepository = mock(PricingPlanRepository.class);
        calculationModelRepository = mock(CalculationModelRepository.class);
        chargeComponentRepository = mock(ChargeComponentRepository.class);
        validator = new ProductPublishValidator(productQueryService, productClauseQueryService, clauseExistencePort,
                rateTableRepository, pricingPlanRepository, calculationModelRepository, chargeComponentRepository);
    }

    // ------------------------------------------------------------------ 正向：全齐放行

    @Test
    void publishedRateTableWithActiveClauseShouldPass() {
        givenProduct(PricingMode.RATE_TABLE, new RateTableRef("rt-1", "LIFE_BASE", "V1", List.of("age")));
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());
        when(rateTableRepository.findAll(TENANT_ID, PRODUCT_ID, RateTableStatus.PUBLISHED))
                .thenReturn(List.of(mock(RateTableDefinition.class)));

        assertDoesNotThrow(() -> validator.validate(TENANT_ID, PRODUCT_ID));
    }

    @Test
    void publishedActuarialComponentsShouldPass() {
        givenProduct(PricingMode.ACTUARIAL_FORMULA, null);
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());
        when(pricingPlanRepository.findAll(TENANT_ID, PRODUCT_ID, PricingPlanStatus.PUBLISHED))
                .thenReturn(List.of(mock(PricingPlanDefinition.class)));
        when(calculationModelRepository.findAll(TENANT_ID, PRODUCT_ID, ActuarialDefinitionStatus.PUBLISHED))
                .thenReturn(List.of(mock(CalculationModelDefinition.class)));
        when(chargeComponentRepository.findAll(TENANT_ID, PRODUCT_ID, ActuarialDefinitionStatus.PUBLISHED))
                .thenReturn(List.of(mock(ChargeComponentDefinition.class)));

        assertDoesNotThrow(() -> validator.validate(TENANT_ID, PRODUCT_ID));
    }

    /**
     * 模式分派证据：费率表模式下，三类精算组件全空**仍须放行**。
     *
     * <p>若校验写成「无论哪种模式都要求费率表 + 精算组件齐备」，本用例即变红——这正是本任务
     * 不做合并校验的理由（合并会误拦一半产品）。</p>
     */
    @Test
    void rateTableModeShouldNotRequireActuarialComponents() {
        givenProduct(PricingMode.RATE_TABLE, new RateTableRef("rt-1", "LIFE_BASE", "V1", List.of("age")));
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());
        when(rateTableRepository.findAll(TENANT_ID, PRODUCT_ID, RateTableStatus.PUBLISHED))
                .thenReturn(List.of(mock(RateTableDefinition.class)));
        // 三类精算组件刻意不打桩（Mockito 默认返回空列表）

        assertDoesNotThrow(() -> validator.validate(TENANT_ID, PRODUCT_ID));
    }

    // ------------------------------------------------- 反向：`G01/AC-02` 注入探针的靶点

    @Test
    void unpublishedRateTableShouldBeRejected() {
        givenProduct(PricingMode.RATE_TABLE, new RateTableRef("rt-1", "LIFE_BASE", "V1", List.of("age")));
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());
        when(rateTableRepository.findAll(TENANT_ID, PRODUCT_ID, RateTableStatus.PUBLISHED))
                .thenReturn(List.of()); // 只有草稿版本

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertEquals(ProductErrorCode.PRODUCT_VALIDATION_FAILED.getCode(), ex.getErrorCodeValue());
        assertTrue(ex.getMessage().contains("费率表"), "缺项须点名「费率表」，实际: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("PUBLISHED"), "缺项须说明判据是已发布，实际: " + ex.getMessage());
    }

    /**
     * 🔴 <b>反误拦用例（2026-09-26 真机实测得出，非拍脑袋）</b>：无费率表引用、但有已发布定价方案 ⇒ <b>放行</b>。
     *
     * <p>运行期 {@code PremiumQuoteApplicationService#quote} 是<b>方案优先</b>：命中已发布方案即走方案
     * （方案自带费率表绑定），{@code product.rateTableRef} 只在无方案时的 legacy 回退路径才被要求。
     * 真机实测租户 1 的 16 个 EFFECTIVE 费率表模式产品<b>全部</b>靠方案试算，其中 13 个引用为空——
     * 无条件要求引用非空会把这 13 个正常产品判为不可上架。</p>
     */
    @Test
    void rateTableModeWithPublishedPricingPlanShouldPassEvenIfRefMissing() {
        givenProduct(PricingMode.RATE_TABLE, null);
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());
        when(pricingPlanRepository.findAll(TENANT_ID, PRODUCT_ID, PricingPlanStatus.PUBLISHED))
                .thenReturn(List.of(mock(PricingPlanDefinition.class)));

        assertDoesNotThrow(() -> validator.validate(TENANT_ID, PRODUCT_ID));
    }

    /**
     * 🔴 <b>反误拦用例（真机 CFT-AUTO-COM-0904 的形态）</b>：{@code pricingMode} 为 {@code null}，
     * 但有已发布定价方案 ⇒ <b>放行</b>。
     *
     * <p>运行期方案路径 {@code validatePricingPlanBinding} 对模式的判据是
     * {@code product.getPricingMode() != null && ...}——<b>模式为空被显式容忍</b>。
     * 真机实测租户 1 有 4 个 EFFECTIVE 产品模式为空，其中 CFT-AUTO-COM-0904 有已发布方案 + 已发布费率表，
     * 完全算得出保费；按「模式为空即拦」会误拦它（以及另外一类 3 个真无路可走的产品需靠别的判据区分）。</p>
     */
    @Test
    void nullPricingModeWithPublishedPricingPlanShouldPass() {
        givenProduct(null, null);
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());
        when(pricingPlanRepository.findAll(TENANT_ID, PRODUCT_ID, PricingPlanStatus.PUBLISHED))
                .thenReturn(List.of(mock(PricingPlanDefinition.class)));

        assertDoesNotThrow(() -> validator.validate(TENANT_ID, PRODUCT_ID));
    }

    /** 两条定价路径皆不可用（无方案 + 无引用）⇒ 拒绝：这是**真的算不出保费**的那一类。 */
    @Test
    void rateTableModeWithoutPlanAndWithoutRefShouldBeRejected() {
        givenProduct(PricingMode.RATE_TABLE, null);
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());
        // 默认无已发布定价方案

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertTrue(ex.getMessage().contains("既无已发布"), "实际: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("也未配置费率表引用"), "实际: " + ex.getMessage());
    }

    /** 条款**不存在**与条款**未生效**必须说两种话（`ClauseBindingCheck` 两类拒绝分开承载）。 */
    @Test
    void missingClauseShouldBeRejectedWithNotFoundWording() {
        givenProduct(PricingMode.RATE_TABLE, new RateTableRef("rt-1", "LIFE_BASE", "V1", List.of("age")));
        givenClauseBinding("clause-1", new ClauseBindingCheck(List.of("clause-1"), List.of()));
        when(rateTableRepository.findAll(TENANT_ID, PRODUCT_ID, RateTableStatus.PUBLISHED))
                .thenReturn(List.of(mock(RateTableDefinition.class)));

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertTrue(ex.getMessage().contains("不存在"), "实际: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("未处于 ACTIVE"), "不得混入「未生效」文案，实际: " + ex.getMessage());
    }

    @Test
    void nonActiveClauseShouldBeRejectedWithStatusWording() {
        givenProduct(PricingMode.RATE_TABLE, new RateTableRef("rt-1", "LIFE_BASE", "V1", List.of("age")));
        givenClauseBinding("clause-1", new ClauseBindingCheck(List.of(), List.of("clause-1")));
        when(rateTableRepository.findAll(TENANT_ID, PRODUCT_ID, RateTableStatus.PUBLISHED))
                .thenReturn(List.of(mock(RateTableDefinition.class)));

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertTrue(ex.getMessage().contains("未处于 ACTIVE"), "实际: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("不存在"), "不得混入「不存在」文案，实际: " + ex.getMessage());
    }

    @Test
    void noClauseBindingShouldBeRejected() {
        givenProduct(PricingMode.RATE_TABLE, new RateTableRef("rt-1", "LIFE_BASE", "V1", List.of("age")));
        when(productClauseQueryService.findByProductId(PRODUCT_ID)).thenReturn(List.of());

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertTrue(ex.getMessage().contains("未绑定任何条款"), "实际: " + ex.getMessage());
    }

    /** 无已发布方案 **且** 未配置定价模式 ⇒ 拒绝：此时 legacy 路径也走不通（它只认 RATE_TABLE）。 */
    @Test
    void missingPricingModeShouldBeRejected() {
        givenProduct(null, null);
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertTrue(ex.getMessage().contains("未配置定价模式"), "实际: " + ex.getMessage());
    }

    @Test
    void actuarialModeWithoutPublishedPricingPlanShouldBeRejected() {
        givenProduct(PricingMode.ACTUARIAL_FORMULA, null);
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());
        when(calculationModelRepository.findAll(TENANT_ID, PRODUCT_ID, ActuarialDefinitionStatus.PUBLISHED))
                .thenReturn(List.of(mock(CalculationModelDefinition.class)));
        when(chargeComponentRepository.findAll(TENANT_ID, PRODUCT_ID, ActuarialDefinitionStatus.PUBLISHED))
                .thenReturn(List.of(mock(ChargeComponentDefinition.class)));

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertTrue(ex.getMessage().contains("定价方案"), "实际: " + ex.getMessage());
    }

    @Test
    void actuarialModeWithoutPublishedCalculationModelShouldBeRejected() {
        givenProduct(PricingMode.ACTUARIAL_FORMULA, null);
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());
        // 🔴 刻意不打「已发布方案」的桩：有方案即放行（方案优先），该用例要验的是**无方案**下的诊断标签
        when(chargeComponentRepository.findAll(TENANT_ID, PRODUCT_ID, ActuarialDefinitionStatus.PUBLISHED))
                .thenReturn(List.of(mock(ChargeComponentDefinition.class)));

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertTrue(ex.getMessage().contains("计算模型"), "实际: " + ex.getMessage());
    }

    @Test
    void actuarialModeWithoutPublishedChargeComponentShouldBeRejected() {
        givenProduct(PricingMode.ACTUARIAL_FORMULA, null);
        givenClauseBinding("clause-1", ClauseBindingCheck.clean());
        // 同上：不打「已发布方案」的桩
        when(calculationModelRepository.findAll(TENANT_ID, PRODUCT_ID, ActuarialDefinitionStatus.PUBLISHED))
                .thenReturn(List.of(mock(CalculationModelDefinition.class)));

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertTrue(ex.getMessage().contains("费用组件"), "实际: " + ex.getMessage());
    }

    /**
     * 收集式而非「抛第一个」：一次告知全部缺项，运营一轮补齐。
     *
     * <p>本用例同时锁定「缺项计数」——若改回逐项抛错，`缺 3 项` 断言即变红。</p>
     */
    @Test
    void multipleDeficienciesShouldBeReportedInOneGo() {
        givenProduct(PricingMode.ACTUARIAL_FORMULA, null);
        when(productClauseQueryService.findByProductId(PRODUCT_ID)).thenReturn(List.of());
        // 三类精算组件全缺 + 条款未绑定 = 4 项

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertTrue(ex.getMessage().contains("缺 4 项"), "实际: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("未绑定任何条款"), "实际: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("定价方案"), "实际: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("计算模型"), "实际: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("费用组件"), "实际: " + ex.getMessage());
    }

    @Test
    void unknownProductShouldBeRejectedWithProductNotExist() {
        when(productQueryService.findProductById(PRODUCT_ID, TENANT_ID)).thenReturn(null);

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));
        assertEquals(ProductErrorCode.PRODUCT_NOT_EXIST.getCode(), ex.getErrorCodeValue());
    }

    // ------------------------------------------------------------------ 错误码强类型回归守卫

    /**
     * 🔴 <b>错误码强类型守卫：异常基类不得把 {@code BaseErrorCode} 枚举降级为 {@code RawErrorCode}。</b>
     *
     * <p><b>本用例原为「已知缺陷钉桩」</b>（{@code knownDefect_errorCodeIsFlattenedToRawErrorCode}），
     * 钉住的是一处<b>在本模块内完全不可见</b>的跨域缺陷：{@code CommandValidationException(BaseErrorCode, ...)}
     * 内部调 {@code super(errorCode.getCode(), ...)}（首参是<b>标识符</b>而非字面量）⇒ 绑定到
     * {@code DomainException(String, String)} 这个 {@code @Deprecated} 重载 ⇒ 枚举被包成
     * {@code RawErrorCode}，其 {@code category()} 恒为 {@code INTERNAL_FAILURE}。</p>
     *
     * <p><b>后果不是「文案问题」而是「状态码失真」</b>：边界出口
     * {@code ExceptionResponseFactory.of(exception)} 用 {@code exception.getErrorCode().category()}
     * 决定 HTTP 状态 ⇒ 本应 400（{@code PARAMETER_INVALID}）的「上架缺项」返回
     * <b>500（INTERNAL_FAILURE）</b>，且 5xx 分支改用码表文案、异常里的具体缺项文案被丢弃。</p>
     *
     * <p>✅ <b>已于 2026-09-26（g01r-04）修复</b>：metadata 3 个 + billing 2 个 + regulatory 1 个 +
     * rule-engine 1 个共 <b>7 个异常类 / 16 处</b>内部转发改走
     * {@code DomainException(BaseErrorCode, String[, Throwable])} 重载
     * （{@code BusinessRuleViolationException} 另补 {@code (BaseErrorCode, String, Throwable)} 构造器，
     * 此前「覆盖文案 + 保留根因」这一组合只能落到废弃重载）。断言随之由「钉住缺陷」翻转为「期望行为」，
     * 用例名亦由 {@code knownDefect_…} 改为守卫语义。</p>
     *
     * <p>🔴 <b>本用例现为回归守卫</b>：任何把异常码重新降级为裸串/常量的改动都会在此变红。
     * 未覆盖的同类形态见 {@code DeprecatedConstructorUsageGuardTest} javadoc 的「第二条逃逸路径」
     * （码串经常量中转，棘轮视野之外）。</p>
     */
    @Test
    void shouldKeepErrorCodeStronglyTypedThroughExceptionBaseClasses() {
        givenProduct(PricingMode.RATE_TABLE, null);

        CommandValidationException ex = assertThrows(CommandValidationException.class,
                () -> validator.validate(TENANT_ID, PRODUCT_ID));

        // 枚举身份必须原样保留——这正是 HTTP 状态能渲染成 400 而非 500 的依据
        assertInstanceOf(ProductErrorCode.class, ex.getErrorCode(),
                "异常基类必须原样保留 BaseErrorCode 枚举；降级为 RawErrorCode 会让 category() 塌缩成 INTERNAL_FAILURE");
        assertEquals(ErrorCategory.PARAMETER_INVALID, ex.getErrorCode().category(),
                "PARAMETER_INVALID 语义须保留，边界出口据此渲染 400");
        // 8 位业务码本身未丢失——AC-01 的判据（码值 ∈ ProductErrorCode 码值域）不受影响
        assertEquals(ProductErrorCode.PRODUCT_VALIDATION_FAILED.getCode(), ex.getErrorCodeValue());
    }

    // ------------------------------------------------------------------ 打桩助手

    private void givenProduct(PricingMode pricingMode, RateTableRef rateTableRef) {
        ProductQueryResult product = new ProductQueryResult();
        product.setProductId(PRODUCT_ID);
        product.setPricingMode(pricingMode);
        product.setRateTableRef(rateTableRef);
        when(productQueryService.findProductById(PRODUCT_ID, TENANT_ID)).thenReturn(product);
    }

    private void givenClauseBinding(String clauseId, ClauseBindingCheck check) {
        ProductClauseQueryResult rel = new ProductClauseQueryResult();
        rel.setClauseId(clauseId);
        when(productClauseQueryService.findByProductId(PRODUCT_ID)).thenReturn(List.of(rel));
        when(clauseExistencePort.checkBindable(any(), anyString())).thenReturn(check);
    }
}
