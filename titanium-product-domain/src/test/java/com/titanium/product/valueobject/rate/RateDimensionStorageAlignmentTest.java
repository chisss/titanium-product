package com.titanium.product.valueobject.rate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/**
 * 维度声明 ↔ 行级存储字段的对齐守卫（G01/AC-05 收口 · 索引文档 §五 第 10 条「声明即登记」）。
 *
 * <h3>为什么需要它</h3>
 * <p>
 * {@link RateDimensionKey} 声明了「有哪些维度」，{@link RateTableRow} 声明了「每行能存哪些维度取值」。
 * 二者是<b>两套独立的声明</b>，其间没有编译期关联：给枚举加一个维度键、却忘了给行加存储分量，
 * <b>编译通过、既有测试全绿</b>，症状是「这个维度在下拉里选得到、配上却永不命中」——
 * 与「字典缺条目」同族的<b>静默失效</b>。
 * </p>
 * <p>
 * 🔴 本测试的映射表**刻意放在测试里**，而不是把存储列名塞进 {@code metadata} 的枚举：
 * 存储列名是产品域的<b>实现细节</b>，写进共享内核会让别的域看见本域的落库形态（分层污染）。
 * 放在测试里恰好形成「两侧互比」——加维度时只要任一侧没同步，本测试即红。
 * </p>
 * <p>
 * 🔴 这张表同时使 {@link RateDimensionKey.ValueType} 获得<b>执行点</b>：分量个数由取值形态决定
 * （{@code RANGE} 需半开区间两列，其余一列）。此前 ValueType 无任何消费方，属「仅枚举占位」。
 * </p>
 */
class RateDimensionStorageAlignmentTest {

    /**
     * 维度键 → 承载其取值的 {@link RateTableRow} 分量名。
     * <p>
     * 🔴 顺序即 {@code [下界(含), 上界(不含)]}，与 {@link RateDimensionKey.ValueType#RANGE} 的半开区间语义一致。
     * </p>
     */
    private static final Map<String, List<String>> DIMENSION_STORAGE_FIELDS = Map.ofEntries(
            Map.entry("age", List.of("ageFrom", "ageToExclusive")),
            Map.entry("gender", List.of("gender")),
            Map.entry("paymentTerm", List.of("paymentTermYears")),
            Map.entry("coverageTerm", List.of("coverageTermYears")),
            Map.entry("occupationClass", List.of("occupationClass")),
            Map.entry("region", List.of("region")),
            Map.entry("vehicleType", List.of("vehicleType")),
            Map.entry("sumInsured", List.of("sumInsuredFrom", "sumInsuredToExclusive")),
            Map.entry("smoking", List.of("smoking")),
            Map.entry("socialInsurance", List.of("socialInsurance")),
            Map.entry("deductible", List.of("deductibleFrom", "deductibleToExclusive")),
            // G01-R3-F1 补齐（2026-09-26）：7 个原已存在于现网费率表数据、却不在枚举内的维度键
            Map.entry("employeeCount", List.of("employeeCount")),
            Map.entry("payrollAmount", List.of("payrollAmountFrom", "payrollAmountToExclusive")),
            Map.entry("propertyUsage", List.of("propertyUsage")),
            Map.entry("propertyValue", List.of("propertyValueFrom", "propertyValueToExclusive")),
            Map.entry("occupancyType", List.of("occupancyType")),
            Map.entry("fireProtectionLevel", List.of("fireProtectionLevel")),
            Map.entry("ncd", List.of("ncd")));

    @Test
    @DisplayName("枚举里的每个维度键都必须声明其行级存储分量（双向：多一个少一个都红）")
    void everyDimensionKeyMustDeclareStorageFields() {
        Set<String> declared = Arrays.stream(RateDimensionKey.values())
                .map(RateDimensionKey::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 双向报差集：只报「枚举有、映射无」会漏掉「映射有、枚举无」（改键名后残留的孤儿条目）
        Set<String> onlyInEnum = new TreeSet<>(declared);
        onlyInEnum.removeAll(DIMENSION_STORAGE_FIELDS.keySet());
        Set<String> onlyInMapping = new TreeSet<>(DIMENSION_STORAGE_FIELDS.keySet());
        onlyInMapping.removeAll(declared);

        assertEquals(Set.of(), onlyInEnum,
                "枚举已声明但未登记行级存储分量的维度键: " + onlyInEnum);
        assertEquals(Set.of(), onlyInMapping,
                "已登记行级存储分量、但枚举中并不存在的维度键（改名后的残留？）: " + onlyInMapping);
    }

    @Test
    @DisplayName("登记的分量名必须真实存在于 RateTableRow 上（防「登记了不存在的列」）")
    void declaredStorageFieldsMustExistOnRateTableRow() {
        Set<String> rowComponents = Arrays.stream(RateTableRow.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, List<String>> dangling = new LinkedHashMap<>();
        DIMENSION_STORAGE_FIELDS.forEach((dimension, fields) -> {
            List<String> missing = fields.stream().filter(field -> !rowComponents.contains(field)).toList();
            if (!missing.isEmpty()) {
                dangling.put(dimension, missing);
            }
        });

        assertEquals(Map.of(), dangling,
                "以下维度登记了 RateTableRow 上不存在的分量（写错了名字，或忘了给行加字段）: " + dangling);
    }

    @Test
    @DisplayName("分量个数由 ValueType 决定：RANGE 需半开区间两列，TEXT/INTEGER/ENUM_REF 各一列")
    void storageFieldCountMustMatchValueType() {
        Map<String, String> mismatched = new LinkedHashMap<>();
        for (RateDimensionKey key : RateDimensionKey.values()) {
            List<String> fields = DIMENSION_STORAGE_FIELDS.get(key.getCode());
            int expected = switch (key.getValueType()) {
                case RANGE -> 2;
                case TEXT, INTEGER, ENUM_REF -> 1;
            };
            if (fields.size() != expected) {
                mismatched.put(key.getCode(),
                        "ValueType=" + key.getValueType() + " 期望 " + expected + " 列，实为 " + fields);
            }
        }

        assertEquals(Map.of(), mismatched, "取值形态与行级存储分量个数不符: " + mismatched);
    }

    @Test
    @DisplayName("区间型维度的两列命名必须成对（<code>XxxFrom</code> / <code>XxxToExclusive</code>），否则匹配与重叠判定无从对号")
    void rangeDimensionsMustUseHalfOpenPairNaming() {
        Map<String, String> bad = new LinkedHashMap<>();
        for (RateDimensionKey key : RateDimensionKey.values()) {
            if (key.getValueType() != RateDimensionKey.ValueType.RANGE) {
                continue;
            }
            List<String> fields = DIMENSION_STORAGE_FIELDS.get(key.getCode());
            boolean paired = fields.equals(List.of(key.getCode() + "From", key.getCode() + "ToExclusive"));
            if (!paired) {
                bad.put(key.getCode(), fields.toString());
            }
        }

        assertTrue(bad.isEmpty(),
                "区间型维度的分量命名偏离 <code>From</code>/<code>ToExclusive</code> 约定（半开区间语义会被误读）: " + bad);
    }
}
