package com.titanium.product.web.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.application.command.pricing.RateTableCommandAppService;
import com.titanium.product.application.query.pricing.RateTableQueryAppService;
import com.titanium.product.command.pricing.CreateRateTableDraftCommand;
import com.titanium.product.command.pricing.ReplaceRateTableRowsCommand;
import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.common.enums.RateUnit;
import com.titanium.product.pricing.aggregate.RateTableDefinition;
import com.titanium.product.valueobject.pricing.RateTableRowDraft;
import com.titanium.product.valueobject.rate.RateTableRow;
import com.titanium.product.valueobject.rate.RateTableValidationResult;
import com.titanium.product.web.dto.pricing.ratetable.CreateRateTableDraftDTO;
import com.titanium.product.web.dto.pricing.ratetable.RateTableRowDTO;
import com.titanium.product.web.dto.pricing.ratetable.RateTableRowVO;
import com.titanium.product.web.dto.pricing.ratetable.RateTableVO;
import com.titanium.product.web.dto.pricing.ratetable.RateTableValidationVO;
import com.titanium.product.web.dto.pricing.ratetable.ReplaceRateTableRowsDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Product 费率表后台管理接口。
 */
@Validated
@RestController
@RequestMapping("/web/v1/products/{productId}/rate-tables")
@RequiredArgsConstructor
public class ProductRateTableController {

    private final RateTableCommandAppService rateTableCommandAppService;
    private final RateTableQueryAppService rateTableQueryAppService;

    @PostMapping
    public ApiResponse<String> createDraft(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CreateRateTableDraftDTO request) {
        RateUnit rateUnit = RateUnit.fromCode(request.rateUnit());
        if (rateUnit == null) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        String tableId = rateTableCommandAppService.createDraft(new CreateRateTableDraftCommand(
                tenantId, productId, request.tableCode(), request.tableVersion(), rateUnit, request.currency(),
                request.effectiveFrom(), request.effectiveTo(), request.dimensionKeys()));
        return ApiResponse.success(tableId);
    }

    @PutMapping("/{tableId}/rows")
    public ApiResponse<Void> replaceRows(
            @PathVariable String productId,
            @PathVariable String tableId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody ReplaceRateTableRowsDTO request) {
        rateTableCommandAppService.replaceRows(new ReplaceRateTableRowsCommand(
                tenantId, productId, tableId, request.rows().stream().map(this::toDraft).toList()));
        return ApiResponse.success(null);
    }

    @PostMapping("/{tableId}/validate")
    public ApiResponse<RateTableValidationVO> validate(
            @PathVariable String productId,
            @PathVariable String tableId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(toValidationResponse(
                rateTableCommandAppService.validate(tenantId, productId, tableId)));
    }

    @PostMapping("/{tableId}/publish")
    public ApiResponse<RateTableValidationVO> publish(
            @PathVariable String productId,
            @PathVariable String tableId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(toValidationResponse(
                rateTableCommandAppService.publish(tenantId, productId, tableId)));
    }

    @PostMapping("/{tableId}/retire")
    public ApiResponse<Void> retire(
            @PathVariable String productId,
            @PathVariable String tableId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        rateTableCommandAppService.retire(tenantId, productId, tableId);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<RateTableVO>> list(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String status) {
        RateTableStatus rateTableStatus = parseStatus(status);
        return ApiResponse.success(rateTableQueryAppService.list(tenantId, productId, rateTableStatus)
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{tableId}")
    public ApiResponse<RateTableVO> get(
            @PathVariable String productId,
            @PathVariable String tableId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(toResponse(
                rateTableQueryAppService.get(tenantId, productId, tableId)));
    }

    private RateTableRowDraft toDraft(RateTableRowDTO request) {
        return new RateTableRowDraft(
                request.ageFrom(), request.ageToExclusive(), request.gender(), request.paymentTermYears(),
                request.coverageTermYears(), request.rate(), request.minimumPremium(), request.maximumPremium());
    }

    private RateTableValidationVO toValidationResponse(RateTableValidationResult validation) {
        return new RateTableValidationVO(true, validation.rowCount(), validation.contentHash());
    }

    private RateTableVO toResponse(RateTableDefinition table) {
        return new RateTableVO(
                table.tableId(), table.productId(), table.tableCode(), table.tableVersion(), table.status().getCode(),
                table.rateUnit().getCode(), table.currency(), table.effectiveFrom(), table.effectiveTo(),
                table.dimensionKeys(), table.rows().size(), table.contentHash(),
                table.rows().stream().map(this::toRowResponse).toList());
    }

    private RateTableRowVO toRowResponse(RateTableRow row) {
        return new RateTableRowVO(
                row.rowId(), row.ageFrom(), row.ageToExclusive(), row.gender(), row.paymentTermYears(),
                row.coverageTermYears(), row.rate(), row.minimumPremium(), row.maximumPremium(), row.dimensionHash());
    }

    private RateTableStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        RateTableStatus parsed = RateTableStatus.fromCode(status);
        if (parsed == null) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        return parsed;
    }
}
