package com.titanium.product.application.orchestration.pricing;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.command.pricing.CreateRateTableDraftCommand;
import com.titanium.product.command.pricing.ReplaceRateTableRowsCommand;
import com.titanium.product.pricing.aggregate.RateTableDefinition;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.RateTableManagementRepository;
import com.titanium.product.valueobject.pricing.RateTableRowDraft;
import com.titanium.product.valueobject.rate.RateTableRow;
import com.titanium.product.valueobject.rate.RateTableValidationResult;

import lombok.RequiredArgsConstructor;

/**
 * Product 费率表后台管理编排。
 */
@Service
@RequiredArgsConstructor
public class RateTableManagementApplicationService {

    private final ProductQueryService productQueryService;
    private final RateTableManagementRepository rateTableManagementRepository;

    /** 创建空费率表草稿。 */
    @Transactional
    public String createDraft(CreateRateTableDraftCommand command) {
        validateProduct(command.productId(), command.tenantId());
        if (rateTableManagementRepository.existsByBusinessKey(
                command.tenantId(), command.productId(), command.tableCode(), command.tableVersion())) {
            throw new BusinessException(ProductErrorCode.RATE_TABLE_ALREADY_EXISTS);
        }
        String tableId = UUID.randomUUID().toString();
        RateTableDefinition rateTable = RateTableDefinition.createDraft(
                tableId, command.productId(), command.tableCode(), command.tableVersion(), command.rateUnit(),
                command.currency(), command.effectiveFrom(), command.effectiveTo(), command.dimensionKeys(),
                command.tenantId());
        rateTableManagementRepository.save(rateTable);
        return tableId;
    }

    /** 整体替换草稿费率行。 */
    @Transactional
    public void replaceRows(ReplaceRateTableRowsCommand command) {
        RateTableDefinition rateTable = requireRateTable(
                command.tenantId(), command.productId(), command.tableId());
        List<RateTableRow> rows = command.rows().stream().map(this::toRateTableRow).toList();
        rateTable.replaceRows(rows);
        rateTableManagementRepository.save(rateTable);
    }

    /** 执行发布前校验，但不修改费率表状态。 */
    @Transactional(readOnly = true)
    public RateTableValidationResult validate(String tenantId, String productId, String tableId) {
        return requireRateTable(tenantId, productId, tableId).validateForPublish();
    }

    /** 发布草稿版本。 */
    @Transactional
    public RateTableValidationResult publish(String tenantId, String productId, String tableId) {
        RateTableDefinition rateTable = requireRateTable(tenantId, productId, tableId);
        RateTableValidationResult validation = rateTable.publish();
        rateTableManagementRepository.save(rateTable);
        return validation;
    }

    /** 退役已发布版本。 */
    @Transactional
    public void retire(String tenantId, String productId, String tableId) {
        RateTableDefinition rateTable = requireRateTable(tenantId, productId, tableId);
        rateTable.retire();
        rateTableManagementRepository.save(rateTable);
    }

    private RateTableDefinition requireRateTable(String tenantId, String productId, String tableId) {
        validateProduct(productId, tenantId);
        return rateTableManagementRepository.findById(tenantId, productId, tableId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.RATE_TABLE_NOT_FOUND));
    }

    private void validateProduct(String productId, String tenantId) {
        ProductQueryResult product = productQueryService.findProductById(productId, tenantId);
        if (product == null) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_EXIST);
        }
    }

    private RateTableRow toRateTableRow(RateTableRowDraft draft) {
        return new RateTableRow(
                UUID.randomUUID().toString(), draft.ageFrom(), draft.ageToExclusive(), draft.gender(),
                draft.paymentTermYears(), draft.coverageTermYears(), draft.rate(), draft.minimumPremium(),
                draft.maximumPremium());
    }
}
