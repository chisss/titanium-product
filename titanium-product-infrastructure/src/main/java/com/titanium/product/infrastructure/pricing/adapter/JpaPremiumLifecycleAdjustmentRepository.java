package com.titanium.product.infrastructure.pricing.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.exception.PremiumLifecycleAdjustmentConcurrentConflictException;
import com.titanium.product.infrastructure.pricing.entity.PremiumLifecycleAdjustmentEntity;
import com.titanium.product.infrastructure.pricing.entity.PremiumLifecycleDifferenceLineEntity;
import com.titanium.product.infrastructure.pricing.entity.PremiumLifecycleDifferenceLineId;
import com.titanium.product.infrastructure.pricing.repository.PremiumLifecycleAdjustmentJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PremiumLifecycleDifferenceLineJpaRepository;
import com.titanium.product.repository.PremiumLifecycleAdjustmentRepository;
import com.titanium.product.valueobject.pricing.lifecycle.PremiumLifecycleDifference;
import com.titanium.product.valueobject.pricing.lifecycle.PremiumLifecycleDifferenceLine;

import lombok.RequiredArgsConstructor;

/**
 * 生命周期费用差额事实 JPA 适配器。
 */
@Repository
@RequiredArgsConstructor
public class JpaPremiumLifecycleAdjustmentRepository implements PremiumLifecycleAdjustmentRepository {

    private final PremiumLifecycleAdjustmentJpaRepository adjustmentJpaRepository;
    private final PremiumLifecycleDifferenceLineJpaRepository lineJpaRepository;

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
            adjustmentJpaRepository.saveAndFlush(toEntity(adjustment));
            lineJpaRepository.saveAll(adjustment.getLines().stream()
                    .map(line -> toEntity(adjustment.getAdjustmentId(), line))
                    .toList());
        } catch (DataIntegrityViolationException exception) {
            throw new PremiumLifecycleAdjustmentConcurrentConflictException(exception);
        }
    }

    private PremiumLifecycleAdjustmentEntity toEntity(PremiumLifecycleAdjustment adjustment) {
        PremiumLifecycleAdjustmentEntity entity = new PremiumLifecycleAdjustmentEntity();
        entity.setAdjustmentId(adjustment.getAdjustmentId());
        entity.setAdjustmentRequestId(adjustment.getAdjustmentRequestId());
        entity.setReversalOfAdjustmentId(adjustment.getReversalOfAdjustmentId());
        entity.setBizNo(adjustment.getBizNo());
        entity.setLifecycleType(adjustment.getLifecycleType());
        entity.setTenantId(adjustment.getTenantId());
        entity.setProductId(adjustment.getProductId());
        entity.setOriginalCalculationId(adjustment.getOriginalCalculationId());
        entity.setOriginalResultHash(adjustment.getOriginalResultHash());
        entity.setReplacementCalculationId(adjustment.getReplacementCalculationId());
        entity.setReplacementResultHash(adjustment.getReplacementResultHash());
        entity.setBusinessTime(adjustment.getBusinessTime());
        entity.setCurrency(adjustment.getCurrency());
        entity.setDirection(adjustment.getDirection());
        entity.setCustomerAmount(adjustment.getCustomerAmount());
        entity.setTaxDirection(adjustment.getTaxDirection());
        entity.setTaxAmount(adjustment.getTaxAmount());
        entity.setInternalCostDirection(adjustment.getInternalCostDirection());
        entity.setInternalCostAmount(adjustment.getInternalCostAmount());
        entity.setReason(adjustment.getReason());
        entity.setRequestHash(adjustment.getRequestHash());
        entity.setResultHash(adjustment.getResultHash());
        return entity;
    }

    private PremiumLifecycleDifferenceLineEntity toEntity(
            String adjustmentId, PremiumLifecycleDifferenceLine line) {
        PremiumLifecycleDifferenceLineEntity entity = new PremiumLifecycleDifferenceLineEntity();
        entity.setId(new PremiumLifecycleDifferenceLineId(adjustmentId, line.lineId()));
        entity.setComponentCode(line.componentCode());
        entity.setOriginalComponentVersion(line.originalComponentVersion());
        entity.setReplacementComponentVersion(line.replacementComponentVersion());
        entity.setCategory(line.category());
        entity.setAmountChannel(line.amountChannel());
        entity.setDirection(line.direction());
        entity.setPayerType(line.payerType());
        entity.setAccountingClass(line.accountingClass());
        entity.setCurrency(line.currency());
        entity.setOriginalDirection(line.originalDirection());
        entity.setBeforeAmount(line.beforeAmount());
        entity.setReplacementDirection(line.replacementDirection());
        entity.setAfterAmount(line.afterAmount());
        entity.setDifferenceAmount(line.differenceAmount());
        entity.setCustomerVisible(line.customerVisible());
        entity.setAffectsCustomerPayable(line.affectsCustomerPayable());
        entity.setDescription(line.description());
        return entity;
    }

    private PremiumLifecycleAdjustment toDomain(PremiumLifecycleAdjustmentEntity entity) {
        List<PremiumLifecycleDifferenceLine> lines = lineJpaRepository
                .findByIdAdjustmentIdOrderByIdLineIdAsc(entity.getAdjustmentId())
                .stream().map(this::toDomain).toList();
        PremiumLifecycleDifference difference = new PremiumLifecycleDifference(
                entity.getDirection(), entity.getCustomerAmount(), entity.getTaxDirection(),
                entity.getTaxAmount(), entity.getInternalCostDirection(), entity.getInternalCostAmount(), lines);
        if (entity.getReversalOfAdjustmentId() != null) {
            return PremiumLifecycleAdjustment.confirmReversal(
                    entity.getAdjustmentId(), entity.getAdjustmentRequestId(), entity.getReversalOfAdjustmentId(),
                    entity.getBizNo(), entity.getLifecycleType(), entity.getTenantId(), entity.getProductId(),
                    entity.getOriginalCalculationId(), entity.getOriginalResultHash(),
                    entity.getReplacementCalculationId(), entity.getReplacementResultHash(),
                    entity.getBusinessTime(), entity.getCurrency(), difference, entity.getReason(),
                    entity.getRequestHash(), entity.getResultHash(), entity.getCreatedAt());
        }
        return PremiumLifecycleAdjustment.confirm(
                entity.getAdjustmentId(), entity.getAdjustmentRequestId(), entity.getBizNo(),
                entity.getLifecycleType(), entity.getTenantId(), entity.getProductId(),
                entity.getOriginalCalculationId(), entity.getOriginalResultHash(),
                entity.getReplacementCalculationId(), entity.getReplacementResultHash(),
                entity.getBusinessTime(), entity.getCurrency(), difference, entity.getReason(),
                entity.getRequestHash(), entity.getResultHash(), entity.getCreatedAt());
    }

    private PremiumLifecycleDifferenceLine toDomain(PremiumLifecycleDifferenceLineEntity entity) {
        return new PremiumLifecycleDifferenceLine(
                entity.getId().getLineId(), entity.getComponentCode(), entity.getOriginalComponentVersion(),
                entity.getReplacementComponentVersion(), entity.getCategory(), entity.getAmountChannel(),
                entity.getDirection(), entity.getPayerType(), entity.getAccountingClass(), entity.getCurrency(),
                entity.getOriginalDirection(), entity.getBeforeAmount(), entity.getReplacementDirection(),
                entity.getAfterAmount(), entity.getDifferenceAmount(), entity.isCustomerVisible(),
                entity.isAffectsCustomerPayable(), entity.getDescription());
    }
}
