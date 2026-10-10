package com.titanium.product.pricing.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.product.RateDimensionKey;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.common.enums.RateUnit;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.rate.RateTableRow;
import com.titanium.product.valueobject.rate.RateTableValidationResult;

class RateTableDefinitionTest {

    @Test
    void shouldPublishValidDraftAndLockRows() {
        RateTableDefinition table = draft();
        table.replaceRows(List.of(
                row("M-18-40", 18, 40, "M", new BigDecimal("0.0015")),
                row("F-18-40", 18, 40, "F", new BigDecimal("0.0012"))));

        RateTableValidationResult validation = table.publish();

        assertEquals(RateTableStatus.PUBLISHED, table.status());
        assertEquals(2, validation.rowCount());
        assertEquals(validation.contentHash(), table.contentHash());
        assertEquals(64, table.contentHash().length());
        PricingDomainException exception = assertThrows(
                PricingDomainException.class, () -> table.replaceRows(List.of()));
        assertEquals(ProductErrorCode.RATE_TABLE_STATUS_INVALID.getCode(), exception.getErrorCodeValue());
    }

    @Test
    void shouldRejectRowsWhoseDimensionsOverlap() {
        RateTableDefinition table = draft();
        table.replaceRows(List.of(
                row("ALL-18-61", 18, 61, "ALL", new BigDecimal("0.0010")),
                row("M-40-61", 40, 61, "M", new BigDecimal("0.0025"))));

        PricingDomainException exception = assertThrows(
                PricingDomainException.class, table::validateForPublish);

        assertEquals(ProductErrorCode.RATE_TABLE_ROW_CONFLICT.getCode(), exception.getErrorCodeValue());
    }

    @Test
    void shouldProduceStableContentHashIndependentOfRowIdsAndOrder() {
        RateTableDefinition first = draft();
        first.replaceRows(List.of(
                row("ROW-A", 18, 40, "M", new BigDecimal("0.001500")),
                row("ROW-B", 40, 61, "M", new BigDecimal("0.002500"))));
        RateTableDefinition second = draft();
        second.replaceRows(List.of(
                row("OTHER-B", 40, 61, "m", new BigDecimal("0.0025")),
                row("OTHER-A", 18, 40, "M", new BigDecimal("0.0015"))));

        assertEquals(
                first.validateForPublish().contentHash(), second.validateForPublish().contentHash());
        assertNotEquals(first.rows().getFirst().rowId(), second.rows().getLast().rowId());
    }

    @Test
    void shouldOnlyRetirePublishedTable() {
        RateTableDefinition table = draft();

        PricingDomainException exception = assertThrows(PricingDomainException.class, table::retire);

        assertEquals(ProductErrorCode.RATE_TABLE_STATUS_INVALID.getCode(), exception.getErrorCodeValue());
    }

    @Test
    void shouldAcceptExtendedDimensionKeys() {
        RateTableDefinition table = RateTableDefinition.createDraft(
                "TABLE-1", "PRODUCT-1", "RATE-ACCIDENT", "V1.0", RateUnit.SUM_INSURED_RATIO, "cny",
                LocalDateTime.of(2026, 1, 1, 0, 0), null,
                List.of("age", "gender", "paymentTerm", "coverageTerm", "occupationClass", "region"),
                "TENANT-1");
        table.replaceRows(List.of(
                occupationRow("CLS-1", "1", new BigDecimal("0.0015")),
                occupationRow("CLS-2", "2", new BigDecimal("0.0025"))));

        RateTableValidationResult validation = table.validateForPublish();

        assertEquals(2, validation.rowCount());
    }

    @Test
    void shouldRejectRowsWhoseOccupationDimensionOverlaps() {
        RateTableDefinition table = draft();
        table.replaceRows(List.of(
                occupationRow("CLS-WILD", null, new BigDecimal("0.0010")),
                occupationRow("CLS-1", "1", new BigDecimal("0.0015"))));

        PricingDomainException exception = assertThrows(
                PricingDomainException.class, table::validateForPublish);

        assertEquals(ProductErrorCode.RATE_TABLE_ROW_CONFLICT.getCode(), exception.getErrorCodeValue());
    }

