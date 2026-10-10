package com.titanium.product.valueobject.config;

import java.util.Objects;

import com.titanium.metadata.enums.underwriting.HealthDeclarationHandling;
import com.titanium.metadata.enums.underwriting.HealthDeclarationQuestion;

/**
 * 告知项配置值对象（G12/g12-02 AC-02）
 * <p>
 * 产品投保条件中的单个<b>自定义</b>告知项：问题编码（权威值域）+ 展示文案 + 是否必答 + 命中处置。
 * 产品经「告知项清单」（{@code InsureCondition.healthDeclarationItems}）声明本产品出单时
 * 需要核对哪些告知问题；<b>未配置清单的产品不校验任何自定义告知项</b>（防老产品被误拦）。
 * </p>
 * <p>
 * 🔴 <b>消费链</b>（出单受理校验，policy 域）：必答项（{@code required=true}）答案缺失即拒；
 * 处置为 {@link HealthDeclarationHandling#REJECT} 且答案命中布尔肯定（{@code "true"}）即拒。
 * 其余处置的执行随 R-6 接入（见枚举 javadoc）。
 * </p>
 * <p>
 * 🔴 <b>不承载既有平铺五项</b>（吸烟/身高/体重/既往病史/家族史）——那五项由出单 extendData
 * 平铺键与核保域 {@code HealthDeclaration} 固定组件承接，本清单只表达扩展自定义项。
 * </p>
 *
 * @param question 问题编码（权威值域，非空）
 * @param text     展示文案（可空，缺省由展示层回落枚举 desc）
 * @param required 是否必答（true 时答案缺失即拒绝出单）
 * @param handling 命中处置（非空；当前仅 REJECT 被执行）
 */
public record HealthDeclarationItem(HealthDeclarationQuestion question, String text, boolean required,
                                    HealthDeclarationHandling handling) {

    public HealthDeclarationItem {
        Objects.requireNonNull(question, "告知项问题编码不得为空");
        Objects.requireNonNull(handling, "告知项处置不得为空");
    }
}
