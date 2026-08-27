package com.titanium.product.port;

import com.titanium.product.valueobject.pricing.CommissionResolutionRequest;
import com.titanium.product.valueobject.pricing.CommissionResolutionResult;
import com.titanium.product.valueobject.pricing.CommissionSchemeValidationRequest;

/**
 * Product 访问 Channel 版本化佣金方案的外部端口。
 */
public interface CommissionResolutionPort {

    void validate(CommissionSchemeValidationRequest request);

    CommissionResolutionResult calculate(CommissionResolutionRequest request);
}
