package com.titanium.product.infrastructure.init;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.domain.command.ActivateProductTemplateCommand;
import com.titanium.product.domain.command.CreateProductTemplateCommand;
import com.titanium.product.domain.valueobject.*;
import com.titanium.product.infrastructure.repository.jpa.ProductTemplateJpaRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 产品模板数据初始化器
 * 在启动时预置6个险种产品模板
 */
@Slf4j
@Component
public class ProductTemplateDataInitializer implements ApplicationRunner {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private ProductTemplateJpaRepository jpaRepository;

    @Override
    public void run(ApplicationArguments args) {
        initTemplateIfAbsent("TPL_CAR_COMPREHENSIVE", this::createCarTemplate);
        initTemplateIfAbsent("TPL_CRITICAL_ILLNESS", this::createCriticalIllnessTemplate);
        initTemplateIfAbsent("TPL_ACCIDENT", this::createAccidentTemplate);
        initTemplateIfAbsent("TPL_PET", this::createPetTemplate);
        initTemplateIfAbsent("TPL_MEDICAL", this::createMedicalTemplate);
        initTemplateIfAbsent("TPL_PROPERTY", this::createPropertyTemplate);
    }

    private void initTemplateIfAbsent(String templateCode, Runnable creator) {
        if (!jpaRepository.existsByTemplateCode(templateCode)) {
            try {
                creator.run();
                log.info("预置产品模板 [{}] 创建成功", templateCode);
            } catch (Exception e) {
                log.warn("预置产品模板 [{}] 创建失败: {}", templateCode, e.getMessage());
            }
        } else {
            log.info("产品模板 [{}] 已存在，跳过初始化", templateCode);
        }
    }

