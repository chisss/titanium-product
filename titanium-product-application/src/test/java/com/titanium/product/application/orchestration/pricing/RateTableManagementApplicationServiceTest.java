package com.titanium.product.application.orchestration.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.command.pricing.CreateRateTableDraftCommand;
import com.titanium.product.common.enums.RateUnit;
import com.titanium.product.pricing.aggregate.RateTableDefinition;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.RateTableManagementRepository;

class RateTableManagementApplicationServiceTest {

    private ProductQueryService productQueryService;

    private RateTableManagementRepository rateTableManagementRepository;

    private RateTableManagementApplicationService service;

    @BeforeEach
    void setUp() {
        productQueryService = mock(ProductQueryService.class);
        rateTableManagementRepository = mock(RateTableManagementRepository.class);
        service = new RateTableManagementApplicationService(productQueryService, rateTableManagementRepository);
    }

    @Test
    void shouldCreateDraftForTenantProduct() {
        when(productQueryService.findProductById("PRODUCT-1", "TENANT-1"))
                .thenReturn(new ProductQueryResult());

        String tableId = service.createDraft(createCommand());

        verify(rateTableManagementRepository).save(any(RateTableDefinition.class));
        assertEquals(36, tableId.length());
    }

    @Test
    void shouldRejectDuplicateBusinessVersion() {
        when(productQueryService.findProductById("PRODUCT-1", "TENANT-1"))
                .thenReturn(new ProductQueryResult());
        when(rateTableManagementRepository.existsByBusinessKey(
                "TENANT-1", "PRODUCT-1", "RATE-LIFE", "V1.0")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createDraft(createCommand()));

        assertEquals(ProductErrorCode.RATE_TABLE_ALREADY_EXISTS.getCode(), exception.getErrorCode());
        verify(rateTableManagementRepository, never()).save(any());
    }

    private CreateRateTableDraftCommand createCommand() {
        return new CreateRateTableDraftCommand(
                "TENANT-1", "PRODUCT-1", "RATE-LIFE", "V1.0", RateUnit.SUM_INSURED_RATIO, "CNY",
                LocalDateTime.of(2026, 1, 1, 0, 0), null,
                List.of("age", "gender", "paymentTerm", "coverageTerm"));
    }
}
