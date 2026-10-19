package com.titanium.product.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.insurance.SubjectType;
import com.titanium.metadata.enums.product.LiabilityStructure;
import com.titanium.product.query.repository.ProductTemplateViewRepository;
import com.titanium.product.query.repository.ProductViewRepository;
import com.titanium.product.query.service.impl.ProductTemplateQueryServiceImpl;
import com.titanium.product.query.view.ProductTemplateView;
import com.titanium.product.valueobject.config.PolicyStructureConfig;

/**
 * 读侧保单结构解析守卫（G11/g11-06，AC-08 第三条）：非法 {@code liabilityStructure} 值的降级形态必须确定且构建期可见。
 * <p>
 * <b>背景（R-6 实测，fastjson2 2.0.45）</b>：读侧 {@code ProductTemplateQueryServiceImpl.parse} 只对
 * <b>抛异常</b>的 JSON 走 try-catch 兜底（整体置 null）；而非法枚举名（如历史数据中的 "MULTIPLE"）
 * <b>不抛异常</b>——fastjson2 静默把该字段置 null、对象其余字段完好。该降级此前无任何测试覆盖，
 * 等于「脏数据静默变 null」无人守护；本测试把这一降级形态<b>固化为构建期契约</b>。
 * </p>
 * <p>
 * 🔴 <b>锁定的是当前行为契约，不是「应该这样」</b>：若未来升级 fastjson2 或改 parse 实现，使非法值改为
 * 「抛异常 → 整体 null」或「显式拒绝」，本测试会判红——那是提示「降级形态已变」，须同步评估上游（policy
 * {@code ProductServiceAdapter} 读 {@code allowMultipleSubjects} 时装的是结构化配置）影响后再更新契约，
 * 不得直接改断言了事。
 * </p>
 *
 * @author wei.sun
 * @since 2026/10/10
 */
class PolicyStructureParsingGuardTest {

    private static final String TEMPLATE_ID = "STD-TPL-AUTO";
    private static final String TENANT_ID = "1";

    @Test
    @DisplayName("合法枚举名正确解析：MAIN_ADDITIONAL（g11-06 修正后的真值）")
    void shouldParseLegalValue() {
        PolicyStructureConfig structure = policyStructureOf(structureJson("MAIN_ADDITIONAL"));

        assertNotNull(structure);
        assertEquals(LiabilityStructure.MAIN_ADDITIONAL, structure.liabilityStructure());
        assertEquals(SubjectType.VEHICLE, structure.subjectType());
        assertTrue(structure.allowMultipleSubjects());
    }

    @Test
    @DisplayName("非法枚举名 MULTIPLE：确定降级为「整体非 null + liabilityStructure 单字段 null + 其余字段完好」")
    void shouldDegradeToNullFieldForIllegalValue() {
        PolicyStructureConfig structure = policyStructureOf(structureJson("MULTIPLE"));

        assertNotNull(structure, "字段级 null 降级不抛异常 ⇒ parse 的 try-catch 不触发 ⇒ 整体不得为 null");
        assertNull(structure.liabilityStructure(), "非法枚举名降级为该字段 null（fastjson2 2.0.45 行为，非异常）");
        // 其余 5 字段完好——降级只波及单字段，不污染整个配置对象
        assertEquals(SubjectType.VEHICLE, structure.subjectType());
        assertEquals("{}", structure.subjectFieldsSchema());
        assertTrue(structure.allowMultipleSubjects());
        assertEquals(2, structure.partyRoles().size());
        assertEquals(1, structure.requiredPartyRoles().size());
    }

    @Test
    @DisplayName("大小写不敏感：小写 main_additional 宽容解析为 MAIN_ADDITIONAL（固化 fastjson2 行为）")
    void shouldParseCaseInsensitively() {
        PolicyStructureConfig structure = policyStructureOf(structureJson("main_additional"));

        assertNotNull(structure);
        assertEquals(LiabilityStructure.MAIN_ADDITIONAL, structure.liabilityStructure());
    }

    @Test
    @DisplayName("非名称输入（数字 5）：同走单字段 null 降级，不抛异常、整体非 null")
    void shouldDegradeToNullFieldForNumericValue() {
        PolicyStructureConfig structure = policyStructureOf(structureJson("5"));

        assertNotNull(structure);
        assertNull(structure.liabilityStructure());
        assertEquals(SubjectType.VEHICLE, structure.subjectType());
    }

    @Test
    @DisplayName("空/空白 JSON：整配置为 null（与单字段降级形态不同——来源不同，调用方须区分处理）")
    void shouldReturnNullConfigForBlankJson() {
        assertNull(policyStructureOf(null), "null JSON ⇒ 整配置 null");
        assertNull(policyStructureOf("   "), "空白 JSON ⇒ 整配置 null");
    }

    /** 走真实 getTemplateById 链路取 policyStructure（读模型 JSON → parse 反序列化） */
    private ProductTemplateQueryServiceImpl queryService(String policyStructureJson) {
        ProductTemplateView view = new ProductTemplateView();
        view.setTemplateId(TEMPLATE_ID);
        view.setTenantId(TENANT_ID);
        view.setPolicyStructureJson(policyStructureJson);

        ProductTemplateViewRepository templateRepository = mock(ProductTemplateViewRepository.class);
        when(templateRepository.findByTemplateIdAndTenantId(TEMPLATE_ID, TENANT_ID)).thenReturn(Optional.of(view));
        return new ProductTemplateQueryServiceImpl(templateRepository, mock(ProductViewRepository.class));
    }

    /** 取查询结果的保单结构配置（经真实查询链路，非直接调 parse） */
    private PolicyStructureConfig policyStructureOf(String policyStructureJson) {
        var result = queryService(policyStructureJson).getTemplateById(TEMPLATE_ID, TENANT_ID);
        assertNotNull(result, "模板存在时查询结果不得为 null");
        return result.getPolicyStructure();
    }

    /** 含 liabilityStructure 的完整保单结构 JSON（其余 5 字段固定为可断言的已知值） */
    private static String structureJson(String liabilityStructure) {
        return "{\"subjectType\":\"VEHICLE\",\"subjectFieldsSchema\":\"{}\",\"allowMultipleSubjects\":true,"
                + "\"partyRoles\":[\"POLICY_HOLDER\",\"INSURED\"],\"requiredPartyRoles\":[\"POLICY_HOLDER\"],"
                + "\"liabilityStructure\":\"" + liabilityStructure + "\"}";
    }
}
