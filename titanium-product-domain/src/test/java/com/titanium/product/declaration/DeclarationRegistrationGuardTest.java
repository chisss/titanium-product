package com.titanium.product.declaration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.Reserved;
import com.titanium.metadata.enums.insurance.InsuranceCapabilityBit;
import com.titanium.metadata.enums.insurance.InsuranceMandateType;
import com.titanium.metadata.enums.insurance.InsuranceTypeCapability;
import com.titanium.metadata.enums.underwriting.UnderwritingInputType;
import com.titanium.product.common.enums.LiabilityStructure;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.valueobject.pricing.pricing.ActuarialBasis;

/**
 * 「声明即登记」跨域守卫（G10/g10-06）：三处「声明了但无触发链路」的定性固化。
 * <p>
 * 守的是各处的<b>结构面</b>——标注存在、登记存在、边界未被越界：
 * <ol>
 *   <li>{@link UnderwritingInputType}：6 值当前全部零触达（数据由 underwriting 域四个平行值对象承载，
 *       不经本枚举），须逐个标 {@link Reserved}；</li>
 *   <li>{@link LiabilityStructure}：<b>业务预留面恰</b> {@code PACKAGE} 与 {@code MODULAR} 标 {@link Reserved}
 *       （G11/g11-06 定性：SINGLE/MAIN_ADDITIONAL 为已接入值，业务预留面多标即误标；
 *       SINGLE 另有 g16-05 零引用登记标注，属另一族，见 {@link #isBusinessReservation}）；</li>
 *   <li>{@code "COMPULSORY"}：裸字符串（险别而非责任编码，注解贴不上），
 *       g11-04 迁移后由 {@link InsuranceMandateType} 唯一承载（落点由 AUTO 默认责任清单迁至枚举文件），
 *       定性登记仍落在 {@link InsuranceCapabilityBit#DEFAULT_COVERAGE_CODES} 的 consumers 上。</li>
 *   <li>（G12/g12-07 增补）{@link LifeProductSpec} 与 {@link ActuarialBasis} 的指定 record 组件：
 *       前者 {@code entryAgeRange}（准入判定零触达）、后者 {@code mortalityTableRef}（计算侧零消费），
 *       各自<b>恰</b>该组件标 {@link Reserved}（同对象其余组件多标即误标）。</li>
 * </ol>
 * </p>
 * <p>
 * 🔴 <b>「触达」那一面不在本测试</b>（静态消费方扫描 + 字面量唯一性）——归验收脚本
 * {@code ~/.g10-snapshots/validate-g10-06.sh}（② 的触达面复核随 g11-06 并入
 * {@code ~/.g11-snapshots/validate-g11-06.sh}），两侧都要能判红。只跑本测试时，
 * 把 {@code @Reserved} 从 PACKAGE/MODULAR 挪到任意别的值上照样全绿，这正是
 * 「结构对得上、事实对不上」的同形判据陷阱。
 * </p>
 * <p>
 * 落点在 product-domain：它是本仓唯一能<b>同时</b>读到 metadata（①③ 的载体）与
 * product-common（② 的枚举所在）的测试基座——product-common 无 test 目录与 junit 依赖，
 * 而 metadata 反向依赖 product-common 会破坏模块分层。
 * </p>
 *
 * @author wei.sun
 * @since 2026/10/9
 */
class DeclarationRegistrationGuardTest {

    @Test
    @DisplayName("① UnderwritingInputType 全部 6 值须逐个标 @Reserved（漏标判红；增删值须同批更新基数）")
    void shouldReserveAllUnderwritingInputTypes() {
        assertEquals(6, UnderwritingInputType.values().length,
                "基数=2026-10-09 核对时点值；若确需增删值，同批更新基数并重新核对全部值的触达结论");

        for (UnderwritingInputType value : UnderwritingInputType.values()) {
            Reserved reserved = reservedOf(UnderwritingInputType.class, value.name());
            assertNotNull(reserved, value + " 零触达却未标 @Reserved（漏登）");
            assertFalse(reserved.value().isBlank(), value + " 的 @Reserved 须写明核对时点与口径");
        }
    }

