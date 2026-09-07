package com.titanium.product.application.init;

import java.util.List;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.insurance.SubjectType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.command.CreateProductTemplateCommand;
import com.titanium.product.common.enums.LiabilityStructure;
import com.titanium.product.valueobject.config.IssuanceProcessConfig;
import com.titanium.product.valueobject.config.PolicyStructureConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品模板数据初始化器（dev-506c 宠物医疗险模板种子）
 * <p>
 * 启动时经命令网关建立宠物医疗险（PET_MEDICAL）标准产品模板，走聚合命令 → 事件 → 投影入读模型
 * （t_product_template_view），保证读写模型同源、聚合可重建。模板承载保单结构配置
 * （标的类型 PET / 标的字段 Schema / 多标的 / 参与方角色），供创建 PET_MEDICAL 产品时引用对齐。
 * 置于 application 层（发命令编排属应用层职责，ArchUnit 禁止 infrastructure 持有 CommandGateway）。
 * </p>
 * <p>
 * 幂等：固定 templateId，启动时先查<b>写侧事件存储</b>（非读模型——读模型由 TrackingEventProcessor
 * 异步投影，启动时可能尚未追平，查读模型会误判不存在而重复发命令触发 "Cannot reuse aggregate
 * identifier"），已存在则跳过，避免重启重复建。平台级公共模板租户为 {@code default}。
 * </p>
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class ProductTemplateDataInitializer implements ApplicationRunner {

    /** 平台级公共模板租户 */
    private static final String TENANT = "default";
    private static final String OPERATOR = "system";

    /** 宠物医疗险模板固定聚合ID（幂等锚点） */
    private static final String PET_MEDICAL_TEMPLATE_ID = "seed_template_pet_medical";

    /**
     * 宠物标的必填字段 Schema（JSON Schema）：
     * breed 品种 / petAge 宠物年龄 / vaccinationRecords 免疫记录 / sterilization 绝育
     */
    private static final String PET_SUBJECT_FIELDS_SCHEMA = """
            {"type":"object","properties":{
              "breed":{"type":"string","description":"宠物品种(猫/犬等)"},
              "petAge":{"type":"integer","description":"宠物年龄(岁)"},
              "vaccinationRecords":{"type":"object","description":"免疫记录"},
              "sterilization":{"type":"boolean","description":"是否已绝育"}
            },"required":["breed","petAge"]}""";

    private final CommandGateway commandGateway;
    private final EventStore     eventStore;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedPetMedicalTemplate();
        } catch (RuntimeException ex) {
            // 种子失败不阻断服务启动（如数据库暂不可用），告警留痕待下次启动重试
            log.warn("[模板种子] 宠物医疗险模板初始化失败（服务照常启动，下次启动重试）: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 建立宠物医疗险模板：两步出单（投保→保单，可跳核保），多宠物投保，
     * 标的字段为品种/年龄/免疫记录/绝育，参与方角色为投保人+被保险人（宠物）。
     */
    private void seedPetMedicalTemplate() {
        if (exists(PET_MEDICAL_TEMPLATE_ID)) {
            return;
        }
        IssuanceProcessConfig issuanceProcessConfig = new IssuanceProcessConfig(
                ProductEnum.IssuanceMode.TWO_STEP,
                List.of(ProductEnum.IssuanceStep.APPLICATION_SUBMIT, ProductEnum.IssuanceStep.POLICY_ISSUE),
                false, true, false, 30, List.of());
        PolicyStructureConfig policyStructure = new PolicyStructureConfig(
                SubjectType.PET,
                PET_SUBJECT_FIELDS_SCHEMA,
                true,
                List.of("POLICY_HOLDER", "INSURED"),
                List.of("POLICY_HOLDER", "INSURED"),
                LiabilityStructure.MAIN_ADDITIONAL);

        commandGateway.sendAndWait(new CreateProductTemplateCommand(
                PET_MEDICAL_TEMPLATE_ID,
                "PET_MEDICAL_TEMPLATE",
                "宠物医疗保险模板",
                InsuranceType.PET,
                "宠物（猫、犬）医疗保险标准模板：两步出单、多宠物投保、品种/年龄/免疫记录/绝育标的字段",
                issuanceProcessConfig,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                TENANT,
                OPERATOR,
                policyStructure));
        log.info("[模板种子] 宠物医疗险模板已初始化 templateId={}", PET_MEDICAL_TEMPLATE_ID);
    }

    /**
     * 判断种子模板聚合是否已存在（幂等）：查写侧事件存储，该聚合已有任何事件即视为存在。
     */
    private boolean exists(String templateId) {
        return eventStore.readEvents(templateId).hasNext();
    }
}