    @Test
    void shouldRejectDuplicateDimensionRowsOnSave() {
        // D-501-34 实测形态：两行维度完全相同（年龄不限、性别不限、缴费/保障年限均为 1），仅费率不同。
        // 保存阶段即应拒绝——同一维度组合在库侧有唯一约束 uk_product_rate_row_dimension，
        // 此前保存期无守卫，非法行直达落库并以 HTTP 500 呈现。
        RateTableDefinition table = draft();

        PricingDomainException exception = assertThrows(
                PricingDomainException.class,
                () -> table.replaceRows(List.of(
                        flatRow("ROW-A", new BigDecimal("0.001")),
                        flatRow("ROW-B", new BigDecimal("0.002")))));

        assertEquals(ProductErrorCode.RATE_TABLE_ROW_CONFLICT.getCode(), exception.getErrorCodeValue());
        assertTrue(exception.getMessage().contains("维度重复"), exception.getMessage());
        // 拒绝不得半提交：草稿行集合保持替换前状态
        assertEquals(List.of(), table.rows());
    }

    private RateTableDefinition draft() {
        return draftWithDimensions(List.of("age", "gender", "paymentTerm", "coverageTerm"));
    }

    private RateTableDefinition draftWithDimensions(List<String> dimensionKeys) {
        return RateTableDefinition.createDraft(
                "TABLE-1", "PRODUCT-1", "RATE-LIFE", "V1.0", RateUnit.SUM_INSURED_RATIO, "cny",
                LocalDateTime.of(2026, 1, 1, 0, 0), null,
                dimensionKeys, "TENANT-1");
    }

    private RateTableRow row(String rowId, int ageFrom, int ageToExclusive, String gender, BigDecimal rate) {
        return new RateTableRow(rowId, ageFrom, ageToExclusive, gender, 20, 20, rate, null, null);
    }

    private RateTableRow occupationRow(String rowId, String occupationClass, BigDecimal rate) {
        return new RateTableRow(rowId, 18, 61, "ALL", 20, 20, rate, null, null,
                occupationClass, null, null);
    }

    /** 维度全开放的行（年龄/性别不限、缴费与保障年限固定 1），仅费率可变——用于构造维度重复对 */
    private RateTableRow flatRow(String rowId, BigDecimal rate) {
        return new RateTableRow(rowId, null, null, "ALL", 1, 1, rate, null, null);
    }

    // ==================== G01/AC-05 费率维度字典化与扩展 ====================

    @Test
    @DisplayName("AC-05 正向：sumInsured 可作维度键建表（取值域来自 RateDimensionKey，不再是聚合内字面量）")
    void shouldAcceptSumInsuredAsDimensionKey() {
        RateTableDefinition table = draftWithDimensions(List.of("age", "sumInsured"));

        assertEquals(List.of("age", "sumInsured"), table.dimensionKeys());
    }

    @Test
    @DisplayName("AC-05 反向：未知维度键被拒，且错误信息点名该键并附受支持全集")
    void shouldRejectUnknownDimensionKey() {
        PricingDomainException exception = assertThrows(
                PricingDomainException.class, () -> draftWithDimensions(List.of("age", "foo")));

        assertEquals(ProductErrorCode.RATE_TABLE_VALIDATION_FAILED.getCode(), exception.getErrorCodeValue());
        // 只报「不支持」而不点名，运营不知道改哪一个键；不附全集则只能逐个试
        assertTrue(exception.getMessage().contains("foo"), exception.getMessage());
        assertTrue(exception.getMessage().contains("sumInsured"), exception.getMessage());
    }

    @Test
    @DisplayName("枚举里的每一个维度键都必须被建表校验接受——防止聚合内再长出第二份子集清单")
    void shouldAcceptEveryDimensionKeyDeclaredByMetadataEnum() {
        for (RateDimensionKey key : RateDimensionKey.values()) {
            RateTableDefinition table = draftWithDimensions(List.of(key.getCode()));
            assertEquals(List.of(key.getCode()), table.dimensionKeys(), "维度键被拒: " + key.getCode());
        }
    }

    @Test
    @DisplayName("受支持维度全集由枚举派生，与 RateDimensionKey 的 code 集合相等（唯一权威）")
    void supportedDimensionCodesMustEqualEnumCodes() {
        Set<String> expected = Arrays.stream(RateDimensionKey.values())
                .map(RateDimensionKey::getCode)
                .collect(Collectors.toSet());

        assertEquals(expected, RateTableDefinition.supportedDimensionCodes());
    }