    private String generateId() {
        return "TPL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** 车险模板：一步出单，免核保 */
    private void createCarTemplate() {
        String id = generateId();
        commandGateway.sendAndWait(new CreateProductTemplateCommand(
                id, "TPL_CAR_COMPREHENSIVE", "车险综合模板", "PROPERTY",
                InsuranceType.CAR, null,
                IssuanceMode.ONE_STEP,
                List.of(new PolicyStage("POLICY", "出单", List.of("APPLICANT", "VEHICLE_SUBJECT", "PRODUCT_LINE"),
                        "VALIDATE_CAR_POLICY", null, true)),
                new UnderwritingConfig(false, null, null, null),
                new PolicyStructureConfig(SubjectType.VEHICLE,
                        "{\"vin\":\"车架号\",\"plateNumber\":\"车牌号\",\"engineNumber\":\"发动机号\",\"vehicleModel\":\"车型\"}",
                        false, List.of("POLICY_HOLDER", "INSURED", "DRIVER"),
                        List.of("POLICY_HOLDER", "INSURED"), LiabilityStructure.MAIN_ADDITIONAL),
                new MaintenanceConfig(List.of("POLICY_TERMINATION", "POLICY_HOLDER_CHANGE"), 0, null, null),
                new ClaimConfig(List.of("REPORT", "SURVEY", "LOSS_ASSESS", "REVIEW", "PAYMENT"),
                        48, 0, "CLAIM_CAR_REVIEW", List.of("事故照片", "驾驶证", "行驶证")),
                new BillingConfig(List.of("LUMP_SUM"), 0, 0, false),
                null,
                "DEFAULT"
        ));
        commandGateway.sendAndWait(new ActivateProductTemplateCommand(id, "DEFAULT"));
    }

    /** 重疾险模板：三步出单，必须核保 */
    private void createCriticalIllnessTemplate() {
        String id = generateId();
        commandGateway.sendAndWait(new CreateProductTemplateCommand(
                id, "TPL_CRITICAL_ILLNESS", "重疾险模板", "HEALTH",
                InsuranceType.CRITICAL_ILLNESS, null,
                IssuanceMode.THREE_STEP,
                List.of(
                        new PolicyStage("PROPOSAL", "意向单", List.of("APPLICANT", "EXPECTED_PRODUCT"),
                                "VALIDATE_CI_PROPOSAL", "CONFIRMED", false),
                        new PolicyStage("INSURANCE", "投保单",
                                List.of("APPLICANT", "INSURED", "BENEFICIARY", "HEALTH_NOTICE", "PRODUCT_LINE"),
                                "VALIDATE_CI_INSURANCE", "UNDERWRITING_APPROVED", false),
                        new PolicyStage("POLICY", "保单", List.of("ALL_FROM_INSURANCE"),
                                "VALIDATE_CI_POLICY", null, true)
                ),
                new UnderwritingConfig(true, "UW_CRITICAL_ILLNESS_AUTO",
                        "UW_CRITICAL_ILLNESS_MANUAL", new BigDecimal("500000")),
                new PolicyStructureConfig(SubjectType.PERSON,
                        "{\"name\":\"姓名\",\"idCard\":\"身份证号\",\"birthDate\":\"出生日期\",\"healthNotice\":\"健康告知\"}",
                        false, List.of("POLICY_HOLDER", "INSURED", "BENEFICIARY"),
                        List.of("POLICY_HOLDER", "INSURED", "BENEFICIARY"), LiabilityStructure.MAIN_ADDITIONAL),
                new MaintenanceConfig(
                        List.of("POLICY_TERMINATION", "BENEFICIARY_CHANGE", "REDUCTION_PAYMENT",
                                "ADDITIONAL_PAYMENT", "POLICY_LOAN"),
                        15, "MT_CI_SURRENDER", null),
                new ClaimConfig(List.of("REPORT", "REVIEW", "ASSESSMENT", "PAYMENT"),
                        10, 180, "CLAIM_CI_REVIEW", List.of("诊断证明", "病理报告", "住院记录")),
                new BillingConfig(List.of("ANNUAL", "SEMI_ANNUAL", "QUARTERLY", "MONTHLY"), 60, 730, true),
                new ReinsuranceConfig(true, new BigDecimal("1000000"), null),
                "DEFAULT"
        ));
        commandGateway.sendAndWait(new ActivateProductTemplateCommand(id, "DEFAULT"));
    }

    /** 意外险模板：一步出单，免核保 */
    private void createAccidentTemplate() {
        String id = generateId();
        commandGateway.sendAndWait(new CreateProductTemplateCommand(
                id, "TPL_ACCIDENT", "意外险模板", "ACCIDENT",
                InsuranceType.ACCIDENT, null,
                IssuanceMode.ONE_STEP,
                List.of(new PolicyStage("POLICY", "出单",
                        List.of("APPLICANT", "INSURED", "PRODUCT_LINE"),
                        "VALIDATE_ACCIDENT_POLICY", null, true)),
                new UnderwritingConfig(false, null, null, null),
                new PolicyStructureConfig(SubjectType.PERSON,
                        "{\"name\":\"姓名\",\"idCard\":\"身份证号\",\"birthDate\":\"出生日期\"}",
                        false, List.of("POLICY_HOLDER", "INSURED"),
                        List.of("POLICY_HOLDER", "INSURED"), LiabilityStructure.SINGLE),
                new MaintenanceConfig(List.of("POLICY_TERMINATION"), 0, null, null),
                new ClaimConfig(List.of("REPORT", "REVIEW", "PAYMENT"),
                        30, 0, "CLAIM_ACCIDENT_REVIEW", List.of("事故证明", "医疗记录")),
                new BillingConfig(List.of("LUMP_SUM"), 0, 0, false),
                null,
                "DEFAULT"
        ));
        commandGateway.sendAndWait(new ActivateProductTemplateCommand(id, "DEFAULT"));
    }

    /** 宠物险模板：一步出单，免核保，等待期30天 */
    private void createPetTemplate() {
        String id = generateId();
        commandGateway.sendAndWait(new CreateProductTemplateCommand(
                id, "TPL_PET", "宠物险模板", "PROPERTY",
                InsuranceType.PET, null,
                IssuanceMode.ONE_STEP,
                List.of(new PolicyStage("POLICY", "出单",
                        List.of("APPLICANT", "PET_SUBJECT"),
                        "VALIDATE_PET_POLICY", null, true)),
                new UnderwritingConfig(false, null, null, null),
                new PolicyStructureConfig(SubjectType.PET,
                        "{\"petName\":\"宠物名\",\"breed\":\"品种\",\"age\":\"年龄\",\"chipId\":\"芯片号\",\"photo\":\"照片\"}",
                        false, List.of("POLICY_HOLDER"),
                        List.of("POLICY_HOLDER"), LiabilityStructure.SINGLE),
                new MaintenanceConfig(List.of("POLICY_TERMINATION"), 0, null, null),
                new ClaimConfig(List.of("REPORT", "REVIEW", "PAYMENT"),
                        30, 30, "CLAIM_PET_REVIEW", List.of("宠物诊疗记录", "费用清单")),
                new BillingConfig(List.of("ANNUAL", "MONTHLY"), 30, 60, true),
                null,
                "DEFAULT"
        ));
        commandGateway.sendAndWait(new ActivateProductTemplateCommand(id, "DEFAULT"));
    }

    /** 医疗险模板：两步出单，自动核保 */
    private void createMedicalTemplate() {
        String id = generateId();
        commandGateway.sendAndWait(new CreateProductTemplateCommand(
                id, "TPL_MEDICAL", "医疗险模板", "HEALTH",
                InsuranceType.MEDICAL, null,
                IssuanceMode.TWO_STEP,
                List.of(
                        new PolicyStage("INSURANCE", "投保单",
                                List.of("APPLICANT", "INSURED", "HEALTH_NOTICE", "PRODUCT_LINE"),
                                "VALIDATE_MEDICAL_INSURANCE", "UNDERWRITING_APPROVED", false),
                        new PolicyStage("POLICY", "保单", List.of("ALL_FROM_INSURANCE"),
                                null, null, true)
                ),
                new UnderwritingConfig(true, "UW_MEDICAL_AUTO", null, null),
                new PolicyStructureConfig(SubjectType.PERSON,
                        "{\"name\":\"姓名\",\"idCard\":\"身份证号\",\"birthDate\":\"出生日期\",\"healthNotice\":\"健康告知\"}",
                        false, List.of("POLICY_HOLDER", "INSURED"),
                        List.of("POLICY_HOLDER", "INSURED"), LiabilityStructure.MODULAR),
                new MaintenanceConfig(List.of("POLICY_TERMINATION", "RENEWAL"), 15, null, null),
                new ClaimConfig(List.of("REPORT", "DOC_REVIEW", "ASSESSMENT", "PAYMENT"),
                        30, 30, "CLAIM_MEDICAL_REVIEW", List.of("诊断证明", "费用清单", "病历")),
                new BillingConfig(List.of("ANNUAL", "MONTHLY"), 30, 60, true),
                null,
                "DEFAULT"
        ));
        commandGateway.sendAndWait(new ActivateProductTemplateCommand(id, "DEFAULT"));
    }

    /** 财产险模板：两步出单，查勘后核保 */
    private void createPropertyTemplate() {
        String id = generateId();
        commandGateway.sendAndWait(new CreateProductTemplateCommand(
                id, "TPL_PROPERTY", "财产险模板", "PROPERTY",
                InsuranceType.PROPERTY, null,
                IssuanceMode.TWO_STEP,
                List.of(
                        new PolicyStage("INSURANCE", "投保单",
                                List.of("APPLICANT", "PROPERTY_SUBJECT", "PRODUCT_LINE"),
                                "VALIDATE_PROPERTY_INSURANCE", "UNDERWRITING_APPROVED", false),
                        new PolicyStage("POLICY", "保单", List.of("ALL_FROM_INSURANCE"),
                                null, null, true)
                ),
                new UnderwritingConfig(true, "UW_PROPERTY_AUTO", "UW_PROPERTY_MANUAL", null),
                new PolicyStructureConfig(SubjectType.PROPERTY,
                        "{\"propertyType\":\"财产类型\",\"address\":\"地址\",\"area\":\"面积\",\"value\":\"估值\"}",
                        true, List.of("POLICY_HOLDER", "INSURED"),
                        List.of("POLICY_HOLDER"), LiabilityStructure.MAIN_ADDITIONAL),
                new MaintenanceConfig(List.of("POLICY_TERMINATION", "SUBJECT_CHANGE"), 0, null, null),
                new ClaimConfig(List.of("REPORT", "SURVEY", "LOSS_ASSESS", "REVIEW", "PAYMENT"),
                        48, 0, "CLAIM_PROPERTY_REVIEW", List.of("现场照片", "损失清单", "权属证明")),
                new BillingConfig(List.of("LUMP_SUM"), 0, 0, false),
                null,
                "DEFAULT"
        ));
        commandGateway.sendAndWait(new ActivateProductTemplateCommand(id, "DEFAULT"));
    }
}
