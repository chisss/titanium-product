package com.titanium.product.port.pricing;

import com.titanium.product.valueobject.pricing.pricing.PricingFeatureResolution;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureResolutionRequest;

/**
 * Product 读取定价特征的驱动端口。
 */
public interface FeatureResolutionPort {

    PricingFeatureResolution resolve(PricingFeatureResolutionRequest request);
}
