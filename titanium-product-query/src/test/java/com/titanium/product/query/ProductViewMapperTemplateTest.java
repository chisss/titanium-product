package com.titanium.product.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.insurance.SubjectType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.common.enums.LiabilityStructure;
import com.titanium.product.event.ProductTemplateCreatedEvent;
import com.titanium.product.query.mapper.ProductViewMapper;
import com.titanium.product.query.mapper.ProductViewMapperImpl;
import com.titanium.product.query.view.ProductTemplateView;
import com.titanium.product.valueobject.config.ClaimConfig;
import com.titanium.product.valueobject.config.PolicyFormConfig;
import com.titanium.product.valueobject.config.PolicyStructureConfig;

/**
 * 产品模板创建事件投影映射测试（覆盖 GAP C 回归 + dev-506c 创建期承载标的结构）
 * <p>
 * 验证 {@link ProductViewMapper#applyTemplateCreated}：历史缺陷曾将 {@code policyFormConfig}
 * 误序列化进 {@code policyStructureJson} 列，导致读侧按 {@code PolicyStructureConfig}
 * 反序列化时类型冲突。dev-506c 起创建事件新增承载 {@code policyStructure}（可空），映射器应：
 * 事件未携带时保持列为空（向后兼容旧事件），携带时整体序列化写入。
 * 此测试固化修复，防止回归。同时校验创建期正常字段（理赔配置）仍被序列化。
 * </p>
 */
class ProductViewMapperTemplateTest {

    // ProductViewMapper 为 componentModel="spring"，无 SPI 注册，Mappers.getMapper 不适用；
    // 单元测试直接实例化生成的实现类（含公有无参构造），脱离 Spring 容器验证纯映射逻辑
    private final ProductViewMapper mapper = new ProductViewMapperImpl();

    @Test
    @DisplayName("模板创建事件投影：未携带标的结构时不写 policy_structure_json（GAP C），但正常序列化理赔配置")
    void shouldNotWritePolicyStructureJsonOnCreateWithoutStructure() {
        PolicyFormConfig policyFormConfig = new PolicyFormConfig(ProductEnum.PolicyFormType.INDIVIDUAL, false, null,
                null, false, List.of(), false, null);
        ClaimConfig claimConfig =
                new ClaimConfig(List.of("REPORT", "SURVEY"), 30, 90, "CLAIM_RULE_SET", List.of("ID_CARD"));
        // 静态工厂 of 不承载 policyStructure（兼容旧语义），映射器须保持 policy_structure_json 为空
        ProductTemplateCreatedEvent event = ProductTemplateCreatedEvent.of("TPL-001", "CODE-001", "健康险模板",
                InsuranceType.MEDICAL, "desc", null, null, claimConfig, null, policyFormConfig, null, List.of(),
                List.of(), "tenant-1", "creator-1");

        ProductTemplateView view = new ProductTemplateView();
        mapper.applyTemplateCreated(view, event);

        // GAP C：创建事件未携带 PolicyStructureConfig 时，映射器须保持 policy_structure_json 为空
        assertNull(view.getPolicyStructureJson(), "创建事件未携带标的结构时不应写入 policy_structure_json");
        // 回归对照：正常配置字段仍被整体序列化
        assertNotNull(view.getClaimConfigJson(), "理赔配置应被序列化");
        ClaimConfig parsed = JSON.parseObject(view.getClaimConfigJson(), ClaimConfig.class);
        assertEquals(30, parsed.reportDeadlineDays());
        assertEquals("CLAIM_RULE_SET", parsed.claimRuleSet());
        // G10/g10-02 双写：MEDICAL 属「同名异体系」唯一映射——权威列写三级 MEDICAL，遗留列原样保留扁平码
        assertEquals(InsuranceProductType.MEDICAL, view.getInsuranceType());
        assertEquals(InsuranceType.MEDICAL, view.getLegacyInsuranceType());
    }

    @Test
    @DisplayName("模板创建事件投影：携带标的结构时序列化写入 policy_structure_json（dev-506c）")
    void shouldWritePolicyStructureJsonOnCreateWithStructure() {
        PolicyStructureConfig policyStructure = new PolicyStructureConfig(SubjectType.PET,
                "{\"type\":\"object\",\"properties\":{\"breed\":{\"type\":\"string\"}}}",
                true, List.of("POLICY_HOLDER", "INSURED"), List.of("POLICY_HOLDER", "INSURED"),
                LiabilityStructure.MAIN_ADDITIONAL);
        ProductTemplateCreatedEvent event = new ProductTemplateCreatedEvent("TPL-002", "CODE-002", "宠物医疗险模板",
                InsuranceType.PET, "desc", null, null, null, null, null, null, List.of(), List.of(),
                com.titanium.metadata.enums.CommonStatus.ACTIVE, "tenant-1", "creator-1", null, policyStructure);

        ProductTemplateView view = new ProductTemplateView();
        mapper.applyTemplateCreated(view, event);

        assertNotNull(view.getPolicyStructureJson(), "创建事件携带标的结构时应序列化写入 policy_structure_json");
        PolicyStructureConfig parsed = JSON.parseObject(view.getPolicyStructureJson(), PolicyStructureConfig.class);
        assertEquals(SubjectType.PET, parsed.subjectType());
        assertEquals(true, parsed.allowMultipleSubjects());
        // G10/g10-02 双写：PET 映射到三级 PET_MEDICAL（异名同义）
        assertEquals(InsuranceProductType.PET_MEDICAL, view.getInsuranceType());
        assertEquals(InsuranceType.PET, view.getLegacyInsuranceType());
    }

    @Test
    @DisplayName("模板创建事件投影：无对映遗留值不猜值——权威列为空、遗留列原样（AC-02 反向契约）")
    void shouldNotGuessAuthoritativeTypeForUnmappedLegacy() {
        // VESSEL 属 6 个「无对映」值之一（三级体系无编码），双写时权威列必须留空
        ProductTemplateCreatedEvent event = ProductTemplateCreatedEvent.of("TPL-003", "CODE-003", "船舶险模板",
                InsuranceType.VESSEL, "desc", null, null, null, null, null, null, List.of(), List.of(), "tenant-1",
                "creator-1");

        ProductTemplateView view = new ProductTemplateView();
        mapper.applyTemplateCreated(view, event);

        assertNull(view.getInsuranceType(), "无对映遗留值不得猜值映射出三级码");
        assertEquals(InsuranceType.VESSEL, view.getLegacyInsuranceType(), "遗留列须原样保留");
    }

    @Test
    @DisplayName("模板创建事件投影：一对多遗留值不猜值——权威列为空、遗留列原样（AC-02 反向契约）")
    void shouldNotGuessAuthoritativeTypeForOneToManyLegacy() {
        // LIFE 属「一对多」值（三级展开 4 个候选：TERM_LIFE/WHOLE_LIFE/ENDOWMENT/ANNUITY），归属未定前不猜
        ProductTemplateCreatedEvent event = ProductTemplateCreatedEvent.of("TPL-004", "CODE-004", "寿险模板",
                InsuranceType.LIFE, "desc", null, null, null, null, null, null, List.of(), List.of(), "tenant-1",
                "creator-1");

        ProductTemplateView view = new ProductTemplateView();
        mapper.applyTemplateCreated(view, event);

        assertNull(view.getInsuranceType(), "一对多遗留值不得猜值映射出三级码");
        assertEquals(InsuranceType.LIFE, view.getLegacyInsuranceType(), "遗留列须原样保留");
    }
}
