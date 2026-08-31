package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.aggregate.ChargeComponentDefinition;
import com.titanium.product.infrastructure.pricing.entity.ChargeComponentDO;

/**
 * 费用项领域对象 → 持久化对象声明式映射（DO 审计列由 JPA/基类维护，忽略未映射目标字段）。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ChargeComponentPersistenceMapper {

    /** 领域对象 → 持久化对象（同名字段自动映射）。 */
    ChargeComponentDO toDO(ChargeComponentDefinition component);
}
