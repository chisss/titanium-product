package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.aggregate.RateTableDefinition;
import com.titanium.product.infrastructure.pricing.entity.RateTableDO;
import com.titanium.product.infrastructure.pricing.entity.RateTableRowDO;
import com.titanium.product.valueobject.RateTableRow;

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

    /** 费率行映射：租户ID/表ID沿用主表，其余同名字段自动映射。 */
    @Mapping(target = "tenantId", source = "rateTable.tenantId")
    @Mapping(target = "tableId", source = "rateTable.tableId")
    RateTableRowDO toDO(RateTableDefinition rateTable, RateTableRow row);

    /** 任意对象 → JSON 字符串。 */
    @Named("toJson")
    default String toJson(Object value) {
        return JSON.toJSONString(value);
    }
}
