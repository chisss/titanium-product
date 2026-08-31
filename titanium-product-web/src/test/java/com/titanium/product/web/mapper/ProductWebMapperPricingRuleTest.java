package com.titanium.product.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.titanium.product.api.response.PricingBasicRuleResponse;
import com.titanium.product.query.result.ProductQueryResult;

/** 产品定价边界字段的读侧映射回归测试。 */
class ProductWebMapperPricingRuleTest {

    @Test
    void shouldExposePremiumBoundsFromQueryResult() {
        ProductQueryResult result = new ProductQueryResult();
        result.setMinPremium(100D);
        result.setMaxPremium(10000D);

        PricingBasicRuleResponse response = Mappers.getMapper(ProductWebMapper.class)
                .toPricingRuleResponse(result);

        assertThat(response.getMinPremium()).isEqualTo(100D);
        assertThat(response.getMaxPremium()).isEqualTo(10000D);
    }
}
