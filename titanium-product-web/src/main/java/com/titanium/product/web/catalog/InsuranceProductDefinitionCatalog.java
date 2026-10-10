package com.titanium.product.web.catalog;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.titanium.metadata.enums.insurance.InsuranceTypeCapability;
import com.titanium.product.api.response.product.InsuranceProductDefinitionResponse;

/**
 * 九类险种标准产品定义目录（<b>权威表的投影</b>，G10/g10-05）。
 * <p>
 * 本类原持有九类险种清单的硬编码；该数据已上移为跨域共享的单一权威表
 * {@link InsuranceTypeCapability}（titanium-metadata），本类降级为「权威表 → 契约响应」
 * 的投影：契约形状、排序与取值均未变动，故 admin BFF 与前端零改动。
 * </p>
 * <p>目录是只读默认值，租户产品仍通过 Product 聚合持久化自己的版本和规则。</p>
 *
 * @see InsuranceTypeCapability 单一权威表（含能力位的消费方登记）
 */
@Component
public class InsuranceProductDefinitionCatalog {

    private final List<InsuranceProductDefinitionResponse> definitions = Arrays.stream(InsuranceTypeCapability.values())
            .map(InsuranceProductDefinitionCatalog::toResponse)
            .toList();

    public List<InsuranceProductDefinitionResponse> list() {
        return definitions;
    }

    private static InsuranceProductDefinitionResponse toResponse(InsuranceTypeCapability capability) {
        return new InsuranceProductDefinitionResponse(capability.getInsuranceType(), capability.getSubjectType(),
                capability.getDefaultIssuanceMode(), capability.getDefaultPricingMode(),
                capability.getRequiredSubjectFields(), capability.getDefaultCoverageCodes(),
                capability.getUnderwritingRuleSet());
    }
}
