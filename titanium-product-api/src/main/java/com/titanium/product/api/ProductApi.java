package com.titanium.product.api;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.titanium.metadata.enums.Reserved;
import com.titanium.metadata.response.ApiResponse;
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

import jakarta.validation.Valid;

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

    /** 查询标准险种产品定义目录 */
    @GetMapping("/definitions")
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<List<InsuranceProductDefinitionResponse>> listDefinitions();

    /** 创建产品 */
    @PostMapping
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<String> createProduct(@RequestBody CreateProductRequest dto);

    /** 根据ID查询产品 */
    @GetMapping("/{productId}")
    ApiResponse<ProductResponse> getProductById(@PathVariable String productId);

    /** 提交产品审核 */
    @PutMapping("/{productId}/submit-audit")
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<Void> submitForAudit(@PathVariable String productId);

    /** 审核产品 */
    @PutMapping("/{productId}/audit")
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<Void> auditProduct(@PathVariable String productId,
                                    @RequestBody AuditProductRequest dto);

    /** 驳回产品审核 */
    @PutMapping("/{productId}/reject")
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<Void> rejectAudit(@PathVariable String productId,
                                   @RequestBody AuditProductRequest dto);

    /** 下架产品 */
    @PutMapping("/{productId}/invalid")
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<Void> invalidateProduct(@PathVariable String productId);

    /** 查询产品出单流程配置（供Policy域调用） */
    @GetMapping("/{productId}/issuance-config")
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<IssuanceProcessConfigResponse> getIssuanceConfig(
            @PathVariable String productId);

    /** 查询产品核保配置（供Underwriting域调用） */
    @GetMapping("/{productId}/underwriting-config")
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<UnderwritingConfigResponse> getUnderwritingConfig(
            @PathVariable String productId);

    /** 按产品编码查询核保配置（供仅持有险种编码的 Underwriting 域调用） */
    @GetMapping("/by-code/{productCode}/underwriting-config")
    ApiResponse<UnderwritingConfigResponse> getUnderwritingConfigByCode(
            @PathVariable String productCode);

    /** 查询产品保单形态配置（供Policy域调用） */
    @GetMapping("/{productId}/policy-form-config")
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<PolicyFormConfigResponse> getPolicyFormConfig(
            @PathVariable String productId);

    /** 查询产品定价基础规则（供Billing域保费计算调用） */
    @GetMapping("/{productId}/pricing-rule")
    ApiResponse<PricingBasicRuleResponse> getPricingRule(@PathVariable String productId);

    /**
     * 查询产品绑定的条款关联（供 Policy 域出单装配条款/责任快照调用）
     * <p>
     * 返回条款ID + <b>版本</b> + 是否主条款。policy 域据此向 clause 域取条款与责任清单，
     * 冻结为保单的条款快照（L2.5）与责任快照（L4）——保单一经签发即适用该版本条款，
     * 条款域后续修订不影响存量保单。
     * </p>
     *
     * @param productId 产品ID
     * @return 条款关联列表，未绑定时为空列表
     */
    @GetMapping("/{productId}/clauses")
    ApiResponse<List<ProductClauseResponse>> getProductClauses(@PathVariable String productId);

    /**
     * 整体替换产品绑定的条款关联（上游 → product；远程写入口）
     * <p>
     * <b>整体替换</b>语义：提交的清单即替换后的全部绑定，不是增量。服务端实现置于 web 层
     * {@code ProductApiProvider}（契约实现落 provider，Controller 不得 implements Api），
     * 与后台/端上的 {@code PUT /web/v1/products/{productId}/clauses} <b>平行收敛</b>到同一个
     * application 薄门面，互不依赖。
     * </p>
     *
     * <p>🔴 仅 {@code DRAFT} 状态产品可改绑定，且新绑定须在条款域真实存在并处于 {@code ACTIVE}——
     * 两条前置由<b>服务端</b>裁决（前者在聚合根、后者在应用层跨域校验），调用方无需自证。</p>
     *
     * @param productId 产品ID
     * @param request   替换后的全部条款关联（非空）
     * @return 无数据体的成功响应；失败时 {@code code} 承载产品域业务错误码
     */
    @PutMapping("/{productId}/clauses")
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<Void> updateProductClauseRel(@PathVariable String productId,
                                             @Valid @RequestBody UpdateProductClauseRelRequest request);

    /**
     * 查询产品的责任级保额档次配置（供出单/投保链路取「每个责任可填什么保额」）
     * <p>
     * 一个产品下每个已配置的责任一条，含档位清单、保额区间与是否开放自由填。未配置时返回空列表
     * （不是 404——「该产品尚未配置档次」是正常状态，不是资源缺失）。
     * </p>
     * <p>
     * ⚠️ <b>读自读模型，最终一致</b>：刚写入的配置可能需要短暂延迟才可见。调用方若要「写完立刻校验」，
     * 应以服务端的写入结果为答，不要以本端点回读为准。
     * </p>
     *
     * @param productId 产品ID
     * @return 该产品全部责任的档次配置；未配置时为空列表
     */
    @GetMapping("/{productId}/coverage-sum-insured-configs")
    ApiResponse<List<CoverageSumInsuredConfigResponse>> getCoverageSumInsuredConfigs(
            @PathVariable String productId);

    /**
     * 整体替换产品的责任级保额档次配置（上游 → product；远程写入口）
     * <p>
     * <b>整体替换</b>语义：提交的清单即替换后的全部配置，不是增量；传空清单表示<b>清空</b>。
     * 服务端实现置于 web 层 {@code ProductApiProvider}（契约实现落 provider，Controller 不得
     * implements Api），与后台/端上的
     * {@code PUT /web/v1/products/{productId}/coverage-sum-insured-configs} <b>平行收敛</b>到同一个
     * application 薄门面，互不依赖。
     * </p>
     *
     * <p>🔴 两条前置由<b>服务端</b>裁决，调用方无需自证：① 仅 {@code DRAFT} 状态产品可配置（聚合根）；
     * ② 每条 {@code coverageCode} 须落在该产品<b>已绑定条款的责任集合</b>内（应用层跨域向条款域求证）——
     * 后者是为了堵住「产品侧配了、投保侧永远匹配不到」的静默失效。</p>
     *
     * @param productId 产品ID
     * @param request   替换后的全部责任档次配置（不得为 null，清空请传空清单）
     * @return 无数据体的成功响应；失败时 {@code code} 承载产品域业务错误码
     */
    @PutMapping("/{productId}/coverage-sum-insured-configs")
    @Reserved("g16-04: 跨域零消费者（管理面经 web 层）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<Void> updateCoverageSumInsuredConfig(
            @PathVariable String productId,
            @Valid @RequestBody UpdateCoverageSumInsuredConfigRequest request);
}
