package com.titanium.product.pricing.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

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
        assertEquals(ProductErrorCode.RATE_TABLE_STATUS_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRejectRowsWhoseDimensionsOverlap() {
        RateTableDefinition table = draft();
        table.replaceRows(List.of(
                row("ALL-18-61", 18, 61, "ALL", new BigDecimal("0.0010")),
                row("M-40-61", 40, 61, "M", new BigDecimal("0.0025"))));

        PricingDomainException exception = assertThrows(
                PricingDomainException.class, table::validateForPublish);

        assertEquals(ProductErrorCode.RATE_TABLE_ROW_CONFLICT.getCode(), exception.getErrorCode());
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

        assertEquals(ProductErrorCode.RATE_TABLE_STATUS_INVALID.getCode(), exception.getErrorCode());
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

        assertEquals(ProductErrorCode.RATE_TABLE_ROW_CONFLICT.getCode(), exception.getErrorCode());
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

        assertEquals(ProductErrorCode.RATE_TABLE_ROW_CONFLICT.getCode(), exception.getErrorCode());
        assertTrue(exception.getMessage().contains("维度重复"), exception.getMessage());
        // 拒绝不得半提交：草稿行集合保持替换前状态
        assertEquals(List.of(), table.rows());
    }

    private RateTableDefinition draft() {
        return RateTableDefinition.createDraft(
                "TABLE-1", "PRODUCT-1", "RATE-LIFE", "V1.0", RateUnit.SUM_INSURED_RATIO, "cny",
                LocalDateTime.of(2026, 1, 1, 0, 0), null,
                List.of("age", "gender", "paymentTerm", "coverageTerm"), "TENANT-1");
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
}
