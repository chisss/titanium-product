package com.titanium.product.infrastructure.pricing.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.aggregate.CalculationModelDefinition;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.pricing.entity.CalculationEdgeEntity;
import com.titanium.product.infrastructure.pricing.entity.CalculationModelEntity;
import com.titanium.product.infrastructure.pricing.entity.CalculationNodeEntity;
import com.titanium.product.infrastructure.pricing.repository.CalculationEdgeJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.CalculationModelJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.CalculationNodeJpaRepository;
import com.titanium.product.repository.CalculationModelRepository;
import com.titanium.product.valueobject.pricing.CalculationEdge;
import com.titanium.product.valueobject.pricing.CalculationNode;

import lombok.RequiredArgsConstructor;

/**
 * 结构化计算模型关系型仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class JpaCalculationModelRepository implements CalculationModelRepository {

    private final CalculationModelJpaRepository modelJpaRepository;
    private final CalculationNodeJpaRepository nodeJpaRepository;
    private final CalculationEdgeJpaRepository edgeJpaRepository;

    @Override
    public boolean existsByBusinessKey(
            String tenantId, String productId, String modelCode, String modelVersion) {
        return modelJpaRepository.existsByTenantIdAndProductIdAndModelCodeAndModelVersion(
                tenantId, productId, modelCode, modelVersion);
    }

    @Override
    public Optional<CalculationModelDefinition> findById(
            String tenantId, String productId, String modelId) {
        return modelJpaRepository.findByModelIdAndTenantIdAndProductId(modelId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    public Optional<CalculationModelDefinition> findPublished(
            String tenantId,
            String productId,
            String modelCode,
            String modelVersion,
            LocalDateTime businessTime) {
        return modelJpaRepository.findPublished(tenantId, productId, modelCode, modelVersion, businessTime)
                .map(this::toDomain);
    }

    @Override
    public List<CalculationModelDefinition> findAll(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        List<CalculationModelEntity> entities = status == null
                ? modelJpaRepository.findByTenantIdAndProductIdOrderByCreateTimeDesc(tenantId, productId)
                : modelJpaRepository.findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
                        tenantId, productId, status);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(CalculationModelDefinition model) {
        boolean newModel = !modelJpaRepository.existsById(model.getModelId());
        modelJpaRepository.save(toEntity(model));
        if (newModel) {
            nodeJpaRepository.saveAll(
                    model.getNodes().stream().map(node -> toEntity(model.getModelId(), node)).toList());
            edgeJpaRepository.saveAll(
                    model.getEdges().stream().map(edge -> toEntity(model.getModelId(), edge)).toList());
        }
    }

    private CalculationModelEntity toEntity(CalculationModelDefinition model) {
        CalculationModelEntity entity = new CalculationModelEntity();
        entity.setModelId(model.getModelId());
        entity.setProductId(model.getProductId());
        entity.setModelCode(model.getModelCode());
        entity.setModelVersion(model.getModelVersion());
        entity.setModelName(model.getModelName());
        entity.setDescription(model.getDescription());
        entity.setCurrency(model.getCurrency());
        entity.setEffectiveFrom(model.getEffectiveFrom());
        entity.setEffectiveTo(model.getEffectiveTo());
        entity.setTenantId(model.getTenantId());
        entity.setStatus(model.getStatus());
        entity.setContentHash(model.getContentHash());
        return entity;
    }

    private CalculationNodeEntity toEntity(String modelId, CalculationNode node) {
        CalculationNodeEntity entity = new CalculationNodeEntity();
        entity.setNodeId(UUID.randomUUID().toString());
        entity.setModelId(modelId);
        entity.setNodeCode(node.nodeCode());
        entity.setNodeName(node.nodeName());
        entity.setNodeType(node.nodeType());
        entity.setOperator(node.operator());
        entity.setComponentCode(node.componentCode());
        entity.setComponentVersion(node.componentVersion());
        entity.setParameterValue(node.parameterValue());
        entity.setExecutionOrder(node.executionOrder());
        return entity;
    }

    private CalculationEdgeEntity toEntity(String modelId, CalculationEdge edge) {
        CalculationEdgeEntity entity = new CalculationEdgeEntity();
        entity.setEdgeId(UUID.randomUUID().toString());
        entity.setModelId(modelId);
        entity.setFromNodeCode(edge.fromNodeCode());
        entity.setToNodeCode(edge.toNodeCode());
        return entity;
    }

    private CalculationModelDefinition toDomain(CalculationModelEntity entity) {
        List<CalculationNode> nodes = nodeJpaRepository.findByModelIdOrderByExecutionOrderAsc(entity.getModelId())
                .stream()
                .map(node -> new CalculationNode(
                        node.getNodeCode(), node.getNodeName(), node.getNodeType(), node.getOperator(),
                        node.getComponentCode(), node.getComponentVersion(), node.getParameterValue(),
                        node.getExecutionOrder()))
                .toList();
        List<CalculationEdge> edges = edgeJpaRepository
                .findByModelIdOrderByFromNodeCodeAscToNodeCodeAsc(entity.getModelId())
                .stream()
                .map(edge -> new CalculationEdge(edge.getFromNodeCode(), edge.getToNodeCode()))
                .toList();
        return CalculationModelDefinition.restore(
                entity.getModelId(), entity.getProductId(), entity.getModelCode(), entity.getModelVersion(),
                entity.getModelName(), entity.getDescription(), entity.getCurrency(), nodes, edges,
                entity.getEffectiveFrom(), entity.getEffectiveTo(), entity.getTenantId(), entity.getStatus(),
                entity.getContentHash());
    }
}
