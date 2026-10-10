package com.titanium.product.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.titanium.product.valueobject.pricing.RateTableRowDraft;
import com.titanium.product.valueobject.rate.RateTableRow;
import com.titanium.product.web.dto.pricing.ratetable.RateTableRowDTO;
import com.titanium.product.web.dto.pricing.ratetable.RateTableRowVO;

/**
 * 费率行 Web 契约 ↔ 领域值对象映射。
 * <p>
 * 🔴 独立成映射器而非在 {@code ProductRateTableController} 内手写构造：费率行有 17 个业务字段，
 * 手写两处（DTO→Draft、Row→VO）就是 34 次取值、两处各错一格即静默错位。此处按字段名自动对齐，
 * 新增维度只需在两侧 record 加同名分量，映射器零改动（§3.4.12 ③：&gt;5 字段组装必须 MapStruct/Assembler）。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface RateTableRowWebMapper {

    /** Web 入参 → 应用层导入模型（字段同名，全自动映射）。 */
    RateTableRowDraft toDraft(RateTableRowDTO request);

    /**
     * 领域费率行 → Web 出参。
     * <p>
     * {@code dimensionHash} <b>不是</b>费率行的 record 分量（是依维度现算的方法），故显式绑定表达式；
     * 不显式声明时映射器不会报错，只会静默留空，前端拿到的去重摘要恒为 {@code null}。
     * </p>
     */
    @Mapping(target = "dimensionHash", expression = "java(row.dimensionHash())")
    RateTableRowVO toVO(RateTableRow row);
}
