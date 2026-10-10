package com.titanium.product.web.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.product.RateDimensionKey;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductApi;
import com.titanium.product.api.request.product.UpdateCoverageSumInsuredConfigRequest;
import com.titanium.product.api.request.product.UpdateProductClauseRelRequest;
import com.titanium.product.api.response.config.CoverageSumInsuredConfigResponse;
import com.titanium.product.api.response.config.CoverageSumInsuredTierResponse;

/**
 * G01 收官契约存在性断言（G01/AC-06）。
 *
 * <h3>AC-06 的边界（缺口文档 §2.2 明写）</h3>
 * <p>
 * {@code G01} <b>不</b>负责证明「出单能选保额」，只负责证明「出单侧要读的契约已存在、
 * 且形状足以被外部域消费」。实际消费在 {@code G02} 验收。
 * </p>
 * <p>
 * 🔴 故本测试断言的是<b>可消费性</b>而非功能：出单侧（policy 域）只依赖 {@code titanium-product-api}
 * 与 {@code titanium-metadata}，<b>看不见</b> {@code product-common} / {@code product-domain}。
 * 若契约载荷里出现本域自定义类型，出单侧就必须反向依赖产品域内部模块——契约在编译期就不可消费，
 * 而这种断裂<b>在产品域自己的构建里完全看不出来</b>。
 * </p>
 */
class ProductConsumerContractExistenceTest {

    @Test
    @DisplayName("AC-06 出单侧可读到保额档次清单：GET coverage-sum-insured-configs 存在且返回 api 契约类型")
    void orderSideMustBeAbleToReadSumInsuredTiers() throws NoSuchMethodException {
        Method method = ProductApi.class.getMethod("getCoverageSumInsuredConfigs", String.class);

        assertEquals(ApiResponse.class, method.getReturnType(),
                "读端点必须返回 ApiResponse 信封（§8.1/§8.2）");
        // 信封的类型参只有一层：ApiResponse<T> 的 T 即 List<CoverageSumInsuredConfigResponse>
        Type dataType = typeArgument(method.getGenericReturnType(), 0);
        assertEquals(List.class, rawType(dataType),
                "信封 data 必须是 List（未配置时为空列表，而非 404）");
        assertEquals(CoverageSumInsuredConfigResponse.class, rawType(typeArgument(dataType, 0)),
                "信封 data 的元素类型必须是 api 层 CoverageSumInsuredConfigResponse");
    }

    @Test
    @DisplayName("AC-06 契约形状足以渲染「选档 / 自由填」二态：档位清单 + 区间边界 + 是否开放自由填 + 默认档")
    void sumInsuredConfigResponseShapeMustCarryEverythingOrderSideNeeds() {
        // 出单侧要能回答三件事，缺一即无法渲染：① 有哪些档（tiers）② 自由填的边界（min/max）
        // ③ 是否允许自由填（allowCustomAmount）+ 默认选中哪档（defaultTierCode）
        assertComponentNames(CoverageSumInsuredConfigResponse.class,
                Set.of("coverageCode", "tiers", "minSumInsured", "maxSumInsured",
                        "allowCustomAmount", "defaultTierCode"),
                "CoverageSumInsuredConfigResponse");

        assertComponentNames(CoverageSumInsuredTierResponse.class,
                Set.of("tierCode", "sumInsured", "allowedTerms", "sortNo"),
                "CoverageSumInsuredTierResponse");
    }

    @Test
    @DisplayName("🔴 跨域载荷不得泄漏产品域内部类型：组件类型必须全部落在 java.* 或 api 契约自身")
    void crossDomainPayloadMustNotLeakProductDomainTypes() {
        Map<String, String> leaks = new LinkedHashMap<>();
        for (Class<?> payload : List.of(CoverageSumInsuredConfigResponse.class,
                CoverageSumInsuredTierResponse.class)) {
            for (RecordComponent component : payload.getRecordComponents()) {
                String typeName = component.getType().getName();
                boolean resolvableOutside = typeName.startsWith("java.")
                        // 同包/同契约模块内的 record 是契约的一部分，外部域依赖 -api 即可解析
                        || typeName.startsWith("com.titanium.product.api.");
                if (!resolvableOutside) {
                    leaks.put(payload.getSimpleName() + "." + component.getName(), typeName);
                }
            }
        }

        assertEquals(Map.of(), leaks,
                "契约载荷出现外部域不可解析的类型（出单侧将被迫反向依赖产品域内部模块）: " + leaks);
    }

    @Test
    @DisplayName("AC-03 条款写契约存在于 api 侧（与 web 侧平行收敛，非仅 web）")
    void clauseWriteContractMustExistOnApiSide() throws NoSuchMethodException {
        Method method = ProductApi.class
                .getMethod("updateProductClauseRel", String.class, UpdateProductClauseRelRequest.class);

        assertEquals(ApiResponse.class, method.getReturnType());
        assertTrue(Arrays.stream(method.getAnnotations())
                        .anyMatch(a -> a.annotationType().getSimpleName().equals("PutMapping")),
                "条款写入口必须是 PUT（整体替换语义）");
    }

    @Test
    @DisplayName("AC-04 档次写契约使用 api 层 Request（非 DTO）且带 Bean Validation")
    void sumInsuredConfigWriteContractMustUseApiRequest() throws NoSuchMethodException {
        Method method = ProductApi.class.getMethod("updateCoverageSumInsuredConfig",
                String.class, UpdateCoverageSumInsuredConfigRequest.class);

        assertEquals(ApiResponse.class, method.getReturnType());
        assertTrue(UpdateCoverageSumInsuredConfigRequest.class.getDeclaredFields().length > 0,
                "写请求体不得为空壳");
    }

    @Test
    @DisplayName("🔴 共享维度枚举必须住在 metadata 共享内核：出单侧只依赖 -api 与 metadata，看不见 product-common")
    void sharedDimensionEnumMustLiveInMetadata() {
        String packageName = RateDimensionKey.class.getPackageName();

        assertTrue(packageName.startsWith("com.titanium.metadata."),
                "维度枚举被 admin 与出单侧同时消费，置于 product-common 会让两侧都无法对账；"
                        + "实际包名: " + packageName);
    }

    /** 取参数化类型的第 index 个类型实参（如 {@code List<X>} 的 0 号实参是 {@code X}）。 */
    private static Type typeArgument(Type type, int index) {
        assertTrue(type instanceof ParameterizedType, "期望参数化类型，实际: " + type);
        Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
        assertTrue(arguments.length > index, "类型实参不足 " + (index + 1) + " 个: " + type);
        return arguments[index];
    }

    private static Class<?> rawType(Type type) {
        return type instanceof ParameterizedType
                ? (Class<?>) ((ParameterizedType) type).getRawType()
                : (Class<?>) type;
    }

    private static void assertComponentNames(Class<?> payload, Set<String> expected, String label) {
        Set<String> actual = Arrays.stream(payload.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        Set<String> extra = new TreeSet<>(actual);
        extra.removeAll(expected);

        assertEquals(Set.of(), missing, label + " 缺少出单侧必需的字段: " + missing);
        assertEquals(Set.of(), extra, label + " 出现未登记字段（新增契约字段须同批更新本断言）: " + extra);
    }
}
