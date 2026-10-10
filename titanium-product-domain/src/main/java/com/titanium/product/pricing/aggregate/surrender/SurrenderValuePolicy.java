package com.titanium.product.pricing.aggregate.surrender;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.SurrenderFactorType;
import com.titanium.product.common.enums.SurrenderRefundBasis;
import com.titanium.product.common.enums.SurrenderRefundType;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.premium.SurrenderBalanceFacts;
import com.titanium.product.valueobject.pricing.premium.SurrenderFactor;
import com.titanium.product.valueobject.pricing.premium.SurrenderLoanFacts;
import com.titanium.product.valueobject.pricing.premium.SurrenderPremiumFacts;
import com.titanium.product.valueobject.pricing.premium.SurrenderRefundBreakdown;
import com.titanium.product.valueobject.pricing.premium.SurrenderValueOutcome;

import lombok.Getter;

/** Product 版本化退保价值策略。 */
@Getter
public final class SurrenderValuePolicy {

    private static final BigDecimal ONE = BigDecimal.ONE;
    /** 未满期保费按日比例折算的年度基准天数（365 日法，g05-02 业务裁示） */
    private static final long DAYS_PER_YEAR = 365L;

    private final String policyId;
    private final String productId;
    private final String policyCode;
    private final String policyVersion;
    private final int policyYear;
    private final int coolingOffDays;
    private final BigDecimal cashValueRate;
    private final BigDecimal internalCostRetentionRate;
    /**
     * 退保手续费率：{@code null} 表示**该产品未配置退保手续费**（不适用，不是 0），
     * 非 null 时须落在 [0,1]。
     * <p>
     * 🔴 与 {@link #internalCostRetentionRate} **不是同一概念**（R-2 裁示，2026-09-30）：
     * 留存率是保险公司的**内部成本口径**（已由现金价值率间接反映，仅随结果披露、不参与扣减），
     * 手续费率是**从客户应退金额中实际扣减**的费率。
     * </p>
     */
    private final BigDecimal handlingFeeRate;
    private final SurrenderRefundBasis refundBasis;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final String tenantId;
    private ActuarialDefinitionStatus status;
    private String contentHash;

