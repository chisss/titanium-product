package com.titanium.product.web.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.insurance.SubjectType;

class InsuranceProductDefinitionCatalogTest {

    private final InsuranceProductDefinitionCatalog catalog = new InsuranceProductDefinitionCatalog();

    @Test
    void exposesAllNineIssuanceProductTypes() {
        Set<InsuranceProductType> expected = EnumSet.of(
                InsuranceProductType.TERM_LIFE, InsuranceProductType.WHOLE_LIFE,
                InsuranceProductType.CRITICAL_ILLNESS, InsuranceProductType.MEDICAL,
                InsuranceProductType.ACCIDENT_COMPREHENSIVE, InsuranceProductType.AUTO,
                InsuranceProductType.ENTERPRISE_PROPERTY, InsuranceProductType.HOUSEHOLD_PROPERTY,
                InsuranceProductType.EMPLOYER_LIABILITY);

        assertEquals(expected, catalog.list().stream().map(item -> item.insuranceType()).collect(Collectors.toSet()));
        assertEquals(9, catalog.list().size());
    }

    @Test
    void returnsRiskSpecificSchemaDefaults() {
        var auto = catalog.list().stream().filter(item -> item.insuranceType() == InsuranceProductType.AUTO).findFirst().orElse(null);
        var employer = catalog.list().stream().filter(item -> item.insuranceType() == InsuranceProductType.EMPLOYER_LIABILITY).findFirst().orElse(null);

        assertNotNull(auto);
        assertEquals(SubjectType.VEHICLE, auto.subjectType());
        assertTrue(auto.requiredSubjectFields().contains("vin"));
        assertNotNull(employer);
        assertEquals(SubjectType.ORGANIZATION, employer.subjectType());
        assertTrue(employer.requiredSubjectFields().contains("payrollAmount"));
    }
}
