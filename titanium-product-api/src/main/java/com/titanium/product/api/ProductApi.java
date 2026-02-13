package com.titanium.product.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.titanium.product.api.dto.ProductDTO;
import com.titanium.product.api.request.AuditProductRequest;
import com.titanium.product.api.request.CreateProductRequest;
import com.titanium.product.api.response.ApiResponse;

/**
 * 产品API Feign客户端
 * 定义产品服务的API接口，供其他服务调用
 */
@FeignClient(name = "titanium-product-service", path = "/api/products")
public interface ProductApi {

    /** 创建产品 */
    @PostMapping
    ApiResponse<String> createProduct(@RequestBody CreateProductRequest request,
                                      @RequestHeader("X-Tenant-ID") String tenantId);

    /** 根据ID查询产品 */
    @GetMapping("/{productId}")
    ApiResponse<ProductDTO> getProductById(@PathVariable("productId") String productId,
                                           @RequestHeader("X-Tenant-ID") String tenantId);

    /** 提交产品审核 */
    @PutMapping("/{productId}/submit-audit")
    ApiResponse<Void> submitForAudit(@PathVariable("productId") String productId,
                                      @RequestHeader("X-Tenant-ID") String tenantId);

    /** 审核产品 */
    @PutMapping("/{productId}/audit")
    ApiResponse<Void> auditProduct(@PathVariable("productId") String productId,
                                    @RequestBody AuditProductRequest request,
                                    @RequestHeader("X-Tenant-ID") String tenantId);

    /** 驳回产品审核 */
    @PutMapping("/{productId}/reject")
    ApiResponse<Void> rejectAudit(@PathVariable("productId") String productId,
                                   @RequestBody AuditProductRequest request,
                                   @RequestHeader("X-Tenant-ID") String tenantId);

    /** 下架产品 */
    @PutMapping("/{productId}/invalid")
    ApiResponse<Void> invalidateProduct(@PathVariable("productId") String productId,
                                         @RequestHeader("X-Tenant-ID") String tenantId);

    /** 查询产品出单流程配置（供Policy域调用） */
    @GetMapping("/{productId}/issuance-config")
    ApiResponse<Object> getIssuanceConfig(@PathVariable("productId") String productId,
                                           @RequestHeader("X-Tenant-ID") String tenantId);

    /** 查询产品核保配置（供Underwriting域调用） */
    @GetMapping("/{productId}/underwriting-config")
    ApiResponse<Object> getUnderwritingConfig(@PathVariable("productId") String productId,
                                               @RequestHeader("X-Tenant-ID") String tenantId);

    /** 查询产品保单形态配置（供Policy域调用） */
    @GetMapping("/{productId}/policy-form-config")
    ApiResponse<Object> getPolicyFormConfig(@PathVariable("productId") String productId,
                                             @RequestHeader("X-Tenant-ID") String tenantId);
}
