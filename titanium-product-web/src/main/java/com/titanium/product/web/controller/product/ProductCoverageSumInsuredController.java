package com.titanium.product.web.controller.product;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.context.RequestContextHolder;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.response.config.CoverageSumInsuredConfigResponse;
import com.titanium.product.application.command.ProductCommandAppService;
import com.titanium.product.application.query.ProductQueryAppService;
import com.titanium.product.command.UpdateCoverageSumInsuredConfigCommand;
import com.titanium.product.web.dto.product.UpdateCoverageSumInsuredConfigDTO;
import com.titanium.product.web.mapper.ProductWebMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 责任级保额档次配置控制器（后台/端上 HTTP 入口）
 * <p>
 * 路径 {@code /web/v1/products/{productId}/coverage-sum-insured-configs}，与
 * {@link ProductController} <b>同前缀、不同末段</b>：档次配置是一块<b>独立的配置资源</b>
 * （自有读模型表 {@code t_product_coverage_sum_insured_view}、自有聚合字段与投影处理器），
 * 按资源/业务主题独立成控制器，而不塞进 {@link ProductController}。
 * </p>
 * <p>
 * 🔴 <b>为何单独成类</b>：产品主控制器端点已达 {@code controllersMustNotBeOverloaded} 棘轮基线
 * （19 个，只许下调），再把档次配置两条端点并入即超线。拆分<b>不改变任何 HTTP 路径</b>，
 * 前端零感知；同时也符合根规约 §3.4.3 / §3.4.13「多类资源被塞进一个类应按资源/业务主题拆」。
 * </p>
 * <p>
 * 与远程同能力入口 {@code /api/v1/products/{productId}/coverage-sum-insured-configs}
 * （{@code ProductApiProvider}）<b>平行</b>：各自翻译成同一个 {@link UpdateCoverageSumInsuredConfigCommand}，
 * 汇入同一个 application 薄门面，互不依赖、不得互调（§3.4.10）。
 * </p>
 */
@RestController
@RequestMapping("/web/v1/products")
@RequiredArgsConstructor
public class ProductCoverageSumInsuredController {

    private final ProductCommandAppService productCommandAppService;

    private final ProductQueryAppService   productQueryAppService;

    private final ProductWebMapper         productWebMapper;

    /**
     * 查询产品的责任级保额档次配置
     * <p>
     * 读读模型 {@code t_product_coverage_sum_insured_view}，返回每个责任可选的保额档位、保额区间
     * 与是否开放自由填。未配置时返回空列表（不是 404——「尚未配置」是正常状态）。
     * </p>
     * <p>
     * ⚠️ 读模型最终一致；且本入口只<b>提供数据</b>，不做「这条保额算选档还是自由填」的裁决
     * （裁决的唯一实现在写侧值对象 {@code CoverageSumInsuredConfig#resolveSelection}）。
     * </p>
     *
     * @param productId 产品ID
     * @return 该产品全部责任的档次配置；未配置时为空列表
     */
    @GetMapping("/{productId}/coverage-sum-insured-configs")
    public ApiResponse<List<CoverageSumInsuredConfigResponse>> getCoverageSumInsuredConfigs(
            @PathVariable String productId) {
        List<CoverageSumInsuredConfigResponse> responses =
                productQueryAppService.queryCoverageSumInsuredConfigs(productId).stream()
                        .map(productWebMapper::toCoverageSumInsuredResponse)
                        .toList();
        return ApiResponse.success(responses);
    }

    /**
     * 整体替换产品的责任级保额档次配置
     * <p>
     * <b>整体替换</b>语义：提交的清单即替换后的全部配置，不是增量；传空清单表示<b>清空</b>。
     * 两条前置由服务端裁决，本层<b>不复制</b>（避免同一判断两处各写一份而漂移）：
     * ① 仅 {@code DRAFT} 状态产品可配置（聚合根）；② 每条 {@code coverageCode} 须落在该产品已绑定条款的
     * 责任集合内（应用层跨域向条款域求证）。
     * </p>
     *
     * @param productId 产品ID
     * @param request   替换后的全部责任档次配置（不得为 null；空清单表示清空）
     * @return 无数据体的成功响应
     */
    @PutMapping("/{productId}/coverage-sum-insured-configs")
    public ApiResponse<Void> updateCoverageSumInsuredConfig(
            @PathVariable String productId,
            @Valid @RequestBody UpdateCoverageSumInsuredConfigDTO request) {
        String tenantId = RequestContextHolder.requireTenantId();
        UpdateCoverageSumInsuredConfigCommand command =
                productWebMapper.toCommand(request, productId, tenantId);
        productCommandAppService.updateCoverageSumInsuredConfig(command);
        return ApiResponse.success(null);
    }
}
