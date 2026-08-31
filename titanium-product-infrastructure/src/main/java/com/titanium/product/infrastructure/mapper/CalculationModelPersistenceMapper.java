package com.titanium.product.infrastructure.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.aggregate.CalculationModelDefinition;
import com.titanium.product.infrastructure.pricing.entity.CalculationEdgeDO;
import com.titanium.product.infrastructure.pricing.entity.CalculationModelDO;
import com.titanium.product.infrastructure.pricing.entity.CalculationNodeDO;
import com.titanium.product.valueobject.pricing.CalculationEdge;
import com.titanium.product.valueobject.pricing.CalculationNode;


/**
 * 计算模型领域对象 → 持久化对象声明式映射。
 * 节点/边行 ID 由 {@link UUID} 随机生成（领域对象不携带行标识）。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        imports = UUID.class)
public interface CalculationModelPersistenceMapper {

    /** 计算模型主表映射（同名字段自动映射）。 */
    CalculationModelDO toDO(CalculationModelDefinition model);

    /** 计算节点映射：nodeId 仓储侧生成，modelId 由调用方传入。 */
    @Mapping(target = "nodeId", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "modelId", source = "modelId")
    CalculationNodeDO toDO(String modelId, CalculationNode node);

    /** 计算边映射：edgeId 仓储侧生成，modelId 由调用方传入。 */
    @Mapping(target = "edgeId", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "modelId", source = "modelId")
    CalculationEdgeDO toDO(String modelId, CalculationEdge edge);
}
