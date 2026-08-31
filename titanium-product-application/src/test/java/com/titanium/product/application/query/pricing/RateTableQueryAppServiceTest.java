package com.titanium.product.application.query.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.RateTableManagementRepository;

class RateTableQueryAppServiceTest {

    private ProductQueryService productQueryService;
    private RateTableManagementRepository rateTableManagementRepository;
    private RateTableQueryAppService service;

    @BeforeEach
    void setUp() {
        productQueryService = mock(ProductQueryService.class);
        rateTableManagementRepository = mock(RateTableManagementRepository.class);
        service = new RateTableQueryAppService(productQueryService, rateTableManagementRepository);
    }

    @Test
    void shouldHideRateTableWhenTenantDoesNotOwnProduct() {
        when(productQueryService.findProductById("PRODUCT-1", "OTHER-TENANT")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.get("OTHER-TENANT", "PRODUCT-1", "TABLE-1"));

        assertEquals(ProductErrorCode.PRODUCT_NOT_EXIST.getCode(), exception.getErrorCode());
        verify(rateTableManagementRepository, never()).findById(any(), any(), any());
    }

    @Test
    void shouldReturnNotFoundForUnknownTableInOwnedProduct() {
        when(productQueryService.findProductById("PRODUCT-1", "TENANT-1"))
                .thenReturn(new ProductQueryResult());
        when(rateTableManagementRepository.findById("TENANT-1", "PRODUCT-1", "TABLE-1"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.get("TENANT-1", "PRODUCT-1", "TABLE-1"));

        assertEquals(ProductErrorCode.RATE_TABLE_NOT_FOUND.getCode(), exception.getErrorCode());
    }
}
