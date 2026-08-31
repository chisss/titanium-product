package com.titanium.product.application.query.pricing;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.RateTableDefinition;
import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.RateTableManagementRepository;

import lombok.RequiredArgsConstructor;

/**
 * 费率表读侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class RateTableQueryAppService {

    private final ProductQueryService productQueryService;
    private final RateTableManagementRepository rateTableManagementRepository;

    /** 查询费率表详情。 */
    @Transactional(readOnly = true)
    public RateTableDefinition get(String tenantId, String productId, String tableId) {
        validateProduct(productId, tenantId);
        return rateTableManagementRepository.findById(tenantId, productId, tableId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.RATE_TABLE_NOT_FOUND));
    }

    /** 查询产品下的费率表版本。 */
    @Transactional(readOnly = true)
    public List<RateTableDefinition> list(String tenantId, String productId, RateTableStatus status) {
        validateProduct(productId, tenantId);
        return rateTableManagementRepository.findAll(tenantId, productId, status);
    }

    private void validateProduct(String productId, String tenantId) {
        ProductQueryResult product = productQueryService.findProductById(productId, tenantId);
        if (product == null) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_EXIST);
        }
    }
}
