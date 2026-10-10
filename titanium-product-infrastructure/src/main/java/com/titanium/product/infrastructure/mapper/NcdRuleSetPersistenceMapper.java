package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.infrastructure.pricing.entity.ncd.NcdRuleDO;
import com.titanium.product.infrastructure.pricing.entity.ncd.NcdRuleSetDO;
import com.titanium.product.pricing.aggregate.ncd.NcdRule;
import com.titanium.product.pricing.aggregate.ncd.NcdRuleSet;

/**
 * NCD 规则集领域对象 → 持久化对象声明式映射（同名字段自动映射）。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface NcdRuleSetPersistenceMapper {

    NcdRuleSetDO toDO(NcdRuleSet ruleSet);

    /**
     * 档位行映射：父键（规则集ID/租户ID）与档位键由父对象/领域方法给出，行ID在落库时新生成。
     * <p>
     * 🔴 行ID 不进入领域记录、也不参与内容指纹：档位是**纯值**（无独立身份语义，反算与指纹都与
     * 行序、行ID 无关），每次保存整体重写即可；把行ID 提到领域会让「同一份档位换行ID」被误判为
     * 内容变更，进而让出单快照的规则版本失去可比性。
     * </p>
     */
    @Mapping(target = "ruleSetId", source = "ruleSet.ruleSetId")
    @Mapping(target = "tenantId", source = "ruleSet.tenantId")
    @Mapping(target = "bandKey", expression = "java(rule.bandKey())")
    @Mapping(target = "ruleId", expression = "java(java.util.UUID.randomUUID().toString())")
    NcdRuleDO toDO(NcdRuleSet ruleSet, NcdRule rule);

    /**
     * 档位反向映射：持久化对象 → 领域档位。
     * <p>
     * {@code bandKey} 不从库列回填——领域侧由 {@code NcdRule#bandKey()} 重算，库列仅作唯一约束
     * 与迁移校验用（与费率行 {@code dimensionHash} 同法）。
     * </p>
     */
    NcdRule toDomain(NcdRuleDO dataObject);
}
