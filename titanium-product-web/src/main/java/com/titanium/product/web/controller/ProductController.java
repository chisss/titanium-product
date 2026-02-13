package com.titanium.product.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.api.ProductApi;
import com.titanium.product.api.dto.ProductDTO;
import com.titanium.product.api.request.AuditProductRequest;
import com.titanium.product.api.request.CreateProductRequest;
import com.titanium.product.api.response.ApiResponse;
import com.titanium.product.application.command.ProductCommandAppService;
import com.titanium.product.application.query.ProductQueryAppService;
import com.titanium.product.domain.command.AuditProductCommand;
import com.titanium.product.domain.command.CreateProductCommand;
import com.titanium.product.domain.command.InvalidateProductCommand;
import com.titanium.product.domain.command.RejectProductAuditCommand;
import com.titanium.product.domain.command.SubmitProductForAuditCommand;
import com.titanium.product.web.mapper.ProductWebMapper;

/**
 * 产品控制器 处理产品相关的HTTP请求，实现ProductApi接口
 */
@RestController
public class ProductController implements ProductApi {

    @Autowired
    private ProductCommandAppService productCommandAppService;

    @Autowired
    private ProductQueryAppService   productQueryAppService;

    @Autowired
    private ProductWebMapper         productWebMapper;

    @Override
    public ApiResponse<String> createProduct(CreateProductRequest request, String tenantId) {
        CreateProductCommand command = productWebMapper.toCreateProductCommand(request, tenantId);
        String productId = productCommandAppService.createProduct(command);
        return ApiResponse.success(productId);
    }

    @Override
    public ApiResponse<ProductDTO> getProductById(String productId, String tenantId) {
        var product = productQueryAppService.queryProductDetail(productId);
        ProductDTO productDTO = productWebMapper.toProductDTO(product);
        return ApiResponse.success(productDTO);
    }

    @Override
    public ApiResponse<Void> submitForAudit(String productId, String tenantId) {
        SubmitProductForAuditCommand command = new SubmitProductForAuditCommand(productId, "system", "system");
        productCommandAppService.submitForAudit(command);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> auditProduct(String productId, AuditProductRequest request, String tenantId) {
        AuditProductCommand command = new AuditProductCommand(productId, request.getAuditorId(),
                request.getAuditorName(), request.getAuditOpinion(), ProductEnum.AuditResult.PASS);
        productCommandAppService.auditProduct(command);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> rejectAudit(String productId, AuditProductRequest request, String tenantId) {
        RejectProductAuditCommand command = new RejectProductAuditCommand(productId, request.getAuditorId(),
                request.getAuditorName(), request.getAuditOpinion());
        productCommandAppService.rejectAudit(command);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> invalidateProduct(String productId, String tenantId) {
        InvalidateProductCommand command = new InvalidateProductCommand(productId);
        productCommandAppService.invalidateProduct(command);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Object> getIssuanceConfig(String productId, String tenantId) {
        var product = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(product.getIssuanceProcessConfig());
    }

    @Override
    public ApiResponse<Object> getUnderwritingConfig(String productId, String tenantId) {
        var product = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(product.getUnderwritingConfig());
    }

    @Override
    public ApiResponse<Object> getPolicyFormConfig(String productId, String tenantId) {
        var product = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(product.getPolicyFormConfig());
    }
}
