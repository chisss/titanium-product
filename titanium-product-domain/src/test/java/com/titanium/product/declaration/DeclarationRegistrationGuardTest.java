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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.Reserved;
import com.titanium.metadata.enums.insurance.InsuranceCapabilityBit;
import com.titanium.metadata.enums.insurance.InsuranceTypeCapability;
import com.titanium.metadata.enums.underwriting.UnderwritingInputType;
import com.titanium.product.common.enums.LiabilityStructure;

/**
 * 「声明即登记」跨域守卫（G10/g10-06）：三处「声明了但无触发链路」的定性固化。
 * <p>
 * 守的是三处的<b>结构面</b>——标注存在、登记存在、边界未被越界：
 * <ol>
 *   <li>{@link UnderwritingInputType}：6 值当前全部零触达（数据由 underwriting 域四个平行值对象承载，
 *       不经本枚举），须逐个标 {@link Reserved}；</li>
 *   <li>{@link LiabilityStructure}：<b>恰</b> {@code PACKAGE} 标 {@link Reserved}
 *       （其余 3 值的定性归 G11/AC-08，多标即越界）；</li>
 *   <li>{@code "COMPULSORY"}：裸字符串（险别而非责任编码，注解贴不上），
 *       登记落在 {@link InsuranceCapabilityBit#DEFAULT_COVERAGE_CODES} 的 consumers 上。</li>
 * </ol>
 * </p>
 * <p>
 * 🔴 <b>「触达」那一面不在本测试</b>（静态消费方扫描 + 字面量唯一性）——归验收脚本
 * {@code ~/.g10-snapshots/validate-g10-06.sh}，两侧都要能判红。只跑本测试时，
 * 把 {@code @Reserved} 从 PACKAGE 挪到任意别的值上照样全绿，这正是「结构对得上、
 * 事实对不上」的同形判据陷阱。
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
    @DisplayName("② LiabilityStructure 恰 PACKAGE 被标 @Reserved（多标=越界 G11/AC-08，漏标=登记被摘）")
    void shouldReserveExactlyPackage() {
        assertEquals(4, LiabilityStructure.values().length,
                "基数=2026-10-09 核对时点值；增删值须同批重新定性");

        List<LiabilityStructure> reserved = new ArrayList<>();
        for (LiabilityStructure value : LiabilityStructure.values()) {
            if (reservedOf(LiabilityStructure.class, value.name()) != null) {
                reserved.add(value);
            }
        }

        assertEquals(List.of(LiabilityStructure.PACKAGE), reserved,
                "本任务（g10-06）只处置 PACKAGE；其余 3 值的定性归 G11/AC-08，多标即越界");
        assertFalse(reservedOf(LiabilityStructure.class, "PACKAGE").value().isBlank(),
                "PACKAGE 的 @Reserved 须写明核对时点、裁决出处与边界");
    }

    @Test
    @DisplayName("③ 裸字符串 COMPULSORY：须在 AUTO 默认责任清单内 + 登记册已登记 g11-04")
    void compulsoryMustStayRegistered() {
        assertTrue(InsuranceTypeCapability.AUTO.getDefaultCoverageCodes().contains("COMPULSORY"),
                "COMPULSORY 的载体位置变更后，本断言与验收脚本的字面量扫描须同批更新");

        assertTrue(InsuranceCapabilityBit.DEFAULT_COVERAGE_CODES.getConsumers().stream()
                        .anyMatch(consumer -> consumer.contains("g11-04") && consumer.contains("COMPULSORY")),
                "COMPULSORY 是险别而非责任编码，注解贴不上裸字符串，登记须落在登记册 consumers 上");
    }

    @Test
    @DisplayName("④ @Reserved 契约：RUNTIME 保留 + 恰只允许 FIELD（扩到 TYPE 则值级粒度失效）")
    void reservedAnnotationContract() {
        Retention retention = Reserved.class.getAnnotation(Retention.class);
        Target target = Reserved.class.getAnnotation(Target.class);

        assertNotNull(retention, "@Reserved 未标 @Retention（源码级注解不进字节码，判据只能退化成 grep 源码文本）");
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertNotNull(target, "@Reserved 未标 @Target");
        assertEquals(List.of(ElementType.FIELD), List.of(target.value()),
                "标注粒度必须是值（枚举常量在字节码里即 FIELD）；扩到 TYPE 会让粒度退化为全有或全无");
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
