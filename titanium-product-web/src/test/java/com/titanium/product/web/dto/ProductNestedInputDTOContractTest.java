package com.titanium.product.web.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.titanium.product.valueobject.config.InsureCondition;
import com.titanium.product.valueobject.pricing.pricing.PricingBasicRule;

/**
 * 产品入参嵌套 DTO 契约守护：web DTO 字段必须与领域值对象对齐
 * <p>
 * 嵌套 DTO（{@code InsureConditionDTO}/{@code PricingBasicRuleDTO}）→ 领域值对象由 MapStruct 隐式映射。
 * MapStruct 对目标端<b>完全不存在的属性</b>既不报错也不映射，只做静默丢弃——运营在后台配置了却从不生效，
 * 且无任何日志或异常可循。历史上 {@code pricingFactors}/{@code minPremium}/{@code maxPremium}/
 * {@code rateTableId}（定价规则）与 {@code occupationRestrictions}/{@code healthDeclaration}/
 * {@code regionRestrictions}（投保条件）共七个字段即由此产生，故以反射断言把
 * 「DTO 字段 ⊆ 领域值对象组件」固化为构建期约束。
 * </p>
 * <p>
 * 约束为单向（DTO ⊆ VO）：领域值对象可以有 web 入参不承载的组件，但 DTO 不得有领域无归属的字段。
 * </p>
 */
class ProductNestedInputDTOContractTest {

    /**
     * 待守护的「嵌套 DTO ↔ 领域值对象」配对
     */
    private static Stream<Arguments> nestedDtoPairs() {
        return Stream.of(
                Arguments.of(InsureConditionDTO.class, InsureCondition.class),
                Arguments.of(PricingBasicRuleDTO.class, PricingBasicRule.class));
    }

    /**
     * DTO 的每个字段名都必须能在领域值对象上找到同名组件，否则该入参必然被静默丢弃
     */
    @ParameterizedTest
    @MethodSource("nestedDtoPairs")
    void everyDtoFieldMustExistOnDomainValueObject(Class<?> dtoType, Class<?> valueObjectType) {
        Set<String> domainComponents = Arrays.stream(valueObjectType.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        List<String> ghostFields = dtoTypeFields(dtoType).stream()
                .map(Field::getName)
                .filter(name -> !domainComponents.contains(name))
                .toList();

        assertTrue(ghostFields.isEmpty(),
                () -> "%s 的以下字段在领域值对象 %s 中不存在，会被 MapStruct 静默丢弃，"
                        .formatted(dtoType.getSimpleName(), valueObjectType.getSimpleName())
                        + "请先扩展领域值对象或删除该入参: " + ghostFields);
    }

    /**
     * 产品级 factors 已被聚合 G7 守卫封杀，保留该入参仅为让误用者收到明确拒绝（而非被静默忽略），须持续标注废弃
     */
    @Test
    void factorsFieldMustStayMarkedDeprecated() throws NoSuchFieldException {
        Field factors = PricingBasicRuleDTO.class.getDeclaredField("factors");

        assertTrue(factors.isAnnotationPresent(Deprecated.class),
                "PricingBasicRuleDTO.factors 必须标注 @Deprecated：产品级 factors 不参与试算，"
                        + "非空值会被聚合以 PRICING_FACTORS_DEPRECATED 拒绝，请改用定价计划的动态因子");
    }

    /** 取 DTO 自身声明的实例字段（排除编译器合成的 this$0 等） */
    private static List<Field> dtoTypeFields(Class<?> dtoType) {
        return Arrays.stream(dtoType.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .toList();
    }
}
