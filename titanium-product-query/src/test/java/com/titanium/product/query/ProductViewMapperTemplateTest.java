package com.titanium.product.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.event.ProductTemplateCreatedEvent;
import com.titanium.product.query.mapper.ProductViewMapper;
import com.titanium.product.query.mapper.ProductViewMapperImpl;
import com.titanium.product.query.view.ProductTemplateView;
import com.titanium.product.valueobject.ClaimConfig;
import com.titanium.product.valueobject.PolicyFormConfig;

/**
 * 产品模板创建事件投影映射测试（覆盖 GAP C 回归）
 * <p>
 * 验证 {@link ProductViewMapper#applyTemplateCreated}：创建事件承载 {@code policyFormConfig} 而非
 * {@code policyStructureConfig}，映射器<b>不得</b>把任何值写入 {@code policyStructureJson} 列
 * （该列专供 {@code UpdateProductTemplateCommand} 写入 {@code PolicyStructureConfig}）。
 * 历史缺陷曾将 {@code policyFormConfig} 误序列化进此列，导致读侧按 {@code PolicyStructureConfig}
 * 反序列化时类型冲突。此测试固化修复，防止回归。同时校验创建期正常字段（理赔配置）仍被序列化。
 * </p>
 */
class ProductViewMapperTemplateTest {

    // ProductViewMapper 为 componentModel="spring"，无 SPI 注册，Mappers.getMapper 不适用；
    // 单元测试直接实例化生成的实现类（含公有无参构造），脱离 Spring 容器验证纯映射逻辑
    private final ProductViewMapper mapper = new ProductViewMapperImpl();

    @Test
    @DisplayName("模板创建事件投影：不写 policy_structure_json（GAP C），但正常序列化理赔配置")
    void shouldNotWritePolicyStructureJsonOnCreate() {
        PolicyFormConfig policyFormConfig = new PolicyFormConfig(ProductEnum.PolicyFormType.INDIVIDUAL, false, null,
                null, false, List.of(), false, null);
        ClaimConfig claimConfig =
                new ClaimConfig(List.of("REPORT", "SURVEY"), 30, 90, "CLAIM_RULE_SET", List.of("ID_CARD"));
        ProductTemplateCreatedEvent event = ProductTemplateCreatedEvent.of("TPL-001", "CODE-001", "健康险模板",
                InsuranceType.MEDICAL, "desc", null, null, claimConfig, null, policyFormConfig, null, List.of(),
                List.of(), "tenant-1", "creator-1");

        ProductTemplateView view = new ProductTemplateView();
        mapper.applyTemplateCreated(view, event);

        // GAP C：创建事件不承载 PolicyStructureConfig，映射器须保持 policy_structure_json 为空
        assertNull(view.getPolicyStructureJson(), "创建期不应写入 policy_structure_json");
        // 回归对照：正常配置字段仍被整体序列化
        assertNotNull(view.getClaimConfigJson(), "理赔配置应被序列化");
        ClaimConfig parsed = JSON.parseObject(view.getClaimConfigJson(), ClaimConfig.class);
        assertEquals(30, parsed.reportDeadlineDays());
        assertEquals("CLAIM_RULE_SET", parsed.claimRuleSet());
    }
}