    @Test
    @DisplayName("维度键重复/为空各有独立拒绝理由，不与「不支持」混为一谈")
    void shouldRejectEmptyOrDuplicatedDimensionKeys() {
        PricingDomainException empty = assertThrows(
                PricingDomainException.class, () -> draftWithDimensions(List.of()));
        assertTrue(empty.getMessage().contains("不能为空"), empty.getMessage());

        PricingDomainException duplicated = assertThrows(
                PricingDomainException.class, () -> draftWithDimensions(List.of("age", "age")));
        assertTrue(duplicated.getMessage().contains("重复"), duplicated.getMessage());
    }

    @Test
    @DisplayName("保额区间重叠在发布期被拦；仅相接（[1000,5000) 与 [5000,10000)）不算重叠")
    void shouldRejectOverlappingSumInsuredRanges() {
        RateTableDefinition overlapped = draftWithDimensions(List.of("age", "sumInsured"));
        overlapped.replaceRows(List.of(
                sumInsuredRow("SI-LOW", "1000", "5000", "0.0010"),
                sumInsuredRow("SI-HIGH", "3000", "8000", "0.0012")));

        PricingDomainException exception = assertThrows(
                PricingDomainException.class, overlapped::validateForPublish);

        assertEquals(ProductErrorCode.RATE_TABLE_ROW_CONFLICT.getCode(), exception.getErrorCodeValue());

        // 正对照：半开区间仅相接时不得误判重叠——否则上面的断言只证明了「判定恒真」
        RateTableDefinition adjacent = draftWithDimensions(List.of("age", "sumInsured"));
        adjacent.replaceRows(List.of(
                sumInsuredRow("SI-LOW", "1000", "5000", "0.0010"),
                sumInsuredRow("SI-HIGH", "5000", "10000", "0.0012")));

        assertEquals(2, adjacent.validateForPublish().rowCount());
    }

    @Test
    @DisplayName("金额型维度用数值比较：1000 与 1000.00 是同一区间，必须判为重叠（equals 会漏过）")
    void shouldCompareAmountRangesNumericallyNotByEquals() {
        RateTableDefinition table = draftWithDimensions(List.of("age", "deductible"));
        table.replaceRows(List.of(
                deductibleRow("DD-A", "1000", "2000", "0.0010"),
                deductibleRow("DD-B", "1000.00", "2000.00", "0.0012")));

        PricingDomainException exception = assertThrows(
                PricingDomainException.class, table::validateForPublish);

        assertEquals(ProductErrorCode.RATE_TABLE_ROW_CONFLICT.getCode(), exception.getErrorCodeValue());
    }

    @Test
    @DisplayName("吸烟维度为文本型：两侧都限定且同值才重叠，不同值不重叠")
    void shouldRejectOverlappingTextDimensionsSmokingAndSocialInsurance() {
        RateTableDefinition table = draftWithDimensions(List.of("age", "smoking"));
        table.replaceRows(List.of(
                textDimensionRow("SMK-Y", "YES", "0.0010"),
                textDimensionRow("SMK-N", "NO", "0.0012")));

        // 不同吸烟情况是两条互斥的费率行，不得判为重复命中
        assertEquals(2, table.validateForPublish().rowCount());

        RateTableDefinition sameSmoking = draftWithDimensions(List.of("age", "smoking"));
        sameSmoking.replaceRows(List.of(
                textDimensionRow("SMK-Y1", "YES", "0.0010"),
                textDimensionRow("SMK-Y2", "yes", "0.0012")));

        // 大小写不敏感（与 RateTableRow 的匹配语义一致），故必须判为重叠
        assertThrows(PricingDomainException.class, sameSmoking::validateForPublish);
    }

    /** 保额区间行：其余维度全开放，仅保额区间与费率可变。 */
    private RateTableRow sumInsuredRow(String rowId, String from, String toExclusive, String rate) {
        return new RateTableRow(rowId, null, null, "ALL", 1, 1, new BigDecimal(rate), null, null,
                null, null, null, new BigDecimal(from), new BigDecimal(toExclusive),
                null, null, null, null);
    }

    /** 免赔额区间行：其余维度全开放。 */
    private RateTableRow deductibleRow(String rowId, String from, String toExclusive, String rate) {
        return new RateTableRow(rowId, null, null, "ALL", 1, 1, new BigDecimal(rate), null, null,
                null, null, null, null, null, null, null,
                new BigDecimal(from), new BigDecimal(toExclusive));
    }

    /** 吸烟维度行：其余维度全开放。 */
    private RateTableRow textDimensionRow(String rowId, String smoking, String rate) {
        return new RateTableRow(rowId, null, null, "ALL", 1, 1, new BigDecimal(rate), null, null,
                null, null, null, null, null, smoking, null, null, null);
    }
}
