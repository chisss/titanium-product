package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.infrastructure.pricing.entity.pricing.DynamicFactorDO;
import com.titanium.product.pricing.aggregate.DynamicFactorDefinition;

/**
 * 动态因子领域对象 → 持久化对象声明式映射（同名字段自动映射）。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface DynamicFactorPersistenceMapper {

    DynamicFactorDO toDO(DynamicFactorDefinition factor);
}
