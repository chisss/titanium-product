package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.infrastructure.pricing.entity.premium.RateTableDO;
import com.titanium.product.infrastructure.pricing.entity.premium.RateTableRowDO;
import com.titanium.product.pricing.aggregate.RateTableDefinition;
import com.titanium.product.valueobject.rate.RateTableRow;

/**
 * 费率表领域对象 → 持久化对象声明式映射。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface RateTablePersistenceMapper {

    /** 费率表主表映射：维度键 JSON 序列化落库，行数冗余列由领域对象计算。 */
    @Mapping(target = "dimensionKeysJson", source = "dimensionKeys", qualifiedByName = "toJson")
    @Mapping(target = "rowCount", expression = "java(rateTable.rows().size())")
    RateTableDO toDO(RateTableDefinition rateTable);

    /** 费率行映射：租户ID/表ID沿用主表，dimensionHash 由领域计算方法生成，其余同名字段自动映射。 */
    @Mapping(target = "tenantId", source = "rateTable.tenantId")
    @Mapping(target = "tableId", source = "rateTable.tableId")
    @Mapping(target = "dimensionHash", expression = "java(row.dimensionHash())")
    RateTableRowDO toDO(RateTableDefinition rateTable, RateTableRow row);

    /**
     * 费率行反向映射：持久化对象 → 领域行。
     * <p>
     * 🔴 唯一实现——此前两个适配器各自手写一遍 18 分量的记录构造，两处都错一格就静默错位。
     * </p>
     * <p>
     * {@code dimensionHash} <b>不从库列回填</b>：领域记录在紧凑构造器里依维度重算，库列仅作
     * 唯一约束与迁移校验用；库列与重算值不一致时以领域侧为准（同名字段自动映射，其余列忽略）。
     * </p>
     */
    RateTableRow toDomain(RateTableRowDO row);

    /** 任意对象 → JSON 字符串。 */
    @Named("toJson")
    default String toJson(Object value) {
        return JSON.toJSONString(value);
    }
}
