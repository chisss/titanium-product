package com.titanium.product.infrastructure.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.infrastructure.entity.ProductClauseRelEntity;
import com.titanium.product.valueobject.CoveragePeriodConfig;
import com.titanium.product.valueobject.InsureCondition;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.PaymentConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.valueobject.SalesChannelConfig;
import com.titanium.product.valueobject.UnderwritingConfig;

/**
 * 产品基础设施层映射器
 * 用于基础设施层和领域层之间的数据转换，包含JSON↔值对象转换
 */
@Mapper
public interface ProductInfraMapper {

    ProductInfraMapper INSTANCE = Mappers.getMapper(ProductInfraMapper.class);

    // ==================== 枚举↔String转换 ====================

    @Named("stringToProductForm")
    default ProductEnum.ProductForm stringToProductForm(String form) {
        return form != null ? ProductEnum.ProductForm.valueOf(form) : null;
    }

    @Named("productFormToString")
    default String productFormToString(ProductEnum.ProductForm form) {
        return form != null ? form.name() : null;
    }

    @Named("stringToInsuranceType")
    default InsuranceType stringToInsuranceType(String insuranceType) {
        return insuranceType != null ? InsuranceType.valueOf(insuranceType) : null;
    }

    @Named("insuranceTypeToString")
    default String insuranceTypeToString(InsuranceType insuranceType) {
        return insuranceType != null ? insuranceType.name() : null;
    }

    @Named("stringToProductStatus")
    default ProductEnum.ProductStatus stringToProductStatus(String status) {
        return status != null ? ProductEnum.ProductStatus.valueOf(status) : null;
    }

    @Named("productStatusToString")
    default String productStatusToString(ProductEnum.ProductStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("stringToProductCategory")
    default ProductEnum.ProductCategory stringToProductCategory(String category) {
        return category != null ? ProductEnum.ProductCategory.valueOf(category) : null;
    }

    @Named("productCategoryToString")
    default String productCategoryToString(ProductEnum.ProductCategory category) {
        return category != null ? category.name() : null;
    }

    // ==================== JSON ↔ 值对象转换 ====================

    @Named("jsonToInsureCondition")
    default InsureCondition jsonToInsureCondition(String json) {
        return json != null ? JSON.parseObject(json, InsureCondition.class) : null;
    }

    @Named("insureConditionToJson")
    default String insureConditionToJson(InsureCondition val) {
        return val != null ? JSON.toJSONString(val) : null;
    }

    @Named("jsonToCoveragePeriodConfig")
    default CoveragePeriodConfig jsonToCoveragePeriodConfig(String json) {
        return json != null ? JSON.parseObject(json, CoveragePeriodConfig.class) : null;
    }

    @Named("coveragePeriodConfigToJson")
    default String coveragePeriodConfigToJson(CoveragePeriodConfig val) {
        return val != null ? JSON.toJSONString(val) : null;
    }

    @Named("jsonToPaymentConfig")
    default PaymentConfig jsonToPaymentConfig(String json) {
        return json != null ? JSON.parseObject(json, PaymentConfig.class) : null;
    }

    @Named("paymentConfigToJson")
    default String paymentConfigToJson(PaymentConfig val) {
        return val != null ? JSON.toJSONString(val) : null;
    }

    @Named("jsonToPricingBasicRule")
    default PricingBasicRule jsonToPricingBasicRule(String json) {
        return json != null ? JSON.parseObject(json, PricingBasicRule.class) : null;
    }

    @Named("pricingBasicRuleToJson")
    default String pricingBasicRuleToJson(PricingBasicRule val) {
        return val != null ? JSON.toJSONString(val) : null;
    }

    @Named("jsonToIssuanceProcessConfig")
    default IssuanceProcessConfig jsonToIssuanceProcessConfig(String json) {
        return json != null ? JSON.parseObject(json, IssuanceProcessConfig.class) : null;
    }

    @Named("issuanceProcessConfigToJson")
    default String issuanceProcessConfigToJson(IssuanceProcessConfig val) {
        return val != null ? JSON.toJSONString(val) : null;
    }

    @Named("jsonToPolicyFormConfig")
    default PolicyFormConfig jsonToPolicyFormConfig(String json) {
        return json != null ? JSON.parseObject(json, PolicyFormConfig.class) : null;
    }

    @Named("policyFormConfigToJson")
    default String policyFormConfigToJson(PolicyFormConfig val) {
        return val != null ? JSON.toJSONString(val) : null;
    }

    @Named("jsonToUnderwritingConfig")
    default UnderwritingConfig jsonToUnderwritingConfig(String json) {
        return json != null ? JSON.parseObject(json, UnderwritingConfig.class) : null;
    }

    @Named("underwritingConfigToJson")
    default String underwritingConfigToJson(UnderwritingConfig val) {
        return val != null ? JSON.toJSONString(val) : null;
    }

    @Named("jsonToSalesChannels")
    default List<SalesChannelConfig> jsonToSalesChannels(String json) {
        return json != null ? JSON.parseObject(json, new TypeReference<List<SalesChannelConfig>>() {}) : null;
    }

    @Named("salesChannelsToJson")
    default String salesChannelsToJson(List<SalesChannelConfig> val) {
        return val != null ? JSON.toJSONString(val) : null;
    }

    @Named("jsonToAttachProductIds")
    default List<String> jsonToAttachProductIds(String json) {
        return json != null ? JSON.parseArray(json, String.class) : null;
    }

    @Named("attachProductIdsToJson")
    default String attachProductIdsToJson(List<String> val) {
        return val != null ? JSON.toJSONString(val) : null;
    }

    // ==================== 条款关联转换 ====================

    @Mapping(source = "clauseId", target = "clauseId")
    @Mapping(source = "clauseVersion", target = "clauseVersion")
    @Mapping(source = "isMainClause", target = "isMainClause")
    ProductClauseRel toProductClauseRel(ProductClauseRelEntity productClauseRelDO);

    @Mapping(source = "clauseId", target = "clauseId")
    @Mapping(source = "clauseVersion", target = "clauseVersion")
    @Mapping(source = "isMainClause", target = "isMainClause")
    ProductClauseRelEntity toProductClauseRelEntity(ProductClauseRel productClauseRel);

    List<ProductClauseRel> toProductClauseRels(List<ProductClauseRelEntity> productClauseRelDOs);

    List<ProductClauseRelEntity> toProductClauseRelEntitys(List<ProductClauseRel> productClauseRels);
}
