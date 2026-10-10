package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.infrastructure.pricing.entity.paidup.ReducedPaidUpPolicyDO;
import com.titanium.product.pricing.aggregate.paidup.ReducedPaidUpPolicy;

/**
 * 减额缴清策略领域对象 → 持久化对象声明式映射（同名字段自动映射，g04-05）。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ReducedPaidUpPolicyPersistenceMapper {

    ReducedPaidUpPolicyDO toDO(ReducedPaidUpPolicy policy);
}
