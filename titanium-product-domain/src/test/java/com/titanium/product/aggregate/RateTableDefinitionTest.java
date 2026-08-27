package com.titanium.product.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.common.enums.RateUnit;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.RateTableRow;
import com.titanium.product.valueobject.RateTableValidationResult;

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

    private RateTableDefinition draft() {
        return RateTableDefinition.createDraft(
                "TABLE-1", "PRODUCT-1", "RATE-LIFE", "V1.0", RateUnit.SUM_INSURED_RATIO, "cny",
                LocalDateTime.of(2026, 1, 1, 0, 0), null,
                List.of("age", "gender", "paymentTerm", "coverageTerm"), "TENANT-1");
    }

    private RateTableRow row(String rowId, int ageFrom, int ageToExclusive, String gender, BigDecimal rate) {
        return new RateTableRow(rowId, ageFrom, ageToExclusive, gender, 20, 20, rate, null, null);
    }
}
