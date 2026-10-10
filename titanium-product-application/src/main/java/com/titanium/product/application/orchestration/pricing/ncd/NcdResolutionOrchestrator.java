package com.titanium.product.application.orchestration.pricing.ncd;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

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
import com.titanium.product.valueobject.pricing.ncd.NcdLookbackWindow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NCD（无赔款优待）反算编排器（G11/g11-03）
 * <p>
 * 编排「取规则 → 定区间 → 取赔案 → 反算档位 → 组装取证」五步，业务判定全部内聚在
 * {@link NcdRuleSet} 聚合（档位命中/冲突）与值对象（区间口径）里，本类不复制任何判定：
 * </p>
 * <ol>
 *   <li><b>校验标的标识</b>：VIN 与标的ID 至少其一，皆空即拒（{@code NCD_SUBJECT_IDENTIFIER_REQUIRED}）；</li>
 *   <li>按业务时点取<b>已发布</b>规则集，取不到即拒（{@code NCD_RULE_NOT_CONFIGURED}）；</li>
 *   <li>由规则集派生统计区间（{@link NcdRuleSet#lookbackWindow(LocalDate)}）；</li>
 *   <li>经 {@link ClaimHistoryPort} 取区间内历史赔案——<b>取不到即拒</b>（{@code NCD_HISTORY_UNAVAILABLE}），
 *       「确无赔款」返回空列表并**正常参与反算**（命中 0 次档位）；</li>
 *   <li>以赔案条数与车型反算唯一命中档位（{@code NCD_BAND_NOT_MATCHED} / {@code NCD_RULE_CONFLICT}），
 *       连同取证字段组装结果。</li>
 * </ol>
 * <p>
 * 🔴 <b>全链失败关闭，绝不回落默认系数 1.0</b>：上述任一环节取不到/命中不了都在此显式拒绝。
 * 若在任一处「没有依据就先按 1.0 算」，「查不到历史」与「确无赔款」将不可区分——前者会静默少收
 * 保费、事后无从察觉，后者则凭空给了不该给的优待。
 * </p>
 * <p>
 * 六边形与分层定位：编排器属 application（§3.4.8），只注入 domain 的仓储端口与驱动端口，
 * 不实现任何 Adapter、不写业务规则。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NcdResolutionOrchestrator {

    private final NcdRuleSetRepository ncdRuleSetRepository;
    private final ClaimHistoryPort claimHistoryPort;

    public NcdResolutionResult resolve(ResolveNcdCommand command) {
        validateCommand(command);
        NcdRuleSet ruleSet = ncdRuleSetRepository
                .findPublished(command.tenantId(), command.productId(), command.businessTime())
                .orElseThrow(() -> new PricingDomainException(ProductErrorCode.NCD_RULE_NOT_CONFIGURED,
                        "产品 " + command.productId() + " 在业务时点 " + command.businessTime()
                                + " 无已发布 NCD 规则集，缺规则即拒绝反算，不得按无优待（系数 1.0）兜底"));

        NcdLookbackWindow window = ruleSet.lookbackWindow(command.businessTime().toLocalDate());
        List<ClaimHistoryItem> incidents = claimHistoryPort.findIncidents(
                new ClaimHistoryQuery(command.vin(), command.subjectId(), window), command.tenantId());
        NcdRule matched = ruleSet.resolve(incidents.size(), command.vehicleType());

        log.info("NCD 反算完成: tenantId={}, productId={}, vin={}, subjectId={}, 赔案条数={}, 区间=[{}, {}), "
                        + "规则集={}/{}, 车型={}, 系数={}",
                command.tenantId(), command.productId(), command.vin(), command.subjectId(), incidents.size(),
                window.from(), window.to(), ruleSet.getRuleSetCode(), ruleSet.getRuleSetVersion(),
                command.vehicleType(), matched.coefficient());
        return new NcdResolutionResult(ruleSet.getProductId(), matched.coefficient(), incidents.size(),
                window.from(), window.to(), ruleSet.getRuleSetCode(), ruleSet.getRuleSetVersion(),
                ruleSet.getContentHash());
    }

    /**
     * 入参前置校验（>3 项按红线 22 内聚于此，不散落在主流程）。
     * <p>
     * 🔴 标的标识缺失（VIN 与标的ID 皆空）是**调用方错误**，不得按「无赔款」处理——
     * 按无赔款处理等于在没有车辆身份的情况下凭空给出优待。
     * </p>
     */
    private void validateCommand(ResolveNcdCommand command) {
        Objects.requireNonNull(command, "反算命令不能为空");
        requireText(command.tenantId(), "租户ID");
        requireText(command.productId(), "产品ID");
        if (command.businessTime() == null) {
            throw new PricingDomainException(ProductErrorCode.PRICING_INPUT_INVALID, "业务时点不能为空");
        }
        boolean hasVin = hasText(command.vin());
        boolean hasSubjectId = hasText(command.subjectId());
        if (!hasVin && !hasSubjectId) {
            throw new PricingDomainException(ProductErrorCode.NCD_SUBJECT_IDENTIFIER_REQUIRED,
                    "反算 NCD 须提供 VIN 或标的ID 之一（VIN 优先），缺键不得按「无赔款」处理");
        }
    }

    private void requireText(String value, String field) {
        if (!hasText(value)) {
            throw new PricingDomainException(ProductErrorCode.PRICING_INPUT_INVALID, field + "不能为空");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
