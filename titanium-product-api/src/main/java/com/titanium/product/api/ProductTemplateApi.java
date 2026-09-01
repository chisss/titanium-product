package com.titanium.product.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.response.product.ProductTemplateResponse;

/**
 * 产品模板 Feign 客户端
 * 供其他微服务调用
 */
@FeignClient(name = "titanium-product-service", contextId = "productTemplateApi", path = "/api/v1/product-templates")
public interface ProductTemplateApi {

    /**
     * 根据产品ID查询产品模板
     */
    @GetMapping("/by-product/{productId}")
    ApiResponse<ProductTemplateResponse> getByProductId(@PathVariable("productId") String productId,
                                                    @RequestHeader("X-Tenant-ID") String tenantId);

    /**
     * 根据模板编码查询产品模板
     */
    @GetMapping("/by-code/{templateCode}")
    ApiResponse<ProductTemplateResponse> getByCode(@PathVariable("templateCode") String templateCode,
                                               @RequestHeader("X-Tenant-ID") String tenantId);

    /**
     * 根据模板ID查询产品模板
     */
    @GetMapping("/{templateId}")
    ApiResponse<ProductTemplateResponse> getById(@PathVariable("templateId") String templateId,
                                            @RequestHeader("X-Tenant-ID") String tenantId);
}
