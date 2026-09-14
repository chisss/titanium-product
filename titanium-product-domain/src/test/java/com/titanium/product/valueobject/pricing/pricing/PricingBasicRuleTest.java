package com.titanium.product.valueobject.pricing.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 定价基础规则值对象契约测试（m16-1904）
 * <p>
 * 固化「产品级 factors 已废弃」这一处置结论在<b>领域侧</b>的落点：{@code factors} 组件必须持续标注
 * {@code @Deprecated}。web 侧同口径断言见
 * {@code ProductNestedInputDTOContractTest#factorsFieldMustStayMarkedDeprecated}——两侧任一被摘除即
 * 测试失败，避免「DTO 标了废弃、领域值对象仍看似合法」的认知分叉，也防止后续有人误删标注
 * 让误用者重新失去提示。
 * </p>
 */
class PricingBasicRuleTest {

    @Test
    @DisplayName("factors 组件必须持续标注 @Deprecated，且传播到字段与访问器")
    void factorsComponentMustStayMarkedDeprecated() throws NoSuchFieldException, NoSuchMethodException {
        Field field = PricingBasicRule.class.getDeclaredField("factors");
        Method accessor = PricingBasicRule.class.getDeclaredMethod("factors");

        assertTrue(field.isAnnotationPresent(Deprecated.class),
                "PricingBasicRule.factors 必须标注 @Deprecated：产品级 factors 不参与试算，"
                        + "非空值会被聚合以 PRICING_FACTORS_DEPRECATED 拒绝，请改用定价计划的动态因子");
        assertTrue(accessor.isAnnotationPresent(Deprecated.class),
                "record 组件的废弃标注须传播到访问器，否则 G7 守卫等读取点收不到编译期提示");
    }

    @Test
    @DisplayName("废弃面仅限 factors：其余组件仍是在用配置")
    void onlyFactorsComponentIsDeprecated() {
        List<String> deprecated = Arrays.stream(PricingBasicRule.class.getRecordComponents())
                .filter(component -> component.getAccessor().isAnnotationPresent(Deprecated.class))
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of("factors"), deprecated,
                "废弃面仅限 factors——pricingType/baseRate/rateFormula/typeSpecificConfig 仍为在用配置，"
                        + "新增废弃组件须同步处置文档与消费方，不得静默扩大");
    }
}
