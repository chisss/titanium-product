package com.titanium.product.port;

import com.titanium.product.valueobject.pricing.PricingRuleComputationRequest;
import com.titanium.product.valueobject.pricing.PricingRuleComputationResult;

/**
 * Product 执行版本化定价规则工件的驱动端口。
 */
public interface RuleComputationPort {

    PricingRuleComputationResult compute(PricingRuleComputationRequest request);
}