    @Test
    @DisplayName("② LiabilityStructure 业务预留面恰 PACKAGE+MODULAR 被标 @Reserved（SINGLE/MAIN_ADDITIONAL 已接入，多标即误标）")
    void shouldReserveExactlyPackageAndModular() {
        assertEquals(4, LiabilityStructure.values().length,
                "基数=2026-10-09 核对时点值；增删值须同批重新定性");

        List<LiabilityStructure> reserved = new ArrayList<>();
        for (LiabilityStructure value : LiabilityStructure.values()) {
            if (isBusinessReservation(reservedOf(LiabilityStructure.class, value.name()))) {
                reserved.add(value);
            }
        }

        assertEquals(List.of(LiabilityStructure.PACKAGE, LiabilityStructure.MODULAR), reserved,
                "G11/g11-06 定性：PACKAGE/MODULAR 零触达标 @Reserved（业务预留面）；SINGLE/MAIN_ADDITIONAL"
                        + "有生产构值与数据承载（已接入），业务预留面多标即误标（SINGLE 上的 g16-05 零引用"
                        + "登记标注属另一族，见 isBusinessReservation 说明）");
        for (LiabilityStructure value : reserved) {
            assertFalse(reservedOf(LiabilityStructure.class, value.name()).value().isBlank(),
                    value + " 的 @Reserved 须写明核对时点、口径与边界");
        }
    }

    /**
     * 业务预留族过滤：{@link Reserved} 现含两个语义族，本测试只断言<b>业务预留族</b>
     * （g10~g15 人工逐值核对的定性产物，note 为逐值核对文案）。
     * <p>
     * 另一族为 <b>g16-05 零引用登记</b>（机械扫描「常量名零出现」的批量产物，登记载体
     * {@code docs/技术文档/枚举零引用清单.md}，双向对账由 titanium-common 的
     * {@code ZeroReferenceEnumGuardTest} 守护）——其 note 恒含固定前缀「g16-05 零引用登记」
     * （四个变体共享：枚举级消费/未见消费/错误码表全量/参考数据），据此区分两族。
     * 零引用族的增删不属本测试断言面（否则每次全域机械扫描都会打红本守卫），但改动/归一化
     * 该前缀文案会（前缀是两族判据，改动须同批复核本族守卫测试）。
     * </p>
     */
    private static boolean isBusinessReservation(Reserved reserved) {
        return reserved != null && !reserved.value().contains("g16-05 零引用登记");
    }

    @Test
    @DisplayName("③ 险别 COMPULSORY：已迁出 AUTO 默认责任清单，由 InsuranceMandateType 唯一承载 + 登记册保留 g11-04")
    void compulsoryMustStayRegistered() {
        // g10-06 移交定案（g11-04 落地）：COMPULSORY 是险别而非险种、也非责任编码，属「值塞错载体」。
        // 已从 AUTO 默认责任清单迁出、由 InsuranceMandateType 承载 ⇒「全仓带引号字面量恰一处」不变量
        // 保持成立，仅落点由权威表迁至枚举文件；交强+商业同单走「多主险段 + 险别」而不经本清单。
        assertFalse(InsuranceTypeCapability.AUTO.getDefaultCoverageCodes().contains("COMPULSORY"),
                "COMPULSORY 是险别而非责任编码，不得回流 AUTO 默认责任清单（回流即两个字面量承载位，唯一性不变量破）");

        assertEquals("COMPULSORY", InsuranceMandateType.COMPULSORY.getCode(),
                "字面量的唯一承载位：InsuranceMandateType.COMPULSORY 的 code；改动须与验收脚本字面量扫描同批更新");

        assertTrue(InsuranceCapabilityBit.DEFAULT_COVERAGE_CODES.getConsumers().stream()
                        .anyMatch(consumer -> consumer.contains("g11-04") && consumer.contains("COMPULSORY")),
                "COMPULSORY 是险别而非责任编码，注解贴不上裸字符串，迁移后的定性登记须保留在登记册 consumers 上");
    }

