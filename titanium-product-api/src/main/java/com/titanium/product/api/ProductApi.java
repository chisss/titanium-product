package com.titanium.product.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.titanium.product.api.request.AuditProductRequest;
import com.titanium.product.api.request.CreateProductRequest;
import com.titanium.product.api.response.ApiResponse;
import com.titanium.product.api.response.PricingBasicRuleResponse;
import com.titanium.product.api.response.ProductResponse;

/**
 * 产品聚合对外契约（Feign）
 * <p>
 * 命名主键为聚合根 {@code Product}，承载产品聚合的远程调用；契约入参统一为 api 的 {@code DTO}
 * （自包含、领域枚举以 String 承载），由 web 层 {@code ProductApiProvider} 实现，DTO→领域命令
 * 的翻译在 web/provider 完成。契约路径遵从内部服务远程调用规约 {@code /api/v1/products}。
 * </p>
 */
@FeignClient(name = "titanium-product-service", contextId = "productApi", path = "/api/v1/products")
public interface ProductApi {

    /** 创建产品 */
    @PostMapping
    ApiResponse<String> createProduct(@RequestBody CreateProductRequest dto,
                                      @RequestHeader("X-Tenant-ID") String tenantId);

    /** 根据ID查询产品 */
    @GetMapping("/{productId}")
    ApiResponse<ProductResponse> getProductById(@PathVariable("productId") String productId,
                                           @RequestHeader("X-Tenant-ID") String tenantId);

    /** 提交产品审核 */
    @PutMapping("/{productId}/submit-audit")
    ApiResponse<Void> submitForAudit(@PathVariable("productId") String productId,
                                      @RequestHeader("X-Tenant-ID") String tenantId);

    /** 审核产品 */
    @PutMapping("/{productId}/audit")
    ApiResponse<Void> auditProduct(@PathVariable("productId") String productId,
                                    @RequestBody AuditProductRequest dto,
                                    @RequestHeader("X-Tenant-ID") String tenantId);

    /** 驳回产品审核 */
    @PutMapping("/{productId}/reject")
    ApiResponse<Void> rejectAudit(@PathVariable("productId") String productId,
                                   @RequestBody AuditProductRequest dto,
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

    /** 查询产品定价基础规则（供Billing域保费计算调用） */
    @GetMapping("/{productId}/pricing-rule")
    ApiResponse<PricingBasicRuleResponse> getPricingRule(@PathVariable("productId") String productId,
                                                    @RequestHeader("X-Tenant-ID") String tenantId);
}
