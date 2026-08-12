package com.titanium.product.web.provider;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductApi;
import com.titanium.product.api.request.AuditProductRequest;
import com.titanium.product.api.request.CreateProductRequest;
import com.titanium.product.api.response.PricingBasicRuleResponse;
import com.titanium.product.api.response.ProductResponse;
import com.titanium.product.application.command.ProductCommandAppService;
import com.titanium.product.application.query.ProductQueryAppService;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.web.mapper.ProductWebMapper;

import lombok.RequiredArgsConstructor;

/**
 * 产品契约实现（Provider）
 * <p>
 * 承接 {@link ProductApi} Feign 契约，面向其它微服务的远程调用。路径由 {@link ProductApi} 的
 * {@code @FeignClient(path="/api/v1/products")} 唯一定义，本类通过 {@code implements} 继承，
 * <b>不重复标注、不篡改</b>。职责仅为协议转换（DTO → 领域命令 / 读模型结果 → DTO）+ 调用应用层门面，
 * 零业务逻辑。与面向后台/端上的 {@code ProductController} 平行收敛到同一应用层门面。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductApiProvider implements ProductApi {

    private final ProductCommandAppService productCommandAppService;

    private final ProductQueryAppService   productQueryAppService;

    private final ProductWebMapper         productWebMapper;

    @Override
    public ApiResponse<String> createProduct(CreateProductRequest dto, String tenantId) {
        // 协议转换：远程 DTO → 领域命令，收敛到同一应用层门面
        CreateProductCommand command = productWebMapper.toCommand(dto, tenantId);
        String productId = productCommandAppService.createProduct(command);
        return ApiResponse.success(productId);
    }

    @Override
    public ApiResponse<ProductResponse> getProductById(String productId, String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(productWebMapper.toProductResponse(result));
    }

    @Override
    public ApiResponse<Void> submitForAudit(String productId, String tenantId) {
        productCommandAppService.submitForAudit(productId);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> auditProduct(String productId, AuditProductRequest dto, String tenantId) {
        productCommandAppService.auditProduct(productWebMapper.toAuditCommand(productId, dto));
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> rejectAudit(String productId, AuditProductRequest dto, String tenantId) {
        productCommandAppService.rejectAudit(productWebMapper.toRejectCommand(productId, dto));
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> invalidateProduct(String productId, String tenantId) {
        productCommandAppService.invalidateProduct(productId);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Object> getIssuanceConfig(String productId, String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(result.getIssuanceProcessConfig());
    }

    @Override
    public ApiResponse<Object> getUnderwritingConfig(String productId, String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(result.getUnderwritingConfig());
    }

    @Override
    public ApiResponse<Object> getPolicyFormConfig(String productId, String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(result.getPolicyFormConfig());
    }

    @Override
    public ApiResponse<PricingBasicRuleResponse> getPricingRule(String productId, String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId);
        return ApiResponse.success(productWebMapper.toPricingRuleResponse(result));
    }

    @Override
    public ApiResponse<List<com.titanium.product.api.response.ProductClauseResponse>> getProductClauses(
            String productId, String tenantId) {
        List<com.titanium.product.api.response.ProductClauseResponse> responses = productQueryAppService
                .queryProductClauses(productId).stream()
                .map(productWebMapper::toProductClauseResponse)
                .toList();
        return ApiResponse.success(responses);
    }
}
