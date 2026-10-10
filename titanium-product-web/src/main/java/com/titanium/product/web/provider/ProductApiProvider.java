package com.titanium.product.web.provider;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.context.RequestContextHolder;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductApi;
import com.titanium.product.api.request.product.AuditProductRequest;
import com.titanium.product.api.request.product.CreateProductRequest;
import com.titanium.product.api.request.product.UpdateCoverageSumInsuredConfigRequest;
import com.titanium.product.api.request.product.UpdateProductClauseRelRequest;
import com.titanium.product.api.response.clause.ProductClauseResponse;
import com.titanium.product.api.response.config.CoverageSumInsuredConfigResponse;
import com.titanium.product.api.response.config.IssuanceProcessConfigResponse;
import com.titanium.product.api.response.config.PolicyFormConfigResponse;
import com.titanium.product.api.response.config.UnderwritingConfigResponse;
import com.titanium.product.api.response.pricing.PricingBasicRuleResponse;
import com.titanium.product.api.response.product.InsuranceProductDefinitionResponse;
import com.titanium.product.api.response.product.ProductResponse;
import com.titanium.product.application.command.ProductCommandAppService;
import com.titanium.product.application.query.ProductQueryAppService;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.web.catalog.InsuranceProductDefinitionCatalog;
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

    private final InsuranceProductDefinitionCatalog definitionCatalog;

    @Override
    public ApiResponse<List<InsuranceProductDefinitionResponse>> listDefinitions() {
        return ApiResponse.success(definitionCatalog.list());
    }

    @Override
    public ApiResponse<String> createProduct(CreateProductRequest dto) {
        // 协议转换：远程 DTO → 领域命令，收敛到同一应用层门面
        CreateProductCommand command = productWebMapper.toCommand(dto, RequestContextHolder.requireTenantId());
        String productId = productCommandAppService.createProduct(command);
        return ApiResponse.success(productId);
    }

    @Override
    public ApiResponse<ProductResponse> getProductById(String productId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, RequestContextHolder.requireTenantId());
        return ApiResponse.success(productWebMapper.toProductResponse(result));
    }

    @Override
    public ApiResponse<Void> submitForAudit(String productId) {
        productCommandAppService.submitForAudit(productId);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> auditProduct(String productId, AuditProductRequest dto) {
        productCommandAppService.auditProduct(productWebMapper.toAuditCommand(productId, dto));
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> rejectAudit(String productId, AuditProductRequest dto) {
        productCommandAppService.rejectAudit(productWebMapper.toRejectCommand(productId, dto));
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> invalidateProduct(String productId) {
        productCommandAppService.invalidateProduct(productId);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<IssuanceProcessConfigResponse> getIssuanceConfig(String productId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, RequestContextHolder.requireTenantId());
        return ApiResponse.success(productWebMapper.toIssuanceProcessConfigResponse(result.getIssuanceProcessConfig()));
    }

    @Override
    public ApiResponse<UnderwritingConfigResponse> getUnderwritingConfig(String productId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, RequestContextHolder.requireTenantId());
        return ApiResponse.success(productWebMapper.toUnderwritingConfigResponse(result.getUnderwritingConfig()));
    }

    @Override
    public ApiResponse<UnderwritingConfigResponse> getUnderwritingConfigByCode(String productCode) {
        ProductQueryResult result = productQueryAppService.queryProductByCode(productCode, RequestContextHolder.requireTenantId());
        return ApiResponse.success(productWebMapper.toUnderwritingConfigResponse(result.getUnderwritingConfig()));
    }

    @Override
    public ApiResponse<PolicyFormConfigResponse> getPolicyFormConfig(String productId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, RequestContextHolder.requireTenantId());
        return ApiResponse.success(productWebMapper.toPolicyFormConfigResponse(result.getPolicyFormConfig()));
    }

    @Override
    public ApiResponse<PricingBasicRuleResponse> getPricingRule(String productId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, RequestContextHolder.requireTenantId());
        return ApiResponse.success(productWebMapper.toPricingRuleResponse(result));
    }

    @Override
    public ApiResponse<List<ProductClauseResponse>> getProductClauses(
            String productId) {
        List<ProductClauseResponse> responses = productQueryAppService
                .queryProductClauses(productId).stream()
                .map(productWebMapper::toProductClauseResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    /**
     * 整体替换产品绑定的条款关联（远程写入口）
     * <p>
     * 与后台/端上的 {@code PUT /web/v1/products/{productId}/clauses} <b>平行</b>：各自经同一
     * {@link ProductWebMapper} 翻译成同一个 {@code UpdateProductClauseRelCommand}，再汇入同一个 application
     * 薄门面 {@code ProductCommandAppService#updateProductClauseRel}。本类不复制任何业务规则——
     * 「仅 DRAFT 可改」在聚合根、条款 ACTIVE 门禁在应用层跨域校验。
     * </p>
     */
    @Override
    public ApiResponse<Void> updateProductClauseRel(String productId,
                                                    UpdateProductClauseRelRequest request) {
        // 协议转换：远程 Request → 领域命令，收敛到同一应用层门面
        productCommandAppService.updateProductClauseRel(
                productWebMapper.toCommand(request, productId, RequestContextHolder.requireTenantId()));
        return ApiResponse.success(null);
    }

    /**
     * 查询产品的责任级保额档次配置（远程读入口）
     * <p>
     * 与后台/端上的 {@code GET /web/v1/products/{productId}/coverage-sum-insured-configs} 平行，
     * 共用同一个 {@link ProductWebMapper} 映射方法与同一个 application 薄门面。
     * 未配置时返回空列表（不是 404——「尚未配置」是正常状态）。
     * </p>
     */
    @Override
    public ApiResponse<List<CoverageSumInsuredConfigResponse>> getCoverageSumInsuredConfigs(
            String productId) {
        List<CoverageSumInsuredConfigResponse> responses = productQueryAppService
                .queryCoverageSumInsuredConfigs(productId).stream()
                .map(productWebMapper::toCoverageSumInsuredResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    /**
     * 整体替换产品的责任级保额档次配置（远程写入口）
     * <p>
     * 与后台/端上的 {@code PUT /web/v1/products/{productId}/coverage-sum-insured-configs}
     * <b>平行</b>：各自经同一 {@link ProductWebMapper} 翻译成同一个
     * {@code UpdateCoverageSumInsuredConfigCommand}，再汇入同一个 application 薄门面
     * {@code ProductCommandAppService#updateCoverageSumInsuredConfig}。本类不复制任何业务规则——
     * 「仅 DRAFT 可配」在聚合根、责任编码归属门禁在应用层跨域校验。
     * </p>
     */
    @Override
    public ApiResponse<Void> updateCoverageSumInsuredConfig(
            String productId, UpdateCoverageSumInsuredConfigRequest request) {
        productCommandAppService.updateCoverageSumInsuredConfig(
                productWebMapper.toCommand(request, productId, RequestContextHolder.requireTenantId()));
        return ApiResponse.success(null);
    }
}
