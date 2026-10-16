package com.titanium.product.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.metadata.enums.insurance.InsuranceMandateType;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.config.ClauseBinding;
import com.titanium.product.valueobject.config.CoveragePeriodConfig;
import com.titanium.product.valueobject.config.DocumentConfig;
import com.titanium.product.valueobject.config.InsureCondition;
import com.titanium.product.valueobject.config.IssuanceProcessConfig;
import com.titanium.product.valueobject.config.PaymentConfig;
import com.titanium.product.valueobject.config.PolicyFormConfig;
import com.titanium.product.valueobject.config.PolicyStructureConfig;
import com.titanium.product.valueobject.config.SalesChannelConfig;
import com.titanium.product.valueobject.config.SalesPeriod;
import com.titanium.product.valueobject.config.UnderwritingConfig;
import com.titanium.product.valueobject.pricing.pricing.ActuarialBasis;
import com.titanium.product.valueobject.pricing.pricing.PricingBasicRule;
import com.titanium.product.valueobject.rate.RateTableRef;

/**
 * 创建产品命令 用于创建新的保险产品，包含完整的产品配置信息
 *
 * <p>DSN-04/m25-06 拆分：原 31 个平铺组件收敛为 28 个——售期起止收敛为 {@link SalesPeriod}、
 * 条款绑定三件套收敛为 {@link ClauseBinding}（红线 17 第 1 条：按内聚度拆分为嵌套值对象）。
 * 命令为瞬态入参（不落事件存储、不参与回放），存量事件 {@code ProductCreatedEvent} 的载荷表示
 * （{@code saleStartTime}/{@code saleEndTime} 平铺 + {@code List<ProductClauseRel> clauseRels}）
 * 独立于命令，此次拆分**零 upcaster 需求**。</p>
 */
public record CreateProductCommand(@TargetAggregateIdentifier String productId, String templateId, String productCode,
                                   String productName, String productDesc, ProductEnum.ProductForm form,
                                   InsuranceProductType insuranceType, ProductEnum.ProductCategory category,
                                   LocalDateTime effectiveTime, SalesPeriod salesPeriod,
                                   InsureCondition insureCondition, CoveragePeriodConfig coveragePeriod,
                                   PaymentConfig paymentConfig, PricingBasicRule pricingBasicRule,
                                   ClauseBinding clauseBinding,
                                   List<SalesChannelConfig> salesChannels, List<String> attachProductIds,
                                   IssuanceProcessConfig issuanceProcessConfig, PolicyFormConfig policyFormConfig,
                                   UnderwritingConfig underwritingConfig, String tenantId,
                                   PricingMode pricingMode, RateTableRef rateTableRef,
                                   ActuarialBasis actuarialBasis, DocumentConfig documentConfig, String createdBy,
                                   PolicyStructureConfig policyStructureConfig,
                                   InsuranceMandateType insuranceMandateType) {

    /**
     * null 归一：售期与条款绑定为 null 时视作「空值块」，使聚合装配与各校验点免于分散判空。
     * <p>
     * 条款为空（含拆分前的 null 语义）仍由聚合 {@code validateCreateCommand} 的
     * 「产品必须绑定至少一条条款」拦截——拒绝路径与拆分前等价（拆分前 clauseIds=null 亦被同一
     * 断言拒绝，触碰不到后续 {@code .stream()}）。</p>
     */
    public CreateProductCommand {
        if (salesPeriod == null) {
            salesPeriod = new SalesPeriod(null, null);
        }
        if (clauseBinding == null) {
            clauseBinding = new ClauseBinding(List.of(), Map.of(), null);
        }
    }
}
