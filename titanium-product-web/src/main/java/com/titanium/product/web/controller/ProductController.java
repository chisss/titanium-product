package com.titanium.product.web.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.response.config.IssuanceProcessConfigResponse;
import com.titanium.product.api.response.config.PolicyFormConfigResponse;
import com.titanium.product.api.response.config.UnderwritingConfigResponse;
import com.titanium.product.api.response.pricing.PricingBasicRuleResponse;
import com.titanium.product.api.response.product.InsuranceProductDefinitionResponse;
import com.titanium.product.api.response.product.ProductResponse;
import com.titanium.product.application.command.ProductCommandAppService;
import com.titanium.product.application.query.ProductQueryAppService;
import com.titanium.product.command.CreateProductCommand;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.web.catalog.InsuranceProductDefinitionCatalog;
import com.titanium.product.web.dto.AuditProductDTO;
import com.titanium.product.web.dto.ConfigureLifeProductDTO;
import com.titanium.product.web.dto.CreateProductDTO;
import com.titanium.product.web.mapper.ProductWebMapper;

import lombok.RequiredArgsConstructor;

/**
 * 产品控制器（后台/端上 HTTP 入口）
 * <p>
 * 面向管理后台/端上，路径 {@code /web/v1/products}，入参 {@code CreateProductDTO} 等 web Request，
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

    private final InsuranceProductDefinitionCatalog definitionCatalog;

    /**
     * 查询九类险种的标准产品定义目录，供后台动态渲染专属字段。
     */
    @GetMapping("/definitions")
    public ApiResponse<List<InsuranceProductDefinitionResponse>> listDefinitions() {
        return ApiResponse.success(definitionCatalog.list());
    }

    /**
     * 创建产品
     */
    @PostMapping
    public ApiResponse<String> createProduct(@RequestBody CreateProductDTO request,
                                             @RequestHeader("X-Tenant-ID") String tenantId) {
        // 协议转换：HTTP Request → 领域命令，收敛到同一应用层门面
        CreateProductCommand command = productWebMapper.toCommand(request, tenantId);
        String productId = productCommandAppService.createProduct(command);
        return ApiResponse.success(productId);
    }

    /**
     * 分页查询产品列表
     * <p>
     * 支持按产品名称（模糊）、险种类型、状态任意组合过滤；险种/状态以编码传入，非法或空值表示不限。
     * 页码 {@code pageNum} 从 0 开始。
     * </p>
     *
     * @param productName 产品名称（模糊匹配，可选）
     * @param insuranceType 险种类型编码（可选）
     * @param status 产品状态编码（可选）
     * @param pageNum 页码（从 0 开始，默认 0）
     * @param pageSize 每页条数（默认 10）
     * @param tenantId 租户ID（请求头）
     * @return 分页产品列表（对外 DTO）
     */
    @GetMapping
    public ApiResponse<Page<ProductResponse>> listProducts(@RequestParam(required = false) String productName,
                                                      @RequestParam(required = false) String insuranceType,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "0") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestHeader("X-Tenant-ID") String tenantId) {
        InsuranceProductType type =
                insuranceType != null ? InsuranceProductType.fromCode(insuranceType) : null;
        ProductEnum.ProductStatus productStatus =
                status != null ? ProductEnum.ProductStatus.fromCode(status) : null;
        Page<ProductQueryResult> page = productQueryAppService.queryProductByCondition(
                productName, null, type, productStatus, pageNum, pageSize, tenantId);
        return ApiResponse.success(page.map(productWebMapper::toProductResponse));
    }

    /**
     * 根据ID查询产品
     */
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable("productId") String productId,
                                                  @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, tenantId);
        return ApiResponse.success(productWebMapper.toProductResponse(result));
    }

    /**
     * 查询产品绑定的条款清单
     * <p>
     * 读取产品条款关联读模型（{@code t_product_clause_rel_view}），返回产品绑定的条款ID/版本/是否主条款。
     * 供后台产品详情页按电子保单形态呈现条款与保障责任（前端据条款ID再取条款域详情与保障责任）。
     * </p>
     *
     * @param productId 产品ID
     * @param tenantId 租户ID（请求头）
     * @return 产品绑定条款关联清单
     */
    @GetMapping("/{productId}/clauses")
    public ApiResponse<List<ProductClauseQueryResult>> getProductClauses(@PathVariable("productId") String productId,
                                                                         @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(productQueryAppService.queryProductClauses(productId));
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
                                          @RequestBody AuditProductDTO request,
                                          @RequestHeader("X-Tenant-ID") String tenantId) {
        productCommandAppService.auditProduct(productWebMapper.toAuditCommand(productId, request));
        return ApiResponse.success(null);
    }

    /**
     * 驳回产品审核
     */
    @PutMapping("/{productId}/reject")
    public ApiResponse<Void> rejectAudit(@PathVariable("productId") String productId,
                                         @RequestBody AuditProductDTO request,
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
    public ApiResponse<IssuanceProcessConfigResponse> getIssuanceConfig(
            @PathVariable("productId") String productId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, tenantId);
        return ApiResponse.success(productWebMapper.toIssuanceProcessConfigResponse(result.getIssuanceProcessConfig()));
    }

    /**
     * 查询产品核保配置
     */
    @GetMapping("/{productId}/underwriting-config")
    public ApiResponse<UnderwritingConfigResponse> getUnderwritingConfig(
            @PathVariable("productId") String productId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, tenantId);
        return ApiResponse.success(productWebMapper.toUnderwritingConfigResponse(result.getUnderwritingConfig()));
    }

    /**
     * 查询产品保单形态配置
     */
    @GetMapping("/{productId}/policy-form-config")
    public ApiResponse<PolicyFormConfigResponse> getPolicyFormConfig(
            @PathVariable("productId") String productId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, tenantId);
        return ApiResponse.success(productWebMapper.toPolicyFormConfigResponse(result.getPolicyFormConfig()));
    }

    /**
     * 查询产品定价基础规则
     */
    @GetMapping("/{productId}/pricing-rule")
    public ApiResponse<PricingBasicRuleResponse> getPricingRule(@PathVariable("productId") String productId,
                                                           @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, tenantId);
        return ApiResponse.success(productWebMapper.toPricingRuleResponse(result));
    }

    /**
     * 配置寿险产品规格
     * <p>
     * 为产品关联的产品模板配置寿险专属规格（投保年龄范围/保额范围/缴费期选项/保障期选项）。
     * web 层经 {@link ProductWebMapper} 把请求转成领域值对象 {@code LifeProductSpec}，交
     * {@link ProductCommandAppService} 定位模板并派发寿险规格配置命令。
     * </p>
     *
     * @param productId 产品ID
     * @param request 配置寿险产品请求
     * @param tenantId 租户ID（请求头）
     * @return 空响应
     */
    @PostMapping("/{productId}/life-config")
    public ApiResponse<Void> configureLifeProduct(@PathVariable("productId") String productId,
                                                  @RequestBody ConfigureLifeProductDTO request,
                                                  @RequestHeader("X-Tenant-ID") String tenantId) {
        LifeProductSpec lifeProductSpec = productWebMapper.toLifeProductSpec(request);
        productCommandAppService.configureLifeProduct(productId, lifeProductSpec, tenantId);
        return ApiResponse.success(null);
    }

    /**
     * 查询寿险产品规格
     * <p>
     * 返回产品关联模板已配置的寿险规格（投保年龄/保额范围/缴费期/保障期）；未配置时 data 为 {@code null}。
     * </p>
     *
     * @param productId 产品ID
     * @param tenantId 租户ID（请求头）
     * @return 寿险产品规格
     */
    @GetMapping("/{productId}/life-config")
    public ApiResponse<LifeProductSpec> getLifeProductSpec(@PathVariable("productId") String productId,
                                                           @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(productQueryAppService.queryLifeProductSpec(productId, tenantId));
    }
}
