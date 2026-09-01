package com.titanium.product.port.pricing;

import com.titanium.product.valueobject.pricing.commission.CommissionResolutionRequest;
import com.titanium.product.valueobject.pricing.commission.CommissionResolutionResult;
import com.titanium.product.valueobject.pricing.commission.CommissionSchemeValidationRequest;

/**
 * Product 访问 Channel 版本化佣金方案的外部端口。
 */
public interface CommissionResolutionPort {

    void validate(CommissionSchemeValidationRequest request);

    CommissionResolutionResult calculate(CommissionResolutionRequest request);
}
