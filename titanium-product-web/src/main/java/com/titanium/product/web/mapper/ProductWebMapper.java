package com.titanium.product.web.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.api.dto.ProductDTO;
import com.titanium.product.api.request.CreateProductRequest;
import com.titanium.product.api.request.InsureConditionRequest;
import com.titanium.product.api.request.PricingBasicRuleRequest;
import com.titanium.product.domain.command.CreateProductCommand;
import com.titanium.product.domain.valueobject.InsureCondition;
import com.titanium.product.domain.valueobject.PricingBasicRule;
import com.titanium.product.query.entity.ProductQueryResult;

/**
 * 产品Web层Mapper
 * 用于Web层和应用层之间的数据转换
 */
@Mapper(componentModel = "spring")
public interface ProductWebMapper {

    ProductWebMapper INSTANCE = Mappers.getMapper(ProductWebMapper.class);

    /**
     * 将CreateProductRequest转换为CreateProductCommand
     */
    default CreateProductCommand toCreateProductCommand(CreateProductRequest request, String tenantId) {
        return new CreateProductCommand(
                request.getProductId() != null ? request.getProductId() : UUID.randomUUID().toString(),
                request.getProductCode(),
                request.getProductName(),
                request.getProductDesc(),
                request.getForm() != null ? ProductEnum.ProductForm.valueOf(request.getForm()) : null,
                request.getInsuranceType() != null ? InsuranceType.valueOf(request.getInsuranceType()) : null,
                request.getCategory() != null ? ProductEnum.ProductCategory.valueOf(request.getCategory()) : null,
                null, // effectiveTime
                request.getSaleStartTime(),
                request.getSaleEndTime(),
                toInsureCondition(request.getInsureCondition()),
                null, // coveragePeriod - 后续通过JSON转换
                null, // paymentConfig - 后续通过JSON转换
                request.getPricingBasicRule() != null ? toPricingBasicRule(request.getPricingBasicRule()) : null,
                request.getClauseIds(),
                request.getClauseVersionMap(),
                request.getMainClauseId(),
                null, // salesChannels - 后续通过JSON转换
                request.getAttachProductIds(),
                null, // issuanceProcessConfig - 后续通过JSON转换
                null, // policyFormConfig - 后续通过JSON转换
                null, // underwritingConfig - 后续通过JSON转换
                tenantId
        );
    }

    /**
     * 将ProductQueryResult转换为ProductDTO
     */
    default ProductDTO toProductDTO(ProductQueryResult result) {
        if (result == null) return null;
        ProductDTO dto = new ProductDTO();
        dto.setProductId(result.getProductId());
        dto.setProductCode(result.getProductCode());
        dto.setProductName(result.getProductName());
        dto.setProductDesc(result.getProductDesc());
        dto.setForm(result.getForm());
        dto.setInsuranceType(result.getInsuranceType());
        dto.setCategory(result.getCategory());
        dto.setVersion(result.getVersion());
        dto.setStatus(result.getStatus());
        dto.setOriginalProductId(result.getOriginalProductId());
        dto.setEffectiveTime(result.getEffectiveTime());
        dto.setInvalidTime(result.getInvalidTime());
        dto.setSaleStartTime(result.getSaleStartTime());
        dto.setSaleEndTime(result.getSaleEndTime());
        dto.setInsureCondition(result.getInsureCondition());
        dto.setCoveragePeriod(result.getCoveragePeriod());
        dto.setPaymentConfig(result.getPaymentConfig());
        dto.setPricingBasicRule(result.getPricingBasicRule());
        dto.setIssuanceProcessConfig(result.getIssuanceProcessConfig());
        dto.setPolicyFormConfig(result.getPolicyFormConfig());
        dto.setUnderwritingConfig(result.getUnderwritingConfig());
        dto.setAuditInfo(result.getAuditInfo());
        dto.setCreatedAt(result.getCreatedAt());
        dto.setCreatedBy(result.getCreatedBy());
        dto.setUpdatedAt(result.getUpdatedAt());
        dto.setUpdatedBy(result.getUpdatedBy());
        return dto;
    }

    @Named("toInsureCondition")
    default InsureCondition toInsureCondition(InsureConditionRequest req) {
        if (req == null) return null;
        return new InsureCondition(
                req.getMinAge(), req.getMaxAge(),
                req.getForbiddenOccupations(), null,
                req.getMinGroupSize(), req.getMaxGroupSize(),
                req.getHealthNotice(),
                null, null, null, null, null, null, null
        );
    }

    @Named("toPricingBasicRule")
    default PricingBasicRule toPricingBasicRule(PricingBasicRuleRequest req) {
        if (req == null) return null;
        return new PricingBasicRule(
                req.getPricingType(), req.getBaseRate(),
                req.getFactors(), req.getRateFormula(),
                req.getTypeSpecificConfig()
        );
    }
}
