package com.titanium.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationTotals;
import com.titanium.product.valueobject.pricing.PremiumCalculationEvidence;
import com.titanium.product.valueobject.pricing.lifecycle.PremiumLifecycleDifference;

class PremiumLifecycleDifferenceServiceTest {

    private final PremiumLifecycleDifferenceService service = new PremiumLifecycleDifferenceService();

    @Test
    void shouldCalculateDebitAcrossCustomerTaxAndCommissionLines() {
        PremiumLifecycleDifference difference = service.compare(
                calculation("original", PricingCalculationPurpose.ISSUANCE_CONFIRM, "100.00", "5.00", "10.00"),
                calculation("replacement", PricingCalculationPurpose.MAINTENANCE, "120.00", "6.00", "12.00"));

        assertEquals(PremiumBalanceDirection.DEBIT, difference.direction());
        assertEquals(new BigDecimal("21.00"), difference.customerAmount());
        assertEquals(new BigDecimal("1.00"), difference.taxAmount());
        assertEquals(new BigDecimal("2.00"), difference.internalCostAmount());
        assertEquals(3, difference.lines().size());
    }

    @Test
    void shouldRepresentRefundAsCreditWithPositiveAbsoluteAmount() {
        PremiumLifecycleDifference difference = service.compare(
                calculation("original", PricingCalculationPurpose.ISSUANCE_CONFIRM, "120.00", "6.00", "12.00"),
                calculation("replacement", PricingCalculationPurpose.MAINTENANCE, "100.00", "5.00", "10.00"));

        assertEquals(PremiumBalanceDirection.CREDIT, difference.direction());
        assertEquals(new BigDecimal("21.00"), difference.customerAmount());
        assertEquals(ChargeDirection.CREDIT, difference.lines().getFirst().direction());
    }

    @Test
    void shouldRejectReplacementWithoutMaintenancePurpose() {
        PremiumCalculation original = calculation(
                "original", PricingCalculationPurpose.ISSUANCE_CONFIRM, "100.00", "5.00", "10.00");
        PremiumCalculation replacement = calculation(
                "replacement", PricingCalculationPurpose.ISSUANCE_CONFIRM, "120.00", "6.00", "12.00");

        assertThrows(PricingDomainException.class, () -> service.compare(original, replacement));
    }

    @Test
    void shouldCalculateChainedDifferenceFromLatestMaintenanceCalculation() {
        PremiumCalculation original = calculation(
                "original", PricingCalculationPurpose.MAINTENANCE, "100.00", "5.00", "10.00");
        PremiumCalculation replacement = calculation(
                "replacement", PricingCalculationPurpose.MAINTENANCE, "120.00", "6.00", "12.00");

        PremiumLifecycleDifference difference = service.compare(original, replacement);

        assertEquals(PremiumBalanceDirection.DEBIT, difference.direction());
        assertEquals(new BigDecimal("21.00"), difference.customerAmount());
        assertEquals(3, difference.lines().size());
    }

    @Test
    void shouldRejectCalculationsWithDifferentBusinessNumbers() {
        PremiumCalculation original = calculation(
                "original", "policy-1", PricingCalculationPurpose.ISSUANCE_CONFIRM,
                "100.00", "5.00", "10.00");
        PremiumCalculation replacement = calculation(
                "replacement", "policy-2", PricingCalculationPurpose.MAINTENANCE,
                "120.00", "6.00", "12.00");

        assertThrows(PricingDomainException.class, () -> service.compare(original, replacement));
    }

    private PremiumCalculation calculation(
            String id,
            PricingCalculationPurpose purpose,
            String premium,
            String tax,
            String commission) {
        return calculation(id, "policy-1", purpose, premium, tax, commission);
    }

    private PremiumCalculation calculation(
            String id,
            String bizNo,
            PricingCalculationPurpose purpose,
            String premium,
            String tax,
            String commission) {
        BigDecimal premiumAmount = new BigDecimal(premium);
        BigDecimal taxAmount = new BigDecimal(tax);
        BigDecimal commissionAmount = new BigDecimal(commission);
        BigDecimal customerPayable = premiumAmount.add(taxAmount);
        return PremiumCalculation.confirm(
                id, "request-" + id, bizNo, purpose, "tenant-a", "product-1",
                LocalDateTime.of(2026, 8, 20, 12, 0), "CNY", customerPayable, customerPayable,
                customerPayable, 1, List.of(),
                new CalculationTotals(premiumAmount, taxAmount, customerPayable, commissionAmount),
                List.of(
                        line("premium", ChargeCategory.RISK_PREMIUM, AmountChannel.CUSTOMER_PRICE,
                                ChargePayerType.POLICYHOLDER, premiumAmount, true),
                        line("tax", ChargeCategory.TAX, AmountChannel.CUSTOMER_PRICE,
                                ChargePayerType.POLICYHOLDER, taxAmount, true),
                        line("commission", ChargeCategory.COMMISSION, AmountChannel.INTERNAL_COST,
                                ChargePayerType.INSURER, commissionAmount, false)),
                evidence(), Map.of(), hash(id.charAt(0)), hash('i'), hash('r'),
                LocalDateTime.of(2026, 8, 20, 12, 1));
    }

    private CalculationLine line(
            String code,
            ChargeCategory category,
            AmountChannel channel,
            ChargePayerType payer,
            BigDecimal amount,
            boolean affectsCustomerPayable) {
        return new CalculationLine(
                code, code, "V1", category, channel, ChargeDirection.DEBIT, payer,
                code.toUpperCase(), "CNY", amount, BigDecimal.ONE, amount, code, true,
                code, affectsCustomerPayable, null);
    }

    private PremiumCalculationEvidence evidence() {
        return new PremiumCalculationEvidence(
                "V1.0", "P1", hash('p'), "TABLE", "V1", hash('t'), "feature-1",
                "RULE", "V1", hash('a'), 2, "HALF_UP");
    }

    private String hash(char value) {
        return String.valueOf(value).repeat(64);
    }
}
