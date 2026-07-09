package com.titanium.product.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.product.api.dto.PricingBasicRuleDTO;
import com.titanium.product.api.dto.ProductDTO;
import com.titanium.product.api.response.ApiResponse;
import com.titanium.product.application.command.ProductCommandAppService;
import com.titanium.product.application.query.ProductQueryAppService;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.web.mapper.ProductWebMapper;
import com.titanium.product.web.request.AuditProductRequest;
import com.titanium.product.web.request.CreateProductRequest;

import lombok.RequiredArgsConstructor;

/**
 * 产品控制器（后台/端上 HTTP 入口）
 * <p>
 * 面向管理后台/端上，路径 {@code /web/v1/products}，入参 {@code CreateProductRequest} 等 web Request，
 * <b>不再 implements ProductApi</b>（远程契约由 {@link com.titanium.product.web.provider.ProductApiProvider}
 * 承接）。表现层经 {@link ProductWebMapper} 把 Request 转成 CQRS 命令交
 * {@link ProductCommandAppService}，读入口查读模型交 {@link ProductQueryAppService}。web 可依赖
 * command/query，但不碰聚合根。与 Provider 平行收敛到同一应用层门面。
 * </p>
 */
@RestController
@RequestMapping("/web/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCommandAppService productCommandAppService;

    private final ProductQueryAppService   productQueryAppService;

    private final ProductWebMapper         productWebMapper;

    /**
     * 创建产品
     */
    @PostMapping
    public ApiResponse<String> createProduct(@RequestBody CreateProductRequest request,
                                             @RequestHeader("X-Tenant-ID") String tenantId) {
        // 协议转换：HTTP Request → 领域命令，收敛到同一应用层门面
        CreateProductCommand command = productWebMapper.toCommand(request, tenantId);
        String productId = productCommandAppService.createProduct(command);
        return ApiResponse.success(productId);
    }

    /**
     * 根据ID查询产品
     */
    @GetMapping("/{productId}")
    public ApiResponse<ProductDTO> getProductById(@PathVariable("productId") String productId,
                                                  @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(productWebMapper.toProductDTO(result));
    }

    /**
     * 提交产品审核
     */
    @PutMapping("/{productId}/submit-audit")
    public ApiResponse<Void> submitForAudit(@PathVariable("productId") String productId,
                                            @RequestHeader("X-Tenant-ID") String tenantId) {
        productCommandAppService.submitForAudit(productId);
        return ApiResponse.success(null);
    }

    /**
     * 审核产品
     */
    @PutMapping("/{productId}/audit")
    public ApiResponse<Void> auditProduct(@PathVariable("productId") String productId,
                                          @RequestBody AuditProductRequest request,
                                          @RequestHeader("X-Tenant-ID") String tenantId) {
        productCommandAppService.auditProduct(productWebMapper.toAuditCommand(productId, request));
        return ApiResponse.success(null);
    }

    /**
     * 驳回产品审核
     */
    @PutMapping("/{productId}/reject")
    public ApiResponse<Void> rejectAudit(@PathVariable("productId") String productId,
                                         @RequestBody AuditProductRequest request,
                                         @RequestHeader("X-Tenant-ID") String tenantId) {
        productCommandAppService.rejectAudit(productWebMapper.toRejectCommand(productId, request));
        return ApiResponse.success(null);
    }

    /**
     * 下架产品
     */
    @PutMapping("/{productId}/invalid")
    public ApiResponse<Void> invalidateProduct(@PathVariable("productId") String productId,
                                               @RequestHeader("X-Tenant-ID") String tenantId) {
        productCommandAppService.invalidateProduct(productId);
        return ApiResponse.success(null);
    }

    /**
     * 查询产品出单流程配置
     */
    @GetMapping("/{productId}/issuance-config")
    public ApiResponse<Object> getIssuanceConfig(@PathVariable("productId") String productId,
                                                 @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(result.getIssuanceProcessConfig());
    }

    /**
     * 查询产品核保配置
     */
    @GetMapping("/{productId}/underwriting-config")
    public ApiResponse<Object> getUnderwritingConfig(@PathVariable("productId") String productId,
                                                     @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(result.getUnderwritingConfig());
    }

    /**
     * 查询产品保单形态配置
     */
    @GetMapping("/{productId}/policy-form-config")
    public ApiResponse<Object> getPolicyFormConfig(@PathVariable("productId") String productId,
                                                   @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(result.getPolicyFormConfig());
    }

    /**
     * 查询产品定价基础规则
     */
    @GetMapping("/{productId}/pricing-rule")
    public ApiResponse<PricingBasicRuleDTO> getPricingRule(@PathVariable("productId") String productId,
                                                           @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(productWebMapper.toPricingRuleDTO(result.getPricingBasicRule()));
    }
}
