package com.titanium.product.application.orchestration.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.CommandValidationException;
import com.titanium.product.command.AuditProductCommand;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.port.clause.ClauseBindingCheck;
import com.titanium.product.port.clause.ClauseExistencePort;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductClauseQueryService;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.CalculationModelRepository;
import com.titanium.product.repository.ChargeComponentRepository;
import com.titanium.product.repository.PricingPlanRepository;
import com.titanium.product.repository.RateTableManagementRepository;
import com.titanium.product.valueobject.rate.RateTableRef;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品上架完整性校验器（写侧编排，非领域服务）。
 *
 * <p>把「产品定义完成」与「产品可被出单消费」之间的前提一次性校验掉：条款可绑、定价依据已发布。
 * 此前 {@code AuditProductCommand} 的聚合处理器<b>只判状态</b>（{@code AUDITING}）即置
 * {@code EFFECTIVE}，可上架一个没有任何定价依据的产品，故障直到出单试算时才暴露。</p>
 *
 * <p><b>六边形定位</b>：校验需要跨聚合仓储与跨域端口，故落在 application 编排层
 * （项目规约 §3.4.5 / §3.4.8），<b>不改聚合命令处理器</b>——聚合不得依赖 Port
 * （项目规约 §3.4.4 + ArchUnit {@code domainServiceShouldNotDependOnPort}）。</p>
 *
 * <p><b>为何是「收集式」而非「抛第一个」</b>：产品上架是运营的低频人工操作，一次告知全部缺项
 * 才能一轮补齐；逐项抛错会让运营「修一个、报一个」反复往返（本批次 O-1「缺项则拒绝上架并说明缺什么」）。</p>
 *
 * <p>🔴 <b>与文档的偏差（2026-09-26 定性复核）</b>：本批次需求文档原写「返回 {@code RuleDecision}」，
 * 实读 product 域<b>无此类型</b>（{@code RuleDecision} 仅存在于 policy / claim 两域，且各为域内值对象、
 * 带险种段序号概念）；本域既有校验器范式为 {@code @Component + void validate} + 携带
 * {@link ProductErrorCode} 的异常（{@code PremiumQuoteCommandValidator} /
 * {@code PremiumCalculationCommandValidator}）。故按<b>仓内范式</b>实现，不新建领域值对象
 * （校验器落 application，引入一个该域不存在的领域 VO 属越层）。</p>
 *
 * @see com.titanium.product.application.command.ProductCommandAppService#auditProduct
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductPublishValidator {

    /** 精算公式模式下，产品必须具备的最小定价闭环（费率表模式不适用）。 */
    private static final String LABEL_CLAUSE = "条款";
    private static final String LABEL_PRICING_MODE = "定价模式";
    private static final String LABEL_RATE_TABLE = "费率表";
    private static final String LABEL_PRICING_PLAN = "定价方案";
    private static final String LABEL_CALC_MODEL = "计算模型";
    private static final String LABEL_CHARGE_COMPONENT = "费用组件";

    private final ProductQueryService            productQueryService;
    private final ProductClauseQueryService      productClauseQueryService;
    private final ClauseExistencePort            clauseExistencePort;
    private final RateTableManagementRepository  rateTableRepository;
    private final PricingPlanRepository          pricingPlanRepository;
    private final CalculationModelRepository     calculationModelRepository;
    private final ChargeComponentRepository      chargeComponentRepository;

    /**
     * 校验产品是否满足上架（{@code AUDITING → EFFECTIVE}）的全部前提。
     *
     * <p>全部缺项收集完毕后**一次性**抛出 {@link CommandValidationException}；全部满足则静默返回。</p>
     *
     * @param tenantId  租户ID
     * @param productId 产品ID
     * @throws CommandValidationException 存在任一缺项时抛出，消息中列出全部缺项
     */
    public void validate(String tenantId, String productId) {
        ProductQueryResult product = productQueryService.findProductById(productId, tenantId);
        if (product == null) {
            throw new CommandValidationException(ProductErrorCode.PRODUCT_NOT_EXIST,
                    AuditProductCommand.class.getSimpleName(), "productId", "产品不存在: " + productId);
        }
        List<String> deficiencies = new ArrayList<>();
        collectClauseDeficiencies(tenantId, productId, deficiencies);
        collectPricingDeficiencies(tenantId, product, deficiencies);
        if (!deficiencies.isEmpty()) {
            String message = "产品不满足上架条件，缺 " + deficiencies.size() + " 项: "
                    + String.join("；", deficiencies);
            log.warn("产品上架校验未通过: tenantId={}, productId={}, deficiencies={}",
                    tenantId, productId, deficiencies);
            throw new CommandValidationException(ProductErrorCode.PRODUCT_VALIDATION_FAILED,
                    AuditProductCommand.class.getSimpleName(), "productId", message);
        }
    }

    /**
     * 收集条款侧缺项：绑定非空，且所绑条款在条款域真实存在并处于 ACTIVE。
     *
     * <p>复用既有 {@link ClauseExistencePort}（<b>不新建 Port</b>）——该端口正是为「产品域无从自行判定
     * 条款主键真伪与生效状态」而设，创建与改绑定两条路径共用同一道校验。</p>
     */
    private void collectClauseDeficiencies(String tenantId, String productId, List<String> deficiencies) {
        List<ProductClauseQueryResult> clauseRels = productClauseQueryService.findByProductId(productId);
        List<String> clauseIds = clauseRels == null ? List.of()
                : clauseRels.stream().map(ProductClauseQueryResult::getClauseId)
                        .filter(Objects::nonNull).distinct().toList();
        if (clauseIds.isEmpty()) {
            deficiencies.add(LABEL_CLAUSE + "：未绑定任何条款");
            return;
        }
        ClauseBindingCheck check = clauseExistencePort.checkBindable(clauseIds, tenantId);
        if (!check.missingClauseIds().isEmpty()) {
            deficiencies.add(LABEL_CLAUSE + "：以下条款不存在 " + String.join(", ", check.missingClauseIds()));
        }
        if (!check.nonActiveClauseIds().isEmpty()) {
            deficiencies.add(LABEL_CLAUSE + "：以下条款未处于 ACTIVE "
                    + String.join(", ", check.nonActiveClauseIds()));
        }
    }

    /**
     * 收集定价侧缺项：**先判「有没有已发布定价方案」，再按模式分派**。
     *
     * <p>🔴 <b>顺序与运行期严格同序</b>：{@code PremiumQuoteApplicationService#quote} 是
     * {@code findEffective(...).map(quoteWithPricingPlan).orElseGet(quoteWithLegacyRateTable)}——
     * <b>方案优先，未命中才回退 legacy 费率表路径</b>。故「有已发布方案」本身就是通行证，
     * 与产品声明了哪种定价模式<b>无关</b>。若先按模式分派、把「未配置定价模式」直接判死，
     * 会把<b>靠方案正常算价</b>的产品误拦。</p>
     *
     * <p>🔴 <b>真机实测（2026-09-26，租户 1）</b>：EFFECTIVE 且 {@code pricing_mode} 为 NULL 的 4 个产品里，
     * {@code CFT-AUTO-COM-0904} 有 1 个已发布定价方案 + 1 个已发布费率表，<b>完全算得出保费</b>；
     * 拿模式声明拦它，本校验上线后会拦下 8/17 个已生效产品（其余 4 个是条款侧已知代价，见 §7.14(5)）。
     * 本条曾以「无条件要求模式非空」的形态被真机数据当场推翻。</p>
     *
     * <p>🔴 <b>上述判断已用真机试算逐条量化（{@code POST /api/v1/products/{id}/premium-quotes}）</b>，
     * 不再是读码推断（阳性对照 {@code V2A-AUTO-001} 返回 422「费率行未命中」，证明端点与入参正常）：</p>
     * <ul>
     *   <li>{@code CFT-AUTO-COM-0904} 返回 <b>60000104「缺少必填特征: NCD」</b>——该错误<b>只可能出自方案路径</b>
     *       {@code quoteWithPricingPlan}，即运行期确实容忍模式为空并走了方案 ⇒ 按模式拦它是<b>误拦</b>；</li>
     *   <li>{@code CFT-MED-MILLION-0904} / {@code CFT-TERM-LIFE-0904} / {@code CFT-ACC-COMP-0904}
     *       均返回 <b>60000106「定价模式不支持」</b>（{@code validateLegacyPricing} 抛出）⇒ 三者确实
     *       <b>无路可走</b>，本校验拦它们<b>不是误拦</b>；</li>
     *   <li>据此 {@code effective_would_be_blocked} 由 8 降至 7，剩余 3 个为真·不可试算、4 个为条款侧已知代价。</li>
     * </ul>
     *
     * <p>⚠️ <b>本判据的范围边界</b>：只判<b>产品配置完整性</b>（配置齐不齐），不判<b>请求完整性</b>
     * （调用方有没有给 NCD、费率行能否命中）——后者是试算时的入参问题，不属上架门槛。</p>
     *
     * <p>无方案时才有「模式」可言：legacy 路径 {@code validateLegacyPricing} 要求模式<b>恰为
     * {@code RATE_TABLE}</b> 且 {@code rateTableRef} 可用——故其余模式在无方案时确实无路可走。</p>
     *
     * <p>⚠️ <b>已知残余（只放过、不误拦）</b>：本判据不校验方案的 {@code productVersion} 与
     * {@code mode} 是否与产品一致（运行期 {@code validatePricingPlanBinding} 会校验并在不一致时抛
     * {@code PRICING_PLAN_VALIDATION_FAILED}）。查全仓仓储可得该信息，但那属「多拦」方向的收紧，
     * 须先有真机数据证明不会误拦，本批次不做。</p>
     */
    private void collectPricingDeficiencies(String tenantId, ProductQueryResult product,
                                            List<String> deficiencies) {
        if (!pricingPlanRepository
                .findAll(tenantId, product.getProductId(), PricingPlanStatus.PUBLISHED).isEmpty()) {
            return;
        }
        PricingMode mode = product.getPricingMode();
        if (PricingMode.RATE_TABLE == mode) {
            collectRateTableDeficiencies(tenantId, product, deficiencies);
            return;
        }
        if (PricingMode.ACTUARIAL_FORMULA == mode) {
            deficiencies.add(LABEL_PRICING_PLAN + "：未存在已发布（PUBLISHED）的定价方案；"
                    + "而无方案时运行期只回退费率表路径，该路径仅接受 RATE_TABLE 模式");
            collectActuarialDeficiencies(tenantId, product.getProductId(), deficiencies);
            return;
        }
        deficiencies.add(LABEL_PRICING_MODE + "：未配置定价模式，且无已发布（PUBLISHED）的定价方案");
    }

    /**
     * 费率表模式（**调用前提：已确认无已发布定价方案**）。
     *
     * <p>此时运行期只剩 legacy 费率表路径，它要求 {@code product.rateTableRef} 的 tableCode/version
     * 均非空白（{@code validateLegacyPricing}），再要求该引用能取到生效快照。故两条缺项：
     * 引用为空（无路可走）、引用有值但无已发布版本。</p>
     */
    private void collectRateTableDeficiencies(String tenantId, ProductQueryResult product,
                                              List<String> deficiencies) {
        RateTableRef ref = product.getRateTableRef();
        if (ref == null || isBlank(ref.tableCode()) || isBlank(ref.version())) {
            deficiencies.add(LABEL_RATE_TABLE + "：既无已发布（PUBLISHED）的定价方案，"
                    + "也未配置费率表引用——两条定价路径均不可用，出单试算必失败");
            return;
        }
        boolean published = !rateTableRepository
                .findAll(tenantId, product.getProductId(), RateTableStatus.PUBLISHED).isEmpty();
        if (!published) {
            deficiencies.add(LABEL_RATE_TABLE + "：引用的费率表未存在已发布（PUBLISHED）的版本");
        }
    }

    /** 字符串空白判定（{@code null} 与纯空白同视）。 */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 精算公式模式下**额外的可执行提示**：计算模型、费用组件须存在已发布版本。
     *
     * <p>🔴 <b>定位是「诊断标签」而非「独立判据」</b>：调用点已因「无已发布方案」拦下，
     * 故这两条<b>不会改变结论</b>，只是告诉运营补数据时还差哪些版本。全仓真机数据里
     * ACTUARIAL_FORMULA 产品数为 0，本分支无实测样本可校准，故不赋予它独立裁决权
     * （避免出现「守则真、却给它一个我无法验证的否决权」）。</p>
     *
     * <p>🔴 <b>为何不要求税率 / 退保价值策略 / 动态因子</b>：三者按产品类型可选
     * （非分红型寿险无退保价值策略、多数产品无动态因子），产品级强行要求会误拦合规产品；
     * 且「被引用即须已发布」已由各自发布路径（{@code validatePublishedComponents} /
     * {@code validateTaxBaseComponents}）保证，在产品级重复拦截属重复门禁。</p>
     */
    private void collectActuarialDeficiencies(String tenantId, String productId, List<String> deficiencies) {
        if (calculationModelRepository
                .findAll(tenantId, productId, ActuarialDefinitionStatus.PUBLISHED).isEmpty()) {
            deficiencies.add(LABEL_CALC_MODEL + "：未存在已发布（PUBLISHED）的计算模型版本");
        }
        if (chargeComponentRepository
                .findAll(tenantId, productId, ActuarialDefinitionStatus.PUBLISHED).isEmpty()) {
            deficiencies.add(LABEL_CHARGE_COMPONENT + "：未存在已发布（PUBLISHED）的费用组件版本");
        }
    }
}
