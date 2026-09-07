package com.titanium.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.RateUnit;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.rate.RateTableCriteria;
import com.titanium.product.valueobject.rate.RateTableRow;
import com.titanium.product.valueobject.rate.RateTableSnapshot;

class RateTableMatchingServiceTest {

    private final RateTableMatchingService service = new RateTableMatchingService();

    @Test
    void shouldMatchSingleRowWithExclusiveAgeUpperBound() {
        RateTableRow younger = row("row-younger", 18, 30, "M", 10, 20, "0.01000000");
        RateTableRow older = row("row-older", 30, 41, "M", 10, 20, "0.01200000");

        RateTableRow matched = service.match(snapshot(List.of(younger, older)),
                new RateTableCriteria(30, "male", 10, 20));

        assertEquals("row-older", matched.rowId());
    }

    @Test
    void shouldRejectWhenNoRowMatches() {
        RateTableSnapshot snapshot = snapshot(List.of(
                row("row-female", 18, 61, "F", 10, 20, "0.01000000")));

        PricingDomainException exception = assertThrows(PricingDomainException.class,
                () -> service.match(snapshot, new RateTableCriteria(35, "M", 10, 20)));

        assertEquals(ProductErrorCode.RATE_ROW_NOT_MATCHED.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRejectWhenExactAndWildcardRowsBothMatch() {
        RateTableSnapshot snapshot = snapshot(List.of(
                row("row-all", 18, 61, "ALL", null, null, "0.01000000"),
                row("row-exact", 18, 61, "F", 10, 20, "0.01200000")));

        PricingDomainException exception = assertThrows(PricingDomainException.class,
                () -> service.match(snapshot, new RateTableCriteria(35, "F", 10, 20)));

        assertEquals(ProductErrorCode.RATE_ROW_MULTIPLE_MATCHED.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldMatchRowWithOccupationClassWhenCriteriaProvidesSameValue() {
        RateTableSnapshot snapshot = snapshot(List.of(
                rowWithOccupation("row-class-1", "1", "0.01000000"),
                rowWithOccupation("row-class-2", "2", "0.01500000")));

        RateTableRow matched = service.match(snapshot,
                new RateTableCriteria(35, "M", 10, 20, "2", null, null));

        assertEquals("row-class-2", matched.rowId());
    }

    @Test
    void shouldNotMatchOccupationSpecificRowWhenCriteriaOmitsDimension() {
        RateTableSnapshot snapshot = snapshot(List.of(
                rowWithOccupation("row-class-1", "1", "0.01000000")));

        PricingDomainException exception = assertThrows(PricingDomainException.class,
                () -> service.match(snapshot, new RateTableCriteria(35, "M", 10, 20)));

        assertEquals(ProductErrorCode.RATE_ROW_NOT_MATCHED.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldMatchWildcardOccupationRowWhenCriteriaProvidesValue() {
        RateTableSnapshot snapshot = snapshot(List.of(
                rowWithOccupation("row-occupation-wildcard", null, "0.01000000")));

        RateTableRow matched = service.match(snapshot,
                new RateTableCriteria(35, "M", 10, 20, "3", "CN", null));

        assertEquals("row-occupation-wildcard", matched.rowId());
    }

    private RateTableSnapshot snapshot(List<RateTableRow> rows) {
        return new RateTableSnapshot(
                "table-1", "product-1", "LIFE_BASE", "V1", RateUnit.SUM_INSURED_RATIO, "CNY",
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "content-hash", rows);
    }

    private RateTableRow row(
            String rowId, Integer ageFrom, Integer ageToExclusive, String gender,
            Integer paymentTerm, Integer coverageTerm, String rate) {
        return new RateTableRow(
                rowId, ageFrom, ageToExclusive, gender, paymentTerm, coverageTerm,
                new BigDecimal(rate), null, null);
    }

    private RateTableRow rowWithOccupation(String rowId, String occupationClass, String rate) {
        return new RateTableRow(
                rowId, 18, 61, "M", 10, 20, new BigDecimal(rate), null, null,
                occupationClass, null, null);
    }
}
