package com.titanium.product.query.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.view.ProductView;
import com.titanium.product.valueobject.ActuarialBasis;
import com.titanium.product.valueobject.CoveragePeriodConfig;
import com.titanium.product.valueobject.DocumentConfig;
import com.titanium.product.valueobject.InsureCondition;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.PaymentConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.UnderwritingConfig;

/**
 * 产品读模型 → 查询结果声明式映射（MapStruct）。
 * <p>
 * 标量字段同名自动映射；JSON 配置列经 {@code parseXxx} 空安全转换方法反序列化为领域值对象；
 * 定价模式列存 code、经 {@link PricingMode#fromCode} 还原；min/maxPremium 从定价规则 JSON 快照提取。
 * </p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductQueryResultMapper {

    /** 读模型 → 查询结果（差异字段见下，其余同名字段自动映射）。 */
    @Mapping(target = "version", source = "versionNo")
    @Mapping(target = "insureCondition", source = "insureConditionJson", qualifiedByName = "parseInsureCondition")
    @Mapping(target = "coveragePeriod", source = "coveragePeriodJson", qualifiedByName = "parseCoveragePeriod")
    @Mapping(target = "paymentConfig", source = "paymentConfigJson", qualifiedByName = "parsePaymentConfig")
    @Mapping(target = "pricingBasicRule", source = "pricingBasicRuleJson", qualifiedByName = "parsePricingBasicRule")
    @Mapping(target = "minPremium", source = "pricingBasicRuleJson", qualifiedByName = "minPremium")
    @Mapping(target = "maxPremium", source = "pricingBasicRuleJson", qualifiedByName = "maxPremium")
    @Mapping(target = "issuanceProcessConfig", source = "issuanceProcessConfigJson",
            qualifiedByName = "parseIssuanceProcessConfig")
    @Mapping(target = "policyFormConfig", source = "policyFormConfigJson", qualifiedByName = "parsePolicyFormConfig")
    @Mapping(target = "underwritingConfig", source = "underwritingConfigJson", qualifiedByName = "parseUnderwritingConfig")
    @Mapping(target = "documentConfig", source = "documentConfigJson", qualifiedByName = "parseDocumentConfig")
    @Mapping(target = "pricingMode", source = "pricingMode", qualifiedByName = "pricingModeFromCode")
    @Mapping(target = "rateTableRef", source = "rateTableRefJson", qualifiedByName = "parseRateTableRef")
    @Mapping(target = "actuarialBasis", source = "actuarialBasisJson", qualifiedByName = "parseActuarialBasis")
    ProductQueryResult toQueryResult(ProductView view);

    /** JSON 字符串 → 投保条件值对象（null 安全）。 */
    @Named("parseInsureCondition")
    default InsureCondition parseInsureCondition(String json) {
        return parse(json, InsureCondition.class);
    }

    /** JSON 字符串 → 保障期间配置值对象（null 安全）。 */
    @Named("parseCoveragePeriod")
    default CoveragePeriodConfig parseCoveragePeriod(String json) {
        return parse(json, CoveragePeriodConfig.class);
    }

    /** JSON 字符串 → 缴费方式配置值对象（null 安全）。 */
    @Named("parsePaymentConfig")
    default PaymentConfig parsePaymentConfig(String json) {
        return parse(json, PaymentConfig.class);
    }

    /** JSON 字符串 → 定价基础规则值对象（null 安全）。 */
    @Named("parsePricingBasicRule")
    default PricingBasicRule parsePricingBasicRule(String json) {
        return parse(json, PricingBasicRule.class);
    }

    /** 定价规则 JSON 快照 → 最低保费（null 安全）。 */
    @Named("minPremium")
    default Double minPremium(String json) {
        return json != null ? JSON.parseObject(json).getDouble("minPremium") : null;
    }

    /** 定价规则 JSON 快照 → 最高保费（null 安全）。 */
    @Named("maxPremium")
    default Double maxPremium(String json) {
        return json != null ? JSON.parseObject(json).getDouble("maxPremium") : null;
    }

    /** JSON 字符串 → 出单流程配置值对象（null 安全）。 */
    @Named("parseIssuanceProcessConfig")
    default IssuanceProcessConfig parseIssuanceProcessConfig(String json) {
        return parse(json, IssuanceProcessConfig.class);
    }

    /** JSON 字符串 → 保单形态配置值对象（null 安全）。 */
    @Named("parsePolicyFormConfig")
    default PolicyFormConfig parsePolicyFormConfig(String json) {
        return parse(json, PolicyFormConfig.class);
    }

    /** JSON 字符串 → 核保配置值对象（null 安全）。 */
    @Named("parseUnderwritingConfig")
    default UnderwritingConfig parseUnderwritingConfig(String json) {
        return parse(json, UnderwritingConfig.class);
    }

    /** JSON 字符串 → 文档配置值对象（null 安全）。 */
    @Named("parseDocumentConfig")
    default DocumentConfig parseDocumentConfig(String json) {
        return parse(json, DocumentConfig.class);
    }

    /** 定价模式 code 字符串 → 定价模式枚举（null 安全）。 */
    @Named("pricingModeFromCode")
    default PricingMode pricingModeFromCode(String code) {
        return code != null ? PricingMode.fromCode(code) : null;
    }

    /** JSON 字符串 → 费率表引用值对象（null 安全）。 */
    @Named("parseRateTableRef")
    default RateTableRef parseRateTableRef(String json) {
        return parse(json, RateTableRef.class);
    }

    /** JSON 字符串 → 精算基础参数值对象（null 安全）。 */
    @Named("parseActuarialBasis")
    default ActuarialBasis parseActuarialBasis(String json) {
        return parse(json, ActuarialBasis.class);
    }

    /** JSON 字符串 → 值对象（null 安全，与查询服务原解析语义一致）。 */
    default <T> T parse(String json, Class<T> type) {
        return json != null ? JSON.parseObject(json, type) : null;
    }
}
