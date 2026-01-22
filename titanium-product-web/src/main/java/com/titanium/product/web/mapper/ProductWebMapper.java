package com.titanium.product.web.mapper;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.titanium.product.api.dto.ProductDTO;
import com.titanium.product.api.request.CreateProductRequest;
import com.titanium.product.api.request.InsureConditionRequest;
import com.titanium.product.api.request.PricingBasicRuleRequest;
import com.titanium.product.domain.command.CreateProductCommand;
import com.titanium.product.domain.valueobject.InsureCondition;
import com.titanium.product.domain.valueobject.PricingBasicRule;
import com.titanium.product.query.entity.ProductQueryResult;

/**
 * 产品Web层Mapper 用于Web层和应用层之间的数据转换
 */
@Mapper(componentModel = "spring")
public interface ProductWebMapper {

    ProductWebMapper INSTANCE = Mappers.getMapper(ProductWebMapper.class);

    /**
     * 将CreateProductRequest转换为CreateProductCommand
     * 
     * @param request 创建产品请求
     * @return 创建产品命令
     */
    @Mapping(source = "form", target = "form", qualifiedByName = "toProductForm")
    @Mapping(source = "insuranceType", target = "insuranceType", qualifiedByName = "toInsuranceType")
    @Mapping(source = "insureCondition", target = "insureCondition", qualifiedByName = "toInsureCondition")
    @Mapping(source = "pricingBasicRule", target = "pricingBasicRule", qualifiedByName = "toPricingBasicRule")
    CreateProductCommand toCreateProductCommand(CreateProductRequest request);

    /**
     * 将ProductQueryResult转换为ProductDTO
     * 
     * @param productQueryResult 产品查询结果
     * @return 产品DTO
     */
    ProductDTO toProductDTO(ProductQueryResult productQueryResult);

    /**
     * 将字符串转换为ProductForm枚举
     * 
     * @param form 产品形态字符串
     * @return ProductForm枚举
     */
    @Named("toProductForm")
    default ProductEnum.ProductForm toProductForm(String form) {
        return ProductEnum.ProductForm.valueOf(form);
    }

    /**
     * 将字符串转换为InsuranceType枚举
     * 
     * @param insuranceType 险种类型字符串
     * @return InsuranceType枚举
     */
    @Named("toInsuranceType")
    default InsuranceType toInsuranceType(String insuranceType) {
        return InsuranceType.valueOf(insuranceType);
    }

    /**
     * 将请求中的投保条件转换为值对象
     * 
     * @param insureCondition 投保条件请求
     * @return InsureCondition值对象
     */
    @Named("toInsureCondition")
    default InsureCondition toInsureCondition(InsureConditionRequest insureCondition) {
        // 这里简化处理，实际应该进行完整的转换
        return new InsureCondition( insureCondition.getMinAge(), insureCondition.getMaxAge(),
                insureCondition.getForbiddenOccupations(), insureCondition.getMinGroupSize(),
                insureCondition.getMaxGroupSize(), insureCondition.getHealthNotice());
    }

    /**
     * 将请求中的定价基础规则转换为值对象
     * 
     * @param pricingBasicRule 定价基础规则请求
     * @return PricingBasicRule值对象
     */
    @Named("toPricingBasicRule")
    default PricingBasicRule toPricingBasicRule(PricingBasicRuleRequest pricingBasicRule) {
        // 这里简化处理，实际应该进行完整的转换
        return new PricingBasicRule(pricingBasicRule.getPricingType(), pricingBasicRule.getBaseRate(),
                pricingBasicRule.getFactors(), pricingBasicRule.getRateFormula(),
                pricingBasicRule.getTypeSpecificConfig());
    }
}
