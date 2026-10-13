package com.titanium.product.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.titanium.product.api.response.pricing.PricingBasicRuleResponse;
import com.titanium.product.query.mapper.ProductQueryResultMapper;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.valueobject.pricing.pricing.PricingBasicRule;

/**
 * 产品定价边界字段的读侧映射回归测试。
 * <p>
 * 除最低/最高保费外，覆盖 G14/g14-04a 新增的 {@code managementFeeRate} 提取链路——
 * 它是 policy 出单时经 Feign 取管理费费率的<b>唯一通道</b>，链路上任何一环静默丢值都会
 * 让投资账户固化不到费率（此后每月计提静默不收费，事后无从对账）。
 * </p>
 */
class ProductWebMapperPricingRuleTest {

    /** 真机读路径形态：读模型 JSON 文本 → 值对象 → Response（经 fastjson2，小数默认 BigDecimal） */
    private PricingBasicRuleResponse toResponseViaJson(String pricingBasicRuleJson) {
        PricingBasicRule parsed = Mappers.getMapper(ProductQueryResultMapper.class)
                .parsePricingBasicRule(pricingBasicRuleJson);
        ProductQueryResult result = new ProductQueryResult();
        result.setPricingBasicRule(parsed);
        return Mappers.getMapper(ProductWebMapper.class).toPricingRuleResponse(result);
    }

    private PricingBasicRuleResponse toResponseWithConfig(Map<String, Object> typeSpecificConfig) {
        ProductQueryResult result = new ProductQueryResult();
        result.setPricingBasicRule(new PricingBasicRule(null, null, null, null, typeSpecificConfig));
        return Mappers.getMapper(ProductWebMapper.class).toPricingRuleResponse(result);
    }

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

    @Test
    void shouldExtractManagementFeeRateThroughRealJsonChain() {
        // 真机形态：t_product_view.pricing_basic_rule_json 原文（0.015 = 投连险年管理费率 1.5%）
        PricingBasicRuleResponse response =
                toResponseViaJson("{\"pricingType\":\"FIXED\",\"baseRate\":0.02,"
                        + "\"typeSpecificConfig\":{\"managementFeeRate\":0.015}}");

        assertThat(response.getManagementFeeRate()).isNotNull();
        assertThat(response.getManagementFeeRate()).isEqualByComparingTo(new BigDecimal("0.015"));
    }

    @Test
    void shouldExtractManagementFeeRateFromNumericAndStringForms() {
        // Map 值的多种到达形态：BigDecimal（fastjson2 直读）/ Double（Jackson 等）/
        // Integer / String（前端或手工配置），全部须提取为同一口径的值
        assertThat(toResponseWithConfig(Map.of("managementFeeRate", new BigDecimal("0.015")))
                .getManagementFeeRate()).isEqualByComparingTo(new BigDecimal("0.015"));
        assertThat(toResponseWithConfig(Map.of("managementFeeRate", 0.02D))
                .getManagementFeeRate()).isEqualByComparingTo(new BigDecimal("0.02"));
        assertThat(toResponseWithConfig(Map.of("managementFeeRate", 0))
                .getManagementFeeRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(toResponseWithConfig(Map.of("managementFeeRate", "0.015"))
                .getManagementFeeRate()).isEqualByComparingTo(new BigDecimal("0.015"));
    }

    @Test
    void shouldReturnNullWhenManagementFeeRateAbsent() {
        // 「未配置」= 该产品不收取管理费（合法形态），不得回落任何默认值
        assertThat(toResponseWithConfig(null).getManagementFeeRate()).isNull();
        assertThat(toResponseWithConfig(Map.of()).getManagementFeeRate()).isNull();
        Map<String, Object> otherKeys = new HashMap<>();
        otherKeys.put("investmentAccountType", "UNIVERSAL");
        assertThat(toResponseWithConfig(otherKeys).getManagementFeeRate()).isNull();
    }

    @Test
    void shouldReturnNullWhenManagementFeeRateUnparseable() {
        // 已知边界（javadoc 登记）：值存在但不可解析 ⇒ 按未配置处理，读路径不因单个脏值字段
        // 拖垮产品详情/列表；越界数值的最终防线在 investment 侧账户创建值域守卫
        assertThat(toResponseWithConfig(Map.of("managementFeeRate", "abc")).getManagementFeeRate())
                .isNull();
    }
}