    @Test
    @DisplayName("④ @Reserved 契约：RUNTIME 保留 + 允许面恰 FIELD/METHOD（扩到 TYPE 则值级粒度失效）")
    void reservedAnnotationContract() {
        Retention retention = Reserved.class.getAnnotation(Retention.class);
        Target target = Reserved.class.getAnnotation(Target.class);

        assertNotNull(retention, "@Reserved 未标 @Retention（源码级注解不进字节码，判据只能退化成 grep 源码文本）");
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertNotNull(target, "@Reserved 未标 @Target");
        assertEquals(Set.of(ElementType.FIELD, ElementType.METHOD), Set.of(target.value()),
                "标注粒度必须是值（枚举常量/record 组件在字节码里即 FIELD）或契约方法"
                        + "（METHOD，G16/g16-04 契约方法零消费者面，见 Reserved javadoc）；"
                        + "扩到 TYPE 会让粒度退化为全有或全无");
    }

    @Test
    @DisplayName("⑤ LifeProductSpec/ActuarialBasis 恰指定组件标 @Reserved（G12/g12-07；其余组件多标即误标）")
    void shouldReserveExactlyDesignatedRecordComponents() {
        assertReservedComponent(LifeProductSpec.class, "entryAgeRange");
        assertReservedComponent(ActuarialBasis.class, "mortalityTableRef");
    }

    /**
     * 断言 {@code type} 的 record 组件中<b>恰</b> {@code reservedComponent} 标 {@link Reserved}，
     * 且 value 写明 g12-07 核对来源（同对象其余组件多标即误标）。
     */
    private void assertReservedComponent(Class<?> type, String reservedComponent) {
        List<String> reserved = new ArrayList<>();
        for (RecordComponent component : type.getRecordComponents()) {
            if (reservedOfField(type, component.getName()) != null) {
                reserved.add(component.getName());
            }
        }
        assertEquals(List.of(reservedComponent), reserved,
                type.getSimpleName() + " 恰 " + reservedComponent + " 应标 @Reserved（g12-07 定性），多标即误标");

        Reserved annotation = reservedOfField(type, reservedComponent);
        assertNotNull(annotation);
        assertFalse(annotation.value().isBlank(),
                type.getSimpleName() + "." + reservedComponent + " 的 @Reserved 须写明核对时点、口径与触发条件");
        assertTrue(annotation.value().contains("g12-07"),
                "value 须写明核对来源（g12-07），当前: " + annotation.value());
    }

    /** 读 record 组件上的 {@link Reserved}（组件注解随编译器落在同名实例字段，JLS 8.10.3） */
    private Reserved reservedOfField(Class<?> type, String fieldName) {
        try {
            Annotation annotation = type.getDeclaredField(fieldName).getAnnotation(Reserved.class);
            return annotation instanceof Reserved reserved ? reserved : null;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("record 组件在字节码里不是字段，判据口径失效: "
                    + type.getSimpleName() + "." + fieldName, e);
        }
    }

    /** 读枚举常量上的 {@link Reserved}（枚举常量在字节码里即字段，故按字段取） */
    private Reserved reservedOf(Class<? extends Enum<?>> enumClass, String constantName) {
        try {
            Annotation annotation = enumClass.getField(constantName).getAnnotation(Reserved.class);
            return annotation instanceof Reserved reserved ? reserved : null;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                    "枚举常量在字节码里不是字段，判据口径失效: " + enumClass.getSimpleName() + "." + constantName, e);
        }
    }
}
