package com.titanium.product.infrastructure.pricing.adapter.calculation;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.CalculationNodeType;
import com.titanium.product.common.enums.CalculationOperator;
import com.titanium.product.infrastructure.mapper.CalculationModelPersistenceMapper;
import com.titanium.product.infrastructure.pricing.repository.calculation.CalculationEdgeJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.calculation.CalculationModelJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.calculation.CalculationNodeJpaRepository;
import com.titanium.product.pricing.aggregate.CalculationModelDefinition;
import com.titanium.product.valueobject.pricing.calculation.CalculationEdge;
import com.titanium.product.valueobject.pricing.calculation.CalculationNode;

@ExtendWith(MockitoExtension.class)
class JpaCalculationModelRepositoryTest {

    @Mock
    private CalculationModelJpaRepository modelJpaRepository;
    @Mock
    private CalculationNodeJpaRepository nodeJpaRepository;
    @Mock
    private CalculationEdgeJpaRepository edgeJpaRepository;
    @Mock
    private CalculationModelPersistenceMapper persistenceMapper;

    @InjectMocks
    private JpaCalculationModelRepository repository;

    @Test
    void shouldOnlyUpdateModelHeaderDuringLifecycleTransition() {
        when(modelJpaRepository.existsById("model-1")).thenReturn(true);

        repository.save(publishedModel());

        verify(modelJpaRepository).save(org.mockito.ArgumentMatchers.any());
        verify(nodeJpaRepository, never()).deleteByModelId("model-1");
        verify(edgeJpaRepository, never()).deleteByModelId("model-1");
        verify(nodeJpaRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
        verify(edgeJpaRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    private CalculationModelDefinition publishedModel() {
        List<CalculationNode> nodes = List.of(
                new CalculationNode("BASE", "标准保费", CalculationNodeType.INPUT,
                        CalculationOperator.STANDARD_PREMIUM, "BASE_PREMIUM", "V1", null, 10),
                new CalculationNode("TOTAL", "客户应付", CalculationNodeType.OUTPUT,
                        CalculationOperator.SUM, null, null, null, 100));
        return CalculationModelDefinition.restore(
                "model-1", "product-1", "MODEL", "V1", "模型", "", "CNY", nodes,
                List.of(new CalculationEdge("BASE", "TOTAL")), LocalDateTime.of(2026, 1, 1, 0, 0),
                null, "tenant-1", ActuarialDefinitionStatus.PUBLISHED, "hash");
    }
}
