package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.aggregate.TaxPolicyDefinition;
import com.titanium.product.infrastructure.pricing.entity.TaxBaseItemDO;
import com.titanium.product.infrastructure.pricing.entity.TaxPolicyDO;

/**
 * 税务政策领域对象 → 持久化对象声明式映射（同名字段自动映射）。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface TaxPolicyPersistenceMapper {

    TaxPolicyDO toDO(TaxPolicyDefinition policy);

    /** 计税基数项映射：itemId 由调用方按策略ID与基数代码派生，policyId 沿用主表。 */
    @Mapping(target = "itemId", source = "itemId")
    @Mapping(target = "policyId", source = "policy.policyId")
    @Mapping(target = "componentCode", source = "componentCode")
    @Mapping(target = "sortOrder", source = "sortOrder")
    TaxBaseItemDO toDO(TaxPolicyDefinition policy, String itemId, String componentCode, int sortOrder);
}
