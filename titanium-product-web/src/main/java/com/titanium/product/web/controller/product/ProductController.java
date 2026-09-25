package com.titanium.product.web.controller.product;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.context.RequestContextHolder;
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
import com.titanium.product.command.ReviseProductCommand;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.web.catalog.InsuranceProductDefinitionCatalog;
import com.titanium.product.web.dto.product.AuditProductDTO;
import com.titanium.product.web.dto.product.ConfigureLifeProductDTO;
import com.titanium.product.web.dto.product.CreateProductDTO;
import com.titanium.product.web.dto.product.ProductVersionVO;
import com.titanium.product.web.dto.product.ReviseProductDTO;
import com.titanium.product.web.export.ProductCsvExporter;
import com.titanium.product.web.mapper.ProductWebMapper;

import jakarta.validation.Valid;
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

    /** 导出单次取数上限：满足条件的记录一次性拉取，防止无界导出拖垮下游 */
    private static final int EXPORT_MAX_ROWS = 10000;

    /**
     * 查询九类险种的标准产品定义目录，供后台动态渲染专属字段。
     */
    @GetMapping("/definitions")
    public ApiResponse<List<InsuranceProductDefinitionResponse>> listDefinitions() {
        return ApiResponse.success(definitionCatalog.list());
    }

    /**
     * 查询同一产品编码下的全部版本（版本历史）。
     * <p>
     * 🔴 本端点是**固定子路径**，与 {@code /definitions} 同族，必须显式登记在兜底
     * {@code /{productId}} 之前：否则会被当作 {@code productId="versions"} 去按ID查产品。
     * </p>
     * <p>
     * 版本线 = 同 {@code productCode} 的多个产品：修订不改写当前生效版本，而是继承编码派生新版本
     * （见 {@code InsuranceProduct.handle(ReviseProductCommand)}）⇒ 按编码即可取回整条版本线。
     * 出参刻意只带定位与状态字段，配置对比由调用方对选中的版本各自取详情完成。
     * </p>
     *
     * @param productCode 产品编码（版本线归组键，必填）
     * @return 该编码下的全部版本（新版在前）
     */
    @GetMapping("/versions")
    public ApiResponse<List<ProductVersionVO>> listVersions(@RequestParam String productCode) {
        String tenantId = RequestContextHolder.requireTenantId();
        return ApiResponse.success(productQueryAppService.queryProductVersions(productCode, tenantId)
                .stream()
                .map(productWebMapper::toProductVersionVO)
                .toList());
    }

    /**
     * 创建产品
     */
    @PostMapping
    public ApiResponse<String> createProduct(@Valid @RequestBody CreateProductDTO request) {
        String tenantId = RequestContextHolder.requireTenantId();
        // 协议转换：HTTP Request → 领域命令，收敛到同一应用层门面
        CreateProductCommand command = productWebMapper.toCommand(request, tenantId);
        String productId = productCommandAppService.createProduct(command);
        return ApiResponse.success(productId);
    }

    /**
     * 修订产品（仅 EFFECTIVE 可修订，生成新版本 DRAFT）
     * <p>
     * 修订不改写当前生效版本，而以新 {@code newProductId} 创建独立聚合（版本号递增，如 V1.0 → V2.0），
     * 模板ID/产品代码/附加险搭配/租户由聚合根继承原产品。存量产品补配核保配置等场景走本端点。
     * </p>
     *
     * @param productId 被修订的当前生效产品ID
     * @param request 修订请求（承载新版本完整配置）
     * @return 新版本产品ID
     */
    @PostMapping("/{productId}/revise")
    public ApiResponse<String> reviseProduct(@PathVariable String productId,
                                             @Valid @RequestBody ReviseProductDTO request) {
        String tenantId = RequestContextHolder.requireTenantId();
        // 协议转换：HTTP Request → 领域命令，收敛到同一应用层门面
        ReviseProductCommand command = productWebMapper.toCommand(request, productId, tenantId);
        String newProductId = productCommandAppService.reviseProduct(command);
        return ApiResponse.success(newProductId);
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
     * @return 分页产品列表（对外 DTO）
     */
    @GetMapping
    public ApiResponse<Page<ProductResponse>> listProducts(@RequestParam(required = false) String productName,
                                                      @RequestParam(required = false) String insuranceType,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "0") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize) {
        String tenantId = RequestContextHolder.requireTenantId();
        Page<ProductQueryResult> page =
                queryProducts(productName, insuranceType, status, pageNum, pageSize, tenantId);
        return ApiResponse.success(page.map(productWebMapper::toProductResponse));
    }

    /**
     * 导出产品列表（CSV 附件）。
     * <p>
     * 与 {@link #listProducts} 共用筛选条件（名称/险种/状态），但**不分页**——按上限一次性取出满足条件的
     * 全量产品并序列化为 CSV。响应为二进制附件（非 JSON 信封），供 BFF 二进制透传下载。
     * </p>
     *
     * @param productName 产品名称（模糊匹配，可选）
     * @param insuranceType 险种类型编码（可选）
     * @param status 产品状态编码（可选）
     * @return CSV 附件字节流（UTF-8 含 BOM，Excel 可直接打开）
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProducts(@RequestParam(required = false) String productName,
                                                 @RequestParam(required = false) String insuranceType,
                                                 @RequestParam(required = false) String status) {
        String tenantId = RequestContextHolder.requireTenantId();
        Page<ProductQueryResult> page =
                queryProducts(productName, insuranceType, status, 0, EXPORT_MAX_ROWS, tenantId);
        // RFC 5987 编码文件名，避免中文文件名在部分浏览器乱码
        String filename = URLEncoder.encode("产品列表.csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(ProductCsvExporter.toCsv(page.getContent()));
    }

    /**
     * 按筛选条件查询产品（列表与导出共用）。
     * <p>导出侧以 {@link #EXPORT_MAX_ROWS} 作 pageSize 取全量；名称/险种/状态三者相互独立，空即不限。</p>
     */
    private Page<ProductQueryResult> queryProducts(String productName, String insuranceType, String status,
                                                   int pageNum, int pageSize, String tenantId) {
        InsuranceProductType type = insuranceType != null ? InsuranceProductType.fromCode(insuranceType) : null;
        ProductEnum.ProductStatus productStatus =
                status != null ? ProductEnum.ProductStatus.fromCode(status) : null;
        return productQueryAppService.queryProductByCondition(
                productName, null, type, productStatus, pageNum, pageSize, tenantId);
    }

    /**
     * 根据ID查询产品
     */
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable String productId) {
        String tenantId = RequestContextHolder.requireTenantId();
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
     * @return 产品绑定条款关联清单
     */
    @GetMapping("/{productId}/clauses")
    public ApiResponse<List<ProductClauseQueryResult>> getProductClauses(@PathVariable String productId) {
        return ApiResponse.success(productQueryAppService.queryProductClauses(productId));
    }

    /**
     * 提交产品审核
     */
    @PutMapping("/{productId}/submit-audit")
    public ApiResponse<Void> submitForAudit(@PathVariable String productId) {
        productCommandAppService.submitForAudit(productId);
        return ApiResponse.success(null);
    }

    /**
     * 审核产品
     */
    @PutMapping("/{productId}/audit")
    public ApiResponse<Void> auditProduct(@PathVariable String productId,
                                          @Valid @RequestBody AuditProductDTO request) {
        productCommandAppService.auditProduct(productWebMapper.toAuditCommand(productId, request));
        return ApiResponse.success(null);
    }

    /**
     * 驳回产品审核
     */
    @PutMapping("/{productId}/reject")
    public ApiResponse<Void> rejectAudit(@PathVariable String productId,
                                         @Valid @RequestBody AuditProductDTO request) {
        productCommandAppService.rejectAudit(productWebMapper.toRejectCommand(productId, request));
        return ApiResponse.success(null);
    }

    /**
     * 下架产品
     */
    @PutMapping("/{productId}/invalid")
    public ApiResponse<Void> invalidateProduct(@PathVariable String productId) {
        productCommandAppService.invalidateProduct(productId);
        return ApiResponse.success(null);
    }

    /**
     * 查询产品出单流程配置
     */
    @GetMapping("/{productId}/issuance-config")
    public ApiResponse<IssuanceProcessConfigResponse> getIssuanceConfig(
            @PathVariable String productId) {
        String tenantId = RequestContextHolder.requireTenantId();
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, tenantId);
        return ApiResponse.success(productWebMapper.toIssuanceProcessConfigResponse(result.getIssuanceProcessConfig()));
    }

    /**
     * 查询产品核保配置
     */
    @GetMapping("/{productId}/underwriting-config")
    public ApiResponse<UnderwritingConfigResponse> getUnderwritingConfig(
            @PathVariable String productId) {
        String tenantId = RequestContextHolder.requireTenantId();
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, tenantId);
        return ApiResponse.success(productWebMapper.toUnderwritingConfigResponse(result.getUnderwritingConfig()));
    }

    /**
     * 查询产品保单形态配置
     */
    @GetMapping("/{productId}/policy-form-config")
    public ApiResponse<PolicyFormConfigResponse> getPolicyFormConfig(
            @PathVariable String productId) {
        String tenantId = RequestContextHolder.requireTenantId();
        ProductQueryResult result = productQueryAppService.queryProductDetail(productId, tenantId);
        return ApiResponse.success(productWebMapper.toPolicyFormConfigResponse(result.getPolicyFormConfig()));
    }

    /**
     * 查询产品定价基础规则
     */
    @GetMapping("/{productId}/pricing-rule")
    public ApiResponse<PricingBasicRuleResponse> getPricingRule(@PathVariable String productId) {
        String tenantId = RequestContextHolder.requireTenantId();
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
     * @return 空响应
     */
    @PostMapping("/{productId}/life-config")
    public ApiResponse<Void> configureLifeProduct(@PathVariable String productId,
                                                  @Valid @RequestBody ConfigureLifeProductDTO request) {
        String tenantId = RequestContextHolder.requireTenantId();
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
     * @return 寿险产品规格
     */
    @GetMapping("/{productId}/life-config")
    public ApiResponse<LifeProductSpec> getLifeProductSpec(@PathVariable String productId) {
        String tenantId = RequestContextHolder.requireTenantId();
        return ApiResponse.success(productQueryAppService.queryLifeProductSpec(productId, tenantId));
    }
}
