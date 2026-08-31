package com.titanium.product.web.catalog;

import java.util.List;

import org.springframework.stereotype.Component;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.insurance.SubjectType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.api.response.InsuranceProductDefinitionResponse;

/**
 * 九类险种的标准产品定义目录。
 * <p>目录是只读默认值，租户产品仍通过 Product 聚合持久化自己的版本和规则。</p>
 */
@Component
public class InsuranceProductDefinitionCatalog {

    private final List<InsuranceProductDefinitionResponse> definitions = List.of(
            definition(InsuranceProductType.TERM_LIFE, SubjectType.PERSON, ProductEnum.IssuanceMode.THREE_STEP,
                    PricingMode.RATE_TABLE, List.of("age", "gender", "occupationClass", "healthNotice"),
                    List.of("DEATH", "TOTAL_DISABILITY"), "UW-LIFE-HEALTH"),
            definition(InsuranceProductType.WHOLE_LIFE, SubjectType.PERSON, ProductEnum.IssuanceMode.THREE_STEP,
                    PricingMode.ACTUARIAL_FORMULA, List.of("age", "gender", "occupationClass", "healthNotice"),
                    List.of("DEATH", "TOTAL_DISABILITY"), "UW-LIFE-HEALTH"),
            definition(InsuranceProductType.CRITICAL_ILLNESS, SubjectType.PERSON, ProductEnum.IssuanceMode.THREE_STEP,
                    PricingMode.RATE_TABLE, List.of("age", "gender", "healthNotice", "familyHistory"),
                    List.of("CRITICAL_ILLNESS", "LIGHT_ILLNESS_WAIVER"), "UW-HEALTH-CRITICAL"),
            definition(InsuranceProductType.MEDICAL, SubjectType.PERSON, ProductEnum.IssuanceMode.TWO_STEP,
                    PricingMode.RATE_TABLE, List.of("age", "socialSecurity", "preExistingCondition", "medicalRegion"),
                    List.of("INPATIENT", "OUTPATIENT", "DRUG"), "UW-HEALTH-MEDICAL"),
            definition(InsuranceProductType.ACCIDENT_COMPREHENSIVE, SubjectType.PERSON,
                    ProductEnum.IssuanceMode.ONE_STEP, PricingMode.RATE_TABLE,
                    List.of("age", "occupationClass", "coverageRegion"),
                    List.of("ACCIDENT_DEATH", "ACCIDENT_DISABILITY", "ACCIDENT_MEDICAL"), "UW-ACCIDENT"),
            definition(InsuranceProductType.AUTO, SubjectType.VEHICLE, ProductEnum.IssuanceMode.TWO_STEP,
                    PricingMode.RATE_TABLE,
                    List.of("licensePlate", "vin", "firstRegistrationDate", "usageType", "ncd"),
                    List.of("COMPULSORY", "VEHICLE_DAMAGE", "THIRD_PARTY_LIABILITY", "PASSENGER"), "UW-AUTO"),
            definition(InsuranceProductType.ENTERPRISE_PROPERTY, SubjectType.PROPERTY,
                    ProductEnum.IssuanceMode.THREE_STEP, PricingMode.ACTUARIAL_FORMULA,
                    List.of("propertyAddress", "propertyUsage", "buildingStructure", "fireProtectionLevel"),
                    List.of("BUILDING", "MACHINERY", "INVENTORY", "BUSINESS_INTERRUPTION"), "UW-PROPERTY-SURVEY"),
            definition(InsuranceProductType.HOUSEHOLD_PROPERTY, SubjectType.PROPERTY,
                    ProductEnum.IssuanceMode.TWO_STEP, PricingMode.RATE_TABLE,
                    List.of("propertyAddress", "buildingStructure", "occupancyType"),
                    List.of("BUILDING", "CONTENTS", "HOUSEHOLD_LIABILITY"), "UW-HOUSEHOLD"),
            definition(InsuranceProductType.EMPLOYER_LIABILITY, SubjectType.ORGANIZATION,
                    ProductEnum.IssuanceMode.THREE_STEP, PricingMode.RATE_TABLE,
                    List.of("industry", "employeeCount", "payrollAmount", "workplaceAddress"),
                    List.of("WORK_INJURY", "OCCUPATIONAL_DISEASE", "LEGAL_COST"), "UW-EMPLOYER-LIABILITY"));

    public List<InsuranceProductDefinitionResponse> list() {
        return definitions;
    }

    private static InsuranceProductDefinitionResponse definition(InsuranceProductType type, SubjectType subjectType,
                                                                 ProductEnum.IssuanceMode issuanceMode,
                                                                 PricingMode pricingMode, List<String> fields,
                                                                 List<String> coverages, String underwritingRuleSet) {
        return new InsuranceProductDefinitionResponse(type, subjectType, issuanceMode, pricingMode, fields, coverages,
                underwritingRuleSet);
    }
}
