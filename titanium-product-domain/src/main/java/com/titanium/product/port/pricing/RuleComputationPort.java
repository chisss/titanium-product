package com.titanium.product.port.pricing;

import com.titanium.product.valueobject.pricing.pricing.PricingRuleComputationRequest;
import com.titanium.product.valueobject.pricing.pricing.PricingRuleComputationResult;

/**
 * Product 执行版本化定价规则工件的驱动端口。
 */
public interface RuleComputationPort {

    PricingRuleComputationResult compute(PricingRuleComputationRequest request);
}
