package com.titanium.product.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;

class ProductQueryAppServiceTest {

    private ProductQueryService productQueryService;
    private ProductQueryAppService service;

    @BeforeEach
    void setUp() {
        productQueryService = mock(ProductQueryService.class);
        service = new ProductQueryAppService(mock(QueryGateway.class), productQueryService);
    }

    @Test
    void shouldQueryProductByCodeWithinTenant() {
        ProductQueryResult expected = new ProductQueryResult();
        when(productQueryService.findProductByCode("TERM_LIFE_V1", "tenant-a")).thenReturn(expected);

        ProductQueryResult actual = service.queryProductByCode("TERM_LIFE_V1", "tenant-a");

        assertSame(expected, actual);
        verify(productQueryService).findProductByCode("TERM_LIFE_V1", "tenant-a");
    }

    @Test
    void shouldRejectMissingProductWithoutNullPointerException() {
        when(productQueryService.findProductById("missing", "tenant-a")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.queryProductDetail("missing", "tenant-a"));

        assertEquals(ProductErrorCode.PRODUCT_NOT_EXIST.getCode(), exception.getErrorCode());
    }
}
