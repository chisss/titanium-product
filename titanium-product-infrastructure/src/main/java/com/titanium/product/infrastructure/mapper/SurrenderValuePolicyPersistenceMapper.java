package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.infrastructure.pricing.entity.premium.SurrenderValuePolicyDO;
import com.titanium.product.pricing.aggregate.surrender.SurrenderValuePolicy;

/**
 * 退保价值策略领域对象 → 持久化对象声明式映射（同名字段自动映射）。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface SurrenderValuePolicyPersistenceMapper {

    SurrenderValuePolicyDO toDO(SurrenderValuePolicy policy);
}
