package com.titanium.product.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.ActuarialBasis;
import com.titanium.product.valueobject.CoveragePeriodConfig;
import com.titanium.product.valueobject.DocumentConfig;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.PaymentConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.SalesChannelConfig;
import com.titanium.product.valueobject.UnderwritingConfig;

import lombok.Data;

/**
 * 创建产品请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上，承载完整产品配置。由 {@code ProductWebMapper} 翻译为领域命令
 * {@code CreateProductCommand}。web 层可依赖 domain 命令与 metadata 枚举。
 * </p>
 */
@Data
public class CreateProductDTO {

    /** 产品ID（可选，不传则自动生成） */
    private String productId;
    /** 所属产品模板ID（必填，单向引用 ProductTemplate） */
    private String templateId;
    /** 产品代码 */
    private String productCode;
    /** 产品名称 */
    private String productName;
    /** 产品描述 */
    private String productDesc;
    /** 产品形态（INDIVIDUAL/GROUP） */
    private ProductEnum.ProductForm form;
    /** 险种类型 */
    private InsuranceProductType insuranceType;
    /** 产品类别（MAIN/RIDER） */
    private ProductEnum.ProductCategory category;
    /** 销售开始时间 */
    private LocalDateTime saleStartTime;
    /** 销售截止时间 */
    private LocalDateTime saleEndTime;

    /** 投保条件 */
    private InsureConditionDTO insureCondition;
    /** 保障期间配置（保障期间类型/固定期限选项/期间单位/最小最大期限） */
    private CoveragePeriodConfig coveragePeriod;
    /** 缴费方式配置（允许缴费频率/缴费期限选项/期限单位/自动续保/宽限期） */
    private PaymentConfig paymentConfig;
    /** 定价基础规则 */
    private PricingBasicRuleDTO pricingBasicRule;

    /** 绑定的条款ID列表 */
    private List<String> clauseIds;
    /** 条款版本映射 */
    private Map<String, String> clauseVersionMap;
    /** 主条款ID */
    private String mainClauseId;

    /** 销售渠道配置 */
    private List<SalesChannelConfig> salesChannels;
    /** 附加险产品ID列表 */
    private List<String> attachProductIds;

    /** 出单流程配置（出单模式/步骤链/是否需意向单/是否可跳核保/前置缴费/出单时效/附加步骤） */
    private IssuanceProcessConfig issuanceProcessConfig;
    /** 保单形态配置（保单形态类型/清单制/子保单数量/层级/受益人要求） */
    private PolicyFormConfig policyFormConfig;
    /** 核保配置（核保模式/自动核保条件/转人工阈值/必需材料/时效/加费/特别约定） */
    private UnderwritingConfig underwritingConfig;
    /** 文档配置（所需投保材料清单 + 生成文档模板清单，纯产品配置不跨文档域） */
    private DocumentConfig documentConfig;

    /** 定价模式编码（RATE_TABLE 费率表查询 / ACTUARIAL_FORMULA 精算公式），billing 出单按此分派保费计算路径 */
    private String pricingMode;
    /** 费率表引用（pricingMode=RATE_TABLE 时有值，指向 clause 域费率表） */
    private RateTableRef rateTableRef;
    /** 精算基础参数（pricingMode=ACTUARIAL_FORMULA 时有值，预定利率/死亡率表/附加费用率） */
    private ActuarialBasis actuarialBasis;

    /** 创建人 */
    private String createdBy;
}
