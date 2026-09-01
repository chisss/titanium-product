package com.titanium.product.api.response.product;

import java.util.List;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.insurance.SubjectType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准险种产品定义目录项。
 * <p>目录只描述可配置产品的默认骨架，具体产品仍需通过产品聚合创建并版本化。</p>
 */
@Schema(description = "标准险种产品定义")
public record InsuranceProductDefinitionResponse(
        InsuranceProductType insuranceType,
        SubjectType subjectType,
        ProductEnum.IssuanceMode defaultIssuanceMode,
        PricingMode defaultPricingMode,
        List<String> requiredSubjectFields,
        List<String> defaultCoverageCodes,
        String underwritingRuleSet) {
}
