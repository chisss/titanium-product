package com.titanium.product.infrastructure.pricing.adapter.premium;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.exception.PremiumLifecycleAdjustmentConcurrentConflictException;
import com.titanium.product.infrastructure.mapper.PremiumLifecycleAdjustmentPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.premium.PremiumLifecycleAdjustmentDO;
import com.titanium.product.infrastructure.pricing.entity.premium.PremiumLifecycleDifferenceLineDO;
import com.titanium.product.infrastructure.pricing.repository.premium.PremiumLifecycleAdjustmentJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.premium.PremiumLifecycleDifferenceLineJpaRepository;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.repository.PremiumLifecycleAdjustmentRepository;
import com.titanium.product.valueobject.pricing.premium.PremiumLifecycleDifference;
import com.titanium.product.valueobject.pricing.premium.PremiumLifecycleDifferenceLine;

import lombok.RequiredArgsConstructor;

/**
 * 生命周期费用差额事实 JPA 适配器。
 */
@Repository
@RequiredArgsConstructor
public class JpaPremiumLifecycleAdjustmentRepository implements PremiumLifecycleAdjustmentRepository {

    private final PremiumLifecycleAdjustmentJpaRepository adjustmentJpaRepository;
    private final PremiumLifecycleDifferenceLineJpaRepository lineJpaRepository;
    private final PremiumLifecycleAdjustmentPersistenceMapper persistenceMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<PremiumLifecycleAdjustment> findById(String tenantId, String adjustmentId) {
        return adjustmentJpaRepository.findByAdjustmentIdAndTenantId(adjustmentId, tenantId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PremiumLifecycleAdjustment> findByRequestId(
            String tenantId, String adjustmentRequestId) {
        return adjustmentJpaRepository.findByTenantIdAndAdjustmentRequestId(tenantId, adjustmentRequestId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PremiumLifecycleAdjustment> findByReversalOfAdjustmentId(
            String tenantId, String sourceAdjustmentId) {
        return adjustmentJpaRepository.findByTenantIdAndReversalOfAdjustmentId(tenantId, sourceAdjustmentId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(PremiumLifecycleAdjustment adjustment) {
        try {
            adjustmentJpaRepository.saveAndFlush(persistenceMapper.toDO(adjustment));
            lineJpaRepository.saveAll(adjustment.getLines().stream()
                    .map(line -> persistenceMapper.toDO(adjustment.getAdjustmentId(), line))
                    .toList());
        } catch (DataIntegrityViolationException exception) {
            throw new PremiumLifecycleAdjustmentConcurrentConflictException(exception);
        }
    }

    private PremiumLifecycleAdjustment toDomain(PremiumLifecycleAdjustmentDO dataObject) {
        List<PremiumLifecycleDifferenceLine> lines = lineJpaRepository
                .findByIdAdjustmentIdOrderByIdLineIdAsc(dataObject.getAdjustmentId())
                .stream().map(this::toDomain).toList();
        PremiumLifecycleDifference difference = new PremiumLifecycleDifference(
                dataObject.getDirection(), dataObject.getCustomerAmount(), dataObject.getTaxDirection(),
                dataObject.getTaxAmount(), dataObject.getInternalCostDirection(),
                dataObject.getInternalCostAmount(), lines);
        if (dataObject.getReversalOfAdjustmentId() != null) {
            return PremiumLifecycleAdjustment.confirmReversal(
                    dataObject.getAdjustmentId(), dataObject.getAdjustmentRequestId(),
                    dataObject.getReversalOfAdjustmentId(), dataObject.getBizNo(),
                    dataObject.getLifecycleType(), dataObject.getTenantId(), dataObject.getProductId(),
                    dataObject.getOriginalCalculationId(), dataObject.getOriginalResultHash(),
                    dataObject.getReplacementCalculationId(), dataObject.getReplacementResultHash(),
                    dataObject.getBusinessTime(), dataObject.getCurrency(), difference,
                    dataObject.getReason(), dataObject.getRequestHash(), dataObject.getResultHash(),
                    dataObject.getCreatedAt());
        }
        return PremiumLifecycleAdjustment.confirm(
                dataObject.getAdjustmentId(), dataObject.getAdjustmentRequestId(), dataObject.getBizNo(),
                dataObject.getLifecycleType(), dataObject.getTenantId(), dataObject.getProductId(),
                dataObject.getOriginalCalculationId(), dataObject.getOriginalResultHash(),
                dataObject.getReplacementCalculationId(), dataObject.getReplacementResultHash(),
                dataObject.getBusinessTime(), dataObject.getCurrency(), difference,
                dataObject.getReason(), dataObject.getRequestHash(), dataObject.getResultHash(),
                dataObject.getCreatedAt());
    }

    private PremiumLifecycleDifferenceLine toDomain(PremiumLifecycleDifferenceLineDO dataObject) {
        return new PremiumLifecycleDifferenceLine(
                dataObject.getId().getLineId(), dataObject.getComponentCode(),
                dataObject.getOriginalComponentVersion(), dataObject.getReplacementComponentVersion(),
                dataObject.getCategory(), dataObject.getAmountChannel(), dataObject.getDirection(),
                dataObject.getPayerType(), dataObject.getAccountingClass(), dataObject.getCurrency(),
                dataObject.getOriginalDirection(), dataObject.getBeforeAmount(),
                dataObject.getReplacementDirection(), dataObject.getAfterAmount(),
                dataObject.getDifferenceAmount(), dataObject.isCustomerVisible(),
                dataObject.isAffectsCustomerPayable(), dataObject.getDescription());
    }
}
