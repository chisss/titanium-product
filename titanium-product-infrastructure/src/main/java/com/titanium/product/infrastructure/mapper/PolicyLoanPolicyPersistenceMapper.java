package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.infrastructure.pricing.entity.loan.PolicyLoanPolicyDO;
import com.titanium.product.pricing.aggregate.loan.PolicyLoanPolicy;

/**
 * 保单贷款策略领域对象 → 持久化对象声明式映射（同名字段自动映射，g04-03）。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PolicyLoanPolicyPersistenceMapper {

    PolicyLoanPolicyDO toDO(PolicyLoanPolicy policy);
}