    private SurrenderValuePolicy(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            int policyYear,
            int coolingOffDays,
            BigDecimal cashValueRate,
            BigDecimal internalCostRetentionRate,
            BigDecimal handlingFeeRate,
            SurrenderRefundBasis refundBasis,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        this.policyId = requireText(policyId, "退保价值策略ID");
        this.productId = requireText(productId, "产品ID");
        this.policyCode = requireText(policyCode, "退保价值策略编码").toUpperCase(Locale.ROOT);
        this.policyVersion = requireText(policyVersion, "退保价值策略版本");
        if (policyYear < 1 || coolingOffDays < 0) {
            throw invalid("保单年度必须大于0且犹豫期天数不能为负数");
        }
        this.policyYear = policyYear;
        this.coolingOffDays = coolingOffDays;
        this.cashValueRate = requireRate(cashValueRate, "现金价值率");
        this.internalCostRetentionRate = requireRate(internalCostRetentionRate, "内部成本留存率");
        // 手续费率可空：null = 该产品不适用（未配置手续费），非 null 须落在 0~1
        this.handlingFeeRate = requireOptionalRate(handlingFeeRate, "退保手续费率");
        // 退费基准决定应退金额走哪条口径，缺省即无从计算，故构造期强制
        this.refundBasis = Objects.requireNonNull(refundBasis, "退费基准不能为空");
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.tenantId = requireText(tenantId, "租户ID");
        this.status = Objects.requireNonNull(status, "生命周期状态不能为空");
        this.contentHash = contentHash == null ? "" : contentHash;
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid("失效时间必须晚于生效时间");
        }
    }

    public static SurrenderValuePolicy createDraft(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            int policyYear,
            int coolingOffDays,
            BigDecimal cashValueRate,
            BigDecimal internalCostRetentionRate,
            BigDecimal handlingFeeRate,
            SurrenderRefundBasis refundBasis,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId) {
        return new SurrenderValuePolicy(
                policyId, productId, policyCode, policyVersion, policyYear, coolingOffDays,
                cashValueRate, internalCostRetentionRate, handlingFeeRate, refundBasis, effectiveFrom, effectiveTo,
                tenantId, ActuarialDefinitionStatus.DRAFT, "");
    }

    public static SurrenderValuePolicy restore(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            int policyYear,
            int coolingOffDays,
            BigDecimal cashValueRate,
            BigDecimal internalCostRetentionRate,
            BigDecimal handlingFeeRate,
            SurrenderRefundBasis refundBasis,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        return new SurrenderValuePolicy(
                policyId, productId, policyCode, policyVersion, policyYear, coolingOffDays,
                cashValueRate, internalCostRetentionRate, handlingFeeRate, refundBasis, effectiveFrom, effectiveTo,
                tenantId, status, contentHash);
    }

    public String approve() {
        requireStatus(ActuarialDefinitionStatus.DRAFT, "只有草稿退保价值策略可以审批");
        contentHash = hash(canonicalContent());
        status = ActuarialDefinitionStatus.APPROVED;
        return contentHash;
    }

    public void publish() {
        requireStatus(ActuarialDefinitionStatus.APPROVED, "只有已审批退保价值策略可以发布");
        status = ActuarialDefinitionStatus.PUBLISHED;
    }

    public void retire() {
        requireStatus(ActuarialDefinitionStatus.PUBLISHED, "只有已发布退保价值策略可以退役");
        status = ActuarialDefinitionStatus.RETIRED;
    }

    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return status == ActuarialDefinitionStatus.PUBLISHED && businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }

    /**
     * 现金价值口径：只读现金价值查询专用（退保试算不得走此入口）。
     * <p>
     * 🔴 与 {@link #calculate} 的关系：两者共用同一份因子组装与加总（同一私有方法），区别只在
     * **保费事实、贷款事实与账户余额事实的可得性**。本入口不提供保费事实，故退费基准为未满期保费时
     * 现金价值因子恒为 {@code NONE}/0——这正是「两者择一」基准下该类产品的现金价值定义（无现金价值），
     * 而非降级近似。同理不提供贷款事实与账户余额事实：本入口只回答「现金价值是多少」，不产生任何
     * 退款，故贷款、欠缴保费、红利及账户价值三个因子恒为 {@code NONE}/0（不适用），既不抵扣也不因
     * 取不到而拒绝。
     * </p>
     *
     * @param originalCustomerPayable 已确认客户应付
     * @param policyEffectiveDate     保单生效日
     * @param valuationDate           估值日
     * @param scale                   金额精度
     * @param roundingMode            舍入模式
     * @return 现金价值结果（含因子明细）
     */
    public SurrenderValueOutcome cashValueAt(
            BigDecimal originalCustomerPayable,
            LocalDate policyEffectiveDate,
            LocalDate valuationDate,
            int scale,
            RoundingMode roundingMode) {
        return outcome(originalCustomerPayable, policyEffectiveDate, valuationDate,
                null, null, null, false, scale, roundingMode);
    }

    /**
     * 退费口径：退保确认专用，按退费基准择一产生应退金额。
     * <p>
     * 🔴 应退金额由**退费因子加总**产生（{@code refundAmount = breakdown.netAmount()}），
     * 不再是「一次乘法」的直接产物：现金价值与未满期保费是明细中的两个因子，贷款本息、欠费、
     * 红利账户价值、手续费等因子随之扩展（g05-03~05）。加总恒等式与净额为负的拒绝均由
     * {@link SurrenderRefundBreakdown} 内聚。
     * </p>
     * <p>
     * 🔴 <b>六个因子始终出现在明细中</b>：退费基准未选中的那一个方向为 {@code NONE}、金额为 0
     * （表示「本产品不适用」），缴费期满或趸缴时未满期保费为 {@code CREDIT}/0（表示「适用但为
     * 0」）——「为 0」与「缺项」在明细上必须可区分，缺项会让运营无法判断是口径不适用还是取数失败。
     * 贷款未偿本息、欠缴保费与红利及账户价值三个因子与基准无关（客户负债先行抵扣、客户权益如数
     * 退还），两条分支都给出实际方向：{@code DEBIT}/{@code 0}（无欠费）、{@code DEBIT}/正额、
     * {@code CREDIT}/{@code 0}（无权益余额，含非分红/非万能险）、{@code CREDIT}/正额。
     * 退保手续费（g05-05）与基准无关但**与是否犹豫期有关**（犹豫期不扣）：未配置费率为
     * {@code NONE}/0「不适用」、已配置且犹豫期内为 {@code DEBIT}/0「适用但为 0」、其余为
     * {@code DEBIT}/正额。
     * </p>
     *
     * @param originalCustomerPayable 已确认客户应付
     * @param policyEffectiveDate     保单生效日
     * @param surrenderDate           退保申请日
     * @param premiumFacts            保费事实（未满期保费基准所需，由保单域下发）
     * @param loanFacts               保单贷款未偿本息事实（由保单域下发，退费时先行抵扣）
     * @param balanceFacts            账户余额事实（欠缴保费与红利/账户价值，由保单域下发）
     * @param scale                   金额精度
     * @param roundingMode            舍入模式
     * @return 退费结果（含因子明细）
     */
    public SurrenderValueOutcome calculate(
            BigDecimal originalCustomerPayable,
            LocalDate policyEffectiveDate,
            LocalDate surrenderDate,
            SurrenderPremiumFacts premiumFacts,
            SurrenderLoanFacts loanFacts,
            SurrenderBalanceFacts balanceFacts,
            int scale,
            RoundingMode roundingMode) {
        return outcome(originalCustomerPayable, policyEffectiveDate, surrenderDate,
                Objects.requireNonNull(premiumFacts, "退费口径必须提供保费事实：未满期保费基准依赖它"),
                Objects.requireNonNull(loanFacts, "退费口径必须提供贷款事实：退费时先行抵扣未偿本息"),
                Objects.requireNonNull(balanceFacts, "退费口径必须提供账户余额事实：欠缴抵扣与权益退还依赖它"),
                true, scale, roundingMode);
    }

    /**
     * 因子组装与加总的唯一实现：现金价值口径与退费口径共用，杜绝两条口径各自演化而漂移。
     *
     * @param premiumFacts 保费事实；为 {@code null} 表示调用方为现金价值口径（无保费语境）
     * @param loanFacts    贷款事实；为 {@code null} 表示调用方为现金价值口径（无退费语境）
     * @param balanceFacts 账户余额事实；为 {@code null} 表示调用方为现金价值口径（无退费语境）
     * @param refundContext 🔴 调用方是否处于**退费语境**（会产生退款指令）。**显式传入、不得由
     *                      {@code loanFacts == null} 之类推得**——用「某字段恰好为空」冒充「上下文」
     *                     会把两个态合成一个（R-12 同型缺陷）。只读现金价值口径传 {@code false}：
     *                      本入口不产生任何退款，故手续费因子恒为 {@code NONE}/0。
     */
    private SurrenderValueOutcome outcome(
            BigDecimal originalCustomerPayable,
            LocalDate policyEffectiveDate,
            LocalDate surrenderDate,
            SurrenderPremiumFacts premiumFacts,
            SurrenderLoanFacts loanFacts,
            SurrenderBalanceFacts balanceFacts,
            boolean refundContext,
            int scale,
            RoundingMode roundingMode) {
        if (originalCustomerPayable == null || originalCustomerPayable.signum() < 0
                || policyEffectiveDate == null || surrenderDate == null || surrenderDate.isBefore(policyEffectiveDate)
                || scale < 0 || roundingMode == null) {
            throw invalid("退保价值计算输入不完整或不合法");
        }
        boolean withinCoolingOff = !surrenderDate.isAfter(policyEffectiveDate.plusDays(coolingOffDays));
        SurrenderRefundType refundType = withinCoolingOff
                ? SurrenderRefundType.COOLING_OFF_FULL_REFUND : refundTypeOfBasis();
        BigDecimal refundRate = withinCoolingOff ? ONE : cashValueRateOfBasis();
        // 🔴 应退基准金额**只算一次**，基准因子与退保手续费共用同一值：两处各乘一遍必然漂移，
        // 届时手续费基数与明细上显示的应退基准对不上，谁也解释不了差额（R-2 裁示③）。
        BigDecimal refundBase = withinCoolingOff
                ? refundBaseAmount(originalCustomerPayable, ONE, scale, roundingMode)
                : refundBaseOf(originalCustomerPayable, surrenderDate, premiumFacts, scale, roundingMode);
        List<SurrenderFactor> factors = withinCoolingOff
                ? List.of(
                        new SurrenderFactor(SurrenderFactorType.CASH_VALUE, ChargeDirection.CREDIT,
                                refundBase,
                                SurrenderValueOutcome.EXPLAIN_COOLING_OFF_FULL_REFUND),
                        unappliedUnearnedPremiumFactor(),
                        loanPrincipalInterestFactor(loanFacts, scale, roundingMode),
                        overduePremiumFactor(balanceFacts, scale, roundingMode),
                        dividendAccountValueFactor(balanceFacts, scale, roundingMode),
                        handlingFeeFactor(refundBase, refundContext, true, scale, roundingMode))
                : List.of(
                        cashValueFactor(refundBase),
                        unearnedPremiumFactor(refundBase),
                        loanPrincipalInterestFactor(loanFacts, scale, roundingMode),
                        overduePremiumFactor(balanceFacts, scale, roundingMode),
                        dividendAccountValueFactor(balanceFacts, scale, roundingMode),
                        handlingFeeFactor(refundBase, refundContext, false, scale, roundingMode));

        BigDecimal netAmount = factors.stream()
                .map(SurrenderFactor::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal retainedCustomerAmount = originalCustomerPayable.subtract(netAmount);
        if (retainedCustomerAmount.signum() < 0) {
            // 明细加总已守卫「净额为负」，此处守卫的是另一件事：净额不得超过客户实付，否则客户
            // 保留金额为负（即退得比交的多）。单独拦一道是为了给出可执行的文案与因由。
            throw invalid("退费金额 " + netAmount.toPlainString() + " 超过客户应付 "
                    + originalCustomerPayable.toPlainString() + "，客户保留金额为负");
        }
        SurrenderRefundBreakdown breakdown = SurrenderRefundBreakdown.of(factors, retainedCustomerAmount);
        return new SurrenderValueOutcome(
                refundType, withinCoolingOff, refundRate, breakdown.netAmount(),
                breakdown.retainedCustomerAmount(),
                withinCoolingOff ? BigDecimal.ZERO : internalCostRetentionRate, breakdown);
    }

    /** 退费基准对应的退费类型。 */
    private SurrenderRefundType refundTypeOfBasis() {
        return refundBasis == SurrenderRefundBasis.UNEARNED_PREMIUM
                ? SurrenderRefundType.UNEARNED_PREMIUM : SurrenderRefundType.CASH_VALUE;
    }

    /** 退费基准对应的「实际适用退费率」：未满期保费基准不适用费率，报 0 而非沿用现金价值率。 */
    private BigDecimal cashValueRateOfBasis() {
        return refundBasis == SurrenderRefundBasis.UNEARNED_PREMIUM ? BigDecimal.ZERO : cashValueRate;
    }

    /**
     * 应退基准金额的**全仓唯一计算点**（按基准择一）：现金价值 ÷ 未满期保费二者取一。
     * <p>
     * 🔴 基准因子与退保手续费共用本方法的返回值（调用点先取一次再分发），不得在别处再乘一遍。
     * </p>
     * <p>
     * 🔴 未满期保费基准下才会触碰保费事实：现金价值基准不要求它可得，故本方法按基准短路，
     * 不给「现金价值口径」凭空加上一份保费事实前置。
     * </p>
     *
     * @param customerPayable 已确认客户应付
     * @param surrenderDate   退保申请日
     * @param premiumFacts    保费事实（仅未满期保费基准需要）
     * @param scale           金额精度
     * @param roundingMode    舍入模式
     * @return 应退基准金额
     */
    private BigDecimal refundBaseOf(
            BigDecimal customerPayable,
            LocalDate surrenderDate,
            SurrenderPremiumFacts premiumFacts,
            int scale,
            RoundingMode roundingMode) {
        if (refundBasis == SurrenderRefundBasis.UNEARNED_PREMIUM) {
            return unearnedPremiumOf(surrenderDate, premiumFacts, scale, roundingMode);
        }
        return refundBaseAmount(customerPayable, cashValueRate, scale, roundingMode);
    }

    /** 现金价值因子：非本基准时方向为 NONE、金额 0（本产品不适用），而非从明细中消失。 */
    private SurrenderFactor cashValueFactor(BigDecimal refundBase) {
        if (refundBasis == SurrenderRefundBasis.UNEARNED_PREMIUM) {
            return new SurrenderFactor(SurrenderFactorType.CASH_VALUE, ChargeDirection.NONE,
                    BigDecimal.ZERO, SurrenderValueOutcome.EXPLAIN_CASH_VALUE);
        }
        return new SurrenderFactor(SurrenderFactorType.CASH_VALUE, ChargeDirection.CREDIT,
                refundBase, SurrenderValueOutcome.EXPLAIN_CASH_VALUE);
    }

    /** 未满期保费因子：非本基准时为 NONE/0；本基准但已缴清／趸缴时为 CREDIT/0（适用但为 0）。 */
    private SurrenderFactor unearnedPremiumFactor(BigDecimal refundBase) {
        if (refundBasis != SurrenderRefundBasis.UNEARNED_PREMIUM) {
            return unappliedUnearnedPremiumFactor();
        }
        return new SurrenderFactor(SurrenderFactorType.UNEARNED_PREMIUM, ChargeDirection.CREDIT,
                refundBase, SurrenderValueOutcome.EXPLAIN_UNEARNED_PREMIUM);
    }

    private static SurrenderFactor unappliedUnearnedPremiumFactor() {
        return new SurrenderFactor(SurrenderFactorType.UNEARNED_PREMIUM, ChargeDirection.NONE,
                BigDecimal.ZERO, SurrenderValueOutcome.EXPLAIN_UNEARNED_PREMIUM);
    }

    /**
     * 保单贷款未偿本息因子：退费时**先行抵扣**（DEBIT）。
     * <p>
     * 🔴 贷款是客户对保险公司的负债，与退费基准无关——无论退费走现金价值还是未满期保费（含犹豫期
     * 全额退），未偿本息都应先冲抵应退金额，故本因子在**两条分支中都出现**，不参与「基准择一」。
     * </p>
     * <p>
     * 🔴 <b>三态必须可区分（AC-05 的承载点）</b>：
     * <ul>
     *   <li>{@code loanFacts == null}——只读现金价值口径，无退费语境：报 {@code NONE}/0「不适用」；</li>
     *   <li>事实不可得（保单域取不到贷款余额）：**拒绝计算**并抛
     *       {@code SURRENDER_LOAN_FACTS_MISSING}，绝不按「无贷款」以 0 抵扣；</li>
     *   <li>无未结贷款（余额确为 0，业务事实）：报 {@code DEBIT}/0「适用但为 0」——与上一条
     *       「取不到」严格区分。</li>
     * </ul>
     * 抵扣后净额为负（贷款超过应退金额）由 {@link SurrenderRefundBreakdown} 拒绝并转人工核价，
     * 本方法不预先截断、也不以 0 兜底。
     * </p>
     *
     * @param loanFacts    贷款事实；{@code null} 表示现金价值口径调用（无退费语境）
     * @param scale        金额精度
     * @param roundingMode 舍入模式
     * @return 贷款未偿本息因子
     */
    private static SurrenderFactor loanPrincipalInterestFactor(
            SurrenderLoanFacts loanFacts, int scale, RoundingMode roundingMode) {
        if (loanFacts == null) {
            return new SurrenderFactor(SurrenderFactorType.LOAN_PRINCIPAL_INTEREST, ChargeDirection.NONE,
                    BigDecimal.ZERO, SurrenderValueOutcome.EXPLAIN_LOAN_PRINCIPAL_INTEREST);
        }
        if (!loanFacts.complete()) {
            throw new PricingDomainException(ProductErrorCode.SURRENDER_LOAN_FACTS_MISSING,
                    "退费缺少贷款未偿本息事实，不得按「无贷款」以 0 抵扣");
        }
        return new SurrenderFactor(SurrenderFactorType.LOAN_PRINCIPAL_INTEREST, ChargeDirection.DEBIT,
                loanFacts.totalOutstanding().setScale(scale, roundingMode),
                SurrenderValueOutcome.EXPLAIN_LOAN_PRINCIPAL_INTEREST);
    }

    /**
     * 欠缴保费因子：退费时**先行抵扣**（DEBIT）。
     * <p>
     * 🔴 客户欠缴的保费是客户对保险公司的负债，与退费基准无关——无论退费走现金价值还是未满期保费
     * （含犹豫期全额退），欠缴保费都应先冲抵应退金额，故本因子在**两条分支中都出现**，不参与
     * 「基准择一」，与贷款未偿本息因子同规则。
     * </p>
     * <p>
     * 🔴 <b>三态必须可区分（AC-05 的承载点）</b>：
     * <ul>
     *   <li>{@code balanceFacts == null}——只读现金价值口径，无退费语境：报 {@code NONE}/0「不适用」；</li>
     *   <li>事实不可得（保单域取不到欠费金额）：**拒绝计算**并抛
     *       {@code SURRENDER_BALANCE_FACTS_MISSING}，绝不按「无欠费」以 0 抵扣；</li>
     *   <li>无欠费（金额确为 0，业务事实）：报 {@code DEBIT}/0「适用但为 0」——与上一条「取不到」
     *       严格区分。</li>
     * </ul>
     * </p>
     *
     * @param balanceFacts 账户余额事实；{@code null} 表示现金价值口径调用（无退费语境）
     * @param scale        金额精度
     * @param roundingMode 舍入模式
     * @return 欠缴保费因子
     */
    private static SurrenderFactor overduePremiumFactor(
            SurrenderBalanceFacts balanceFacts, int scale, RoundingMode roundingMode) {
        if (balanceFacts == null) {
            return new SurrenderFactor(SurrenderFactorType.OVERDUE_PREMIUM, ChargeDirection.NONE,
                    BigDecimal.ZERO, SurrenderValueOutcome.EXPLAIN_OVERDUE_PREMIUM);
        }
        requireBalanceFacts(balanceFacts, "欠缴保费");
        return new SurrenderFactor(SurrenderFactorType.OVERDUE_PREMIUM, ChargeDirection.DEBIT,
                balanceFacts.overduePremium().setScale(scale, roundingMode),
                SurrenderValueOutcome.EXPLAIN_OVERDUE_PREMIUM);
    }

    /**
     * 红利及账户价值因子：退费时**一并退还**（CREDIT）。
     * <p>
     * 🔴 红利余额与投资账户价值同为客户在保险公司账上的权益，退保解除合同时应一并退还，与退费基准
     * 无关，故本因子在**两条分支中都出现**。两者在 {@link SurrenderBalanceFacts} 中合并为
     * 「红利及账户价值」一个因子金额——合并口径属本域（退费口径权威域），保全域与适配器只搬运
     * 原始分量、不得先行求和。
     * </p>
     * <p>
     * 🔴 <b>三态必须可区分（AC-05 的承载点）</b>：
     * <ul>
     *   <li>{@code balanceFacts == null}——只读现金价值口径，无退费语境：报 {@code NONE}/0「不适用」；</li>
     *   <li>事实不可得（保单域取不到余额）：**拒绝计算**并抛
     *       {@code SURRENDER_BALANCE_FACTS_MISSING}，绝不按「无余额」以 0 退还；</li>
     *   <li>无权益余额（金额确为 0，业务事实，含非分红/非万能险）：报 {@code CREDIT}/0「适用但为
     *       0」——与上一条「取不到」严格区分。</li>
     * </ul>
     * 🔴 <b>「非分红/非万能险」不报 {@code NONE}</b>：本域在退费上下文里不持有产品形态判据，
     * 无法区分「该险种无此概念」与「有概念但余额为 0」；用 {@code NONE} 等于**宣称一个没有依据的
     * 事实**。故一律按「适用」呈现（{@code CREDIT}，0 即 0），「无此概念」是**业务合法**的 0，
     * 不是错误，也**不与「取数失败」共用任何错误码**。
     * </p>
     *
     * @param balanceFacts 账户余额事实；{@code null} 表示现金价值口径调用（无退费语境）
     * @param scale        金额精度
     * @param roundingMode 舍入模式
     * @return 红利及账户价值因子
     */
    private static SurrenderFactor dividendAccountValueFactor(
            SurrenderBalanceFacts balanceFacts, int scale, RoundingMode roundingMode) {
        if (balanceFacts == null) {
            return new SurrenderFactor(SurrenderFactorType.DIVIDEND_ACCOUNT_VALUE, ChargeDirection.NONE,
                    BigDecimal.ZERO, SurrenderValueOutcome.EXPLAIN_DIVIDEND_ACCOUNT_VALUE);
        }
        requireBalanceFacts(balanceFacts, "红利及账户价值");
        return new SurrenderFactor(SurrenderFactorType.DIVIDEND_ACCOUNT_VALUE, ChargeDirection.CREDIT,
                balanceFacts.totalDividendAccountValue().setScale(scale, roundingMode),
                SurrenderValueOutcome.EXPLAIN_DIVIDEND_ACCOUNT_VALUE);
    }

    /**
     * 账户余额事实完整性守卫（使用点失败关闭）。
     * <p>
     * 🔴 两个因子共用同一守卫：三项事实同生同死（见 {@link SurrenderBalanceFacts} 构造期约束），
     * 故任一因子取不到即整笔退费不可算——不得只拒其中一项、让另一项以 0 蒙混过关。
     * </p>
     *
     * @param balanceFacts 账户余额事实（调用点保证非 null）
     * @param field        触发失败的因子名（用于可执行的文案）
     */
    private static void requireBalanceFacts(SurrenderBalanceFacts balanceFacts, String field) {
        if (!balanceFacts.complete()) {
            throw new PricingDomainException(ProductErrorCode.SURRENDER_BALANCE_FACTS_MISSING,
                    "退费缺少账户余额事实（" + field + "），不得按「无欠费、无余额」以 0 兜底");
        }
    }

    /**
     * 退保手续费因子：退费时**从应退金额中扣减**（DEBIT）。
     * <p>
     * 🔴 计费基数 = **应退基准金额**（{@link #refundBaseOf} 的返回值，由调用点算好传入——全仓
     * **不出现第二个退费乘算实现**，也不允许代码内写字面量比率），费率 = 本策略配置的
     * {@link #handlingFeeRate}（R-2 裁示，2026-09-30）。
     * </p>
     * <p>
     * 🔴 <b>四条路径必须可区分（AC-05 同规）</b>：
     * <ul>
     *   <li>非退费语境（只读现金价值入口，{@code refundContext == false}）→ {@code NONE}/0
     *       「不适用」：该入口不产生任何退款指令，本就不存在「收手续费」这件事；</li>
     *   <li>退费语境但策略**未配置**手续费率（{@code handlingFeeRate == null}）→ {@code NONE}/0
     *       「不适用」：该产品不收退保手续费——**不以 0 冒充已配置**；</li>
     *   <li>退费语境、已配置费率、**犹豫期内** → {@code DEBIT}/0「**适用但为 0**」：犹豫期全额退，
     *       不扣手续费（R-2 裁示），与上一条的「不适用」在明细上必须可区分；</li>
     *   <li>退费语境、已配置费率、犹豫期外 → {@code DEBIT}/应退基准金额 × 费率。</li>
     * </ul>
     * 手续费使净额为负（手续费与贷款/欠费扣项合计超过应退合计）由
     * {@link SurrenderRefundBreakdown} 拒绝并转人工核价，本方法不预先截断、也不以 0 兜底。
     * </p>
     *
     * @param refundBase      应退基准金额（由 {@link #refundBaseOf} 算好传入，与基准因子同值）
     * @param refundContext   是否退费语境（会产生退款指令）；只读现金价值口径为 {@code false}
     * @param withinCoolingOff 是否犹豫期内
     * @param scale           金额精度
     * @param roundingMode    舍入模式
     * @return 退保手续费因子
     */
    private SurrenderFactor handlingFeeFactor(
            BigDecimal refundBase,
            boolean refundContext,
            boolean withinCoolingOff,
            int scale,
            RoundingMode roundingMode) {
        if (!refundContext || handlingFeeRate == null) {
            return new SurrenderFactor(SurrenderFactorType.HANDLING_FEE, ChargeDirection.NONE,
                    BigDecimal.ZERO, SurrenderValueOutcome.EXPLAIN_HANDLING_FEE);
        }
        BigDecimal fee = withinCoolingOff
                ? BigDecimal.ZERO
                : refundBaseAmount(refundBase, handlingFeeRate, scale, roundingMode);
        return new SurrenderFactor(SurrenderFactorType.HANDLING_FEE, ChargeDirection.DEBIT, fee,
                SurrenderValueOutcome.EXPLAIN_HANDLING_FEE);
    }

    /**
     * 未满期保费 = 年保费 × 剩余保障天数 / 365（按日比例，全仓唯一实现）。
     * <p>
     * 🔴 剩余天数取「退保日 → 保障止期」的自然日差，并**双向截断到 [0, 365]**：下界保证保费用尽
     * 时不出现负额（AC-04 ③），上界保证未满期保费不超过年保费自身（多年度保障期、或退保日早于
     * 保障起期时，不截断会算出超过一年保费的退费）。
     * </p>
     * <p>
     * 🔴 缴费期满（含趸缴）**直接返回 0，不要求保费事实**——「已缴清」本身就是该项为 0 的充分
     * 理由；反过来，事实不齐备（保单未落年保费或保障止期）则在**使用点**失败关闭，不以 0 兜底：
     * 0 是「确定没有未满期保费」，取不到是另一回事。
     * </p>
     *
     * @param surrenderDate 退保申请日
     * @param premiumFacts  保费事实；{@code null} 表示现金价值口径调用（无保费语境）
     */
    private static BigDecimal unearnedPremiumOf(
            LocalDate surrenderDate,
            SurrenderPremiumFacts premiumFacts,
            int scale,
            RoundingMode roundingMode) {
        if (premiumFacts == null || premiumFacts.premiumPaidUp()) {
            return BigDecimal.ZERO;
        }
        if (!premiumFacts.complete()) {
            throw new PricingDomainException(ProductErrorCode.SURRENDER_PREMIUM_FACTS_MISSING,
                    "未满期保费基准缺少保费事实（年保费或保障止期不可得），不得以 0 兜底");
        }
        long remainingDays = ChronoUnit.DAYS.between(surrenderDate, premiumFacts.coverageEndDate());
        long effectiveDays = Math.min(Math.max(remainingDays, 0L), DAYS_PER_YEAR);
        return premiumFacts.annualPremium()
                .multiply(BigDecimal.valueOf(effectiveDays))
                .divide(BigDecimal.valueOf(DAYS_PER_YEAR), scale, roundingMode);
    }

    /** 退费基准金额：客户应付 × 本策略适用退费率（犹豫期内为全额），按策略精度舍入。 */
    private static BigDecimal refundBaseAmount(
            BigDecimal customerPayable, BigDecimal refundRate, int scale, RoundingMode roundingMode) {
        return customerPayable.multiply(refundRate).setScale(scale, roundingMode);
    }

    private String canonicalContent() {
        return String.join("|", productId, policyCode, policyVersion, Integer.toString(policyYear),
                Integer.toString(coolingOffDays), decimal(cashValueRate), decimal(internalCostRetentionRate),
                handlingFeeRate == null ? "*" : decimal(handlingFeeRate),
                refundBasis.getCode(),
                effectiveFrom.toString(), effectiveTo == null ? "*" : effectiveTo.toString(), tenantId);
    }

    private void requireStatus(ActuarialDefinitionStatus expected, String message) {
        if (status != expected) {
            throw invalid(message);
        }
    }

    private static BigDecimal requireRate(BigDecimal value, String field) {
        if (value == null || value.signum() < 0 || value.compareTo(ONE) > 0) {
            throw invalid(field + "必须在0到1之间");
        }
        return value;
    }

    /**
     * 可空费率校验：{@code null} 表示「该产品不适用」（未配置），此时**不**回落为 0——
     * 「不适用」与「适用但为 0」是两种业务事实，必须在明细上可区分（AC-05 同规）。
     *
     * @param value 费率；{@code null} 表示不适用
     * @param field 字段名（用于可执行文案）
     * @return 原值或 {@code null}
     */
    private static BigDecimal requireOptionalRate(BigDecimal value, String field) {
        return value == null ? null : requireRate(value, field);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + "不能为空");
        }
        return value.trim();
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
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
        return new PricingDomainException(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED, detail);
    }
}
