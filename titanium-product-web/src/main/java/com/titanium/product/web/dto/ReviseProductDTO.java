package com.titanium.product.web.dto;

import java.util.List;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.config.CoveragePeriodConfig;
import com.titanium.product.valueobject.config.IssuanceProcessConfig;
import com.titanium.product.valueobject.config.PaymentConfig;
import com.titanium.product.valueobject.config.PolicyFormConfig;
import com.titanium.product.valueobject.config.SalesChannelConfig;
import com.titanium.product.valueobject.config.UnderwritingConfig;
import com.titanium.product.valueobject.pricing.pricing.ActuarialBasis;
import com.titanium.product.valueobject.rate.RateTableRef;

import lombok.Data;

/**
 * 修订产品请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上，承载修订后新版本的完整产品配置，由 {@code ProductWebMapper} 翻译为
 * 领域命令 {@code ReviseProductCommand}。仅 EFFECTIVE 产品可修订：修订不改写当前生效版本，
 * 而以新 {@code newProductId} 创建新版本 DRAFT 聚合（版本号由聚合根递增，如 V1.0 → V2.0）。
 * 存量产品补配核保配置等场景走本链路。web 层可依赖 domain 命令与 metadata 枚举。
 * </p>
 */
@Data
public class ReviseProductDTO {

    /** 新版本产品ID（可选，不传则自动生成） */
    private String newProductId;
    /** 新版本产品名称 */
    private String newProductName;
    /** 新版本产品描述 */
    private String newProductDesc;
    /** 新版本产品形态（INDIVIDUAL/GROUP） */
    private ProductEnum.ProductForm newForm;
    /** 新版本险种类型 */
    private InsuranceProductType newInsuranceType;
    /** 新版本产品类别（MAIN/RIDER） */
    private ProductEnum.ProductCategory newCategory;

    /** 新版本投保条件 */
    private InsureConditionDTO newInsureCondition;
    /** 新版本保障期间配置 */
    private CoveragePeriodConfig newCoveragePeriod;
    /** 新版本缴费方式配置 */
    private PaymentConfig newPaymentConfig;
    /** 新版本条款关联（由前端带出现有绑定关系，修订时可调整） */
    private List<ClauseRelDTO> newClauseRels;
    /** 新版本定价基础规则 */
    private PricingBasicRuleDTO newPricingBasicRule;

    /** 新版本销售渠道配置 */
    private List<SalesChannelConfig> newSalesChannels;
    /** 新版本出单流程配置 */
    private IssuanceProcessConfig newIssuanceProcessConfig;
    /** 新版本保单形态配置 */
    private PolicyFormConfig newPolicyFormConfig;
    /** 新版本核保配置（核保模式/自动核保条件/转人工阈值/必需材料/时效/加费/特别约定） */
    private UnderwritingConfig newUnderwritingConfig;

    /** 新版本定价模式编码（RATE_TABLE / ACTUARIAL_FORMULA） */
    private String newPricingMode;
    /** 新版本费率表引用 */
    private RateTableRef newRateTableRef;
    /** 新版本精算基础参数 */
    private ActuarialBasis newActuarialBasis;

    /**
     * 条款关联入参（修订请求内嵌）
     * <p>
     * 只承载条款ID/版本/是否主条款三元组，由映射器经 {@code ProductClauseRel} 便捷构造器
     * 派生相对标识与绑定时间，避免调用方手工拼装聚合内标识。
     * </p>
     *
     * @param clauseId 关联的条款ID
     * @param clauseVersion 条款版本
     * @param isMainClause 是否为主条款
     */
    public record ClauseRelDTO(String clauseId, String clauseVersion, Boolean isMainClause) {
    }
}
