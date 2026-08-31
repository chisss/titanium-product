package com.titanium.product.infrastructure.pricing.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.aggregate.CalculationModelDefinition;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.mapper.CalculationModelPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.CalculationModelDO;
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
    private final CalculationModelPersistenceMapper persistenceMapper;

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
        List<CalculationModelDO> dataObjects = status == null
                ? modelJpaRepository.findByTenantIdAndProductIdOrderByCreateTimeDesc(tenantId, productId)
                : modelJpaRepository.findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
                        tenantId, productId, status);
        return dataObjects.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(CalculationModelDefinition model) {
        boolean newModel = !modelJpaRepository.existsById(model.getModelId());
        modelJpaRepository.save(persistenceMapper.toDO(model));
        if (newModel) {
            nodeJpaRepository.saveAll(
                    model.getNodes().stream().map(node -> persistenceMapper.toDO(model.getModelId(), node)).toList());
            edgeJpaRepository.saveAll(
                    model.getEdges().stream().map(edge -> persistenceMapper.toDO(model.getModelId(), edge)).toList());
        }
    }

    private CalculationModelDefinition toDomain(CalculationModelDO dataObject) {
        List<CalculationNode> nodes = nodeJpaRepository
                .findByModelIdOrderByExecutionOrderAsc(dataObject.getModelId())
                .stream()
                .map(node -> new CalculationNode(
                        node.getNodeCode(), node.getNodeName(), node.getNodeType(), node.getOperator(),
                        node.getComponentCode(), node.getComponentVersion(), node.getParameterValue(),
                        node.getExecutionOrder()))
                .toList();
        List<CalculationEdge> edges = edgeJpaRepository
                .findByModelIdOrderByFromNodeCodeAscToNodeCodeAsc(dataObject.getModelId())
                .stream()
                .map(edge -> new CalculationEdge(edge.getFromNodeCode(), edge.getToNodeCode()))
                .toList();
        return CalculationModelDefinition.restore(
                dataObject.getModelId(), dataObject.getProductId(), dataObject.getModelCode(),
                dataObject.getModelVersion(), dataObject.getModelName(), dataObject.getDescription(),
                dataObject.getCurrency(), nodes, edges, dataObject.getEffectiveFrom(),
                dataObject.getEffectiveTo(), dataObject.getTenantId(), dataObject.getStatus(),
                dataObject.getContentHash());
    }
}
