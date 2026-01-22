package com.titanium.product.api;

import com.titanium.product.api.dto.ProductDTO;
import com.titanium.product.api.request.CreateProductRequest;
import com.titanium.product.api.request.AuditProductRequest;
import com.titanium.product.api.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 产品API Feign客户端
 * 定义产品服务的API接口，供其他服务调用
 */
@FeignClient(name = "titanium-product-service", path = "/api/products")
public interface ProductApi {
    /**
     * 创建产品
     * @param request 创建产品请求
     * @param tenantId 租户ID
     * @return API响应
     */
    @PostMapping
    ApiResponse<String> createProduct(@RequestBody CreateProductRequest request, 
                                     @RequestHeader("X-Tenant-ID") String tenantId);
    
    /**
     * 根据ID查询产品
     * @param productId 产品ID
     * @param tenantId 租户ID
     * @return API响应
     */
    @GetMapping("/{productId}")
    ApiResponse<ProductDTO> getProductById(@PathVariable("productId") String productId, 
                                         @RequestHeader("X-Tenant-ID") String tenantId);
    
    /**
     * 审核产品
     * @param productId 产品ID
     * @param request 审核产品请求
     * @param tenantId 租户ID
     * @return API响应
     */
    @PutMapping("/{productId}/audit")
    ApiResponse<Void> auditProduct(@PathVariable("productId") String productId, 
                                  @RequestBody AuditProductRequest request, 
                                  @RequestHeader("X-Tenant-ID") String tenantId);
    
    /**
     * 下架产品
     * @param productId 产品ID
     * @param tenantId 租户ID
     * @return API响应
     */
    @PutMapping("/{productId}/invalid")
    ApiResponse<Void> invalidateProduct(@PathVariable("productId") String productId, 
                                       @RequestHeader("X-Tenant-ID") String tenantId);
}