package com.titanium.product.web.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.response.ProductTemplateResponse;
import com.titanium.product.application.command.ProductTemplateCommandAppService;
import com.titanium.product.application.query.ProductTemplateQueryAppService;
import com.titanium.product.command.CreateProductTemplateCommand;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.web.dto.CreateProductTemplateDTO;
import com.titanium.product.web.dto.UpdateProductTemplateDTO;
import com.titanium.product.web.mapper.ProductTemplateWebMapper;

import lombok.RequiredArgsConstructor;

/**
 * 产品模板控制器（后台/端上 HTTP 入口）
 * <p>
 * 面向管理后台/端上，路径 {@code /web/v1/product-templates}，入参 web Request、出参对外 DTO，
 * <b>不再 implements ProductTemplateApi</b>（远程只读契约由
 * {@link com.titanium.product.web.provider.ProductTemplateApiProvider} 承接）。表现层经
 * {@link ProductTemplateWebMapper} 把 Request 转成 {@link CreateProductTemplateCommand} 交
 * {@link ProductTemplateCommandAppService}，读入口查读模型交 {@link ProductTemplateQueryAppService}。
 * </p>
 */
@RestController
@RequestMapping("/web/v1/product-templates")
@RequiredArgsConstructor
public class ProductTemplateController {

    private final ProductTemplateCommandAppService commandAppService;

    private final ProductTemplateQueryAppService   queryAppService;

    private final ProductTemplateWebMapper         webMapper;

    /**
     * 根据产品ID查询产品模板
     */
    @GetMapping("/by-product/{productId}")
    public ApiResponse<ProductTemplateResponse> getByProductId(@PathVariable String productId,
                                                          @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductTemplateQueryResult result = queryAppService.getTemplateByProductId(productId, tenantId);
        return ApiResponse.success(webMapper.toResponse(result));
    }

    /**
     * 根据模板编码查询产品模板
     */
    @GetMapping("/by-code/{templateCode}")
    public ApiResponse<ProductTemplateResponse> getByCode(@PathVariable String templateCode,
                                                     @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductTemplateQueryResult result = queryAppService.getTemplateByCode(templateCode, tenantId);
        return ApiResponse.success(webMapper.toResponse(result));
    }

    /**
     * 根据模板ID查询产品模板
     */
    @GetMapping("/{templateId}")
    public ApiResponse<ProductTemplateResponse> getById(@PathVariable String templateId,
                                                   @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductTemplateQueryResult result = queryAppService.getTemplateById(templateId, tenantId);
        return ApiResponse.success(webMapper.toResponse(result));
    }

    /**
     * 创建产品模板
     */
    @PostMapping
    public ApiResponse<String> createTemplate(@RequestBody CreateProductTemplateDTO request,
                                              @RequestHeader("X-Tenant-ID") String tenantId) {
        // 协议转换：HTTP Request → 领域命令，收敛到应用层门面
        CreateProductTemplateCommand command = webMapper.toCommand(request, tenantId);
        String templateId = commandAppService.createTemplate(command);
        return ApiResponse.success(templateId);
    }

    /**
     * 更新产品模板行为配置
     * <p>
     * 更新模板的出单模式/出单阶段/核保/保单结构/保全/理赔/缴费/再保/分红等行为配置。web 层经
     * {@link ProductTemplateWebMapper} 把请求转成 {@code UpdateProductTemplateCommand}，交
     * {@link ProductTemplateCommandAppService} 派发。模板须为非删除态，否则聚合根拒绝更新。
     * </p>
     *
     * @param templateId 模板ID（路径变量）
     * @param request 更新产品模板请求
     * @param tenantId 租户ID（请求头）
     * @return 空响应
     */
    @PutMapping("/{templateId}")
    public ApiResponse<Void> updateTemplate(@PathVariable String templateId,
                                            @RequestBody UpdateProductTemplateDTO request,
                                            @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.updateTemplate(webMapper.toUpdateCommand(templateId, request, tenantId));
        return ApiResponse.success(null);
    }

    /**
     * 激活产品模板
     */
    @PutMapping("/{templateId}/activate")
    public ApiResponse<Void> activateTemplate(@PathVariable String templateId,
                                              @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.activateTemplate(templateId, tenantId);
        return ApiResponse.success(null);
    }

    /**
     * 停用产品模板
     */
    @PutMapping("/{templateId}/deactivate")
    public ApiResponse<Void> deactivateTemplate(@PathVariable String templateId,
                                                @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.deactivateTemplate(templateId, tenantId);
        return ApiResponse.success(null);
    }

    /**
     * 根据险种类型查询模板列表
     */
    @GetMapping("/by-type/{insuranceTypeCode}")
    public ApiResponse<List<ProductTemplateResponse>> getByInsuranceType(@PathVariable String insuranceTypeCode,
                                                                    @RequestHeader("X-Tenant-ID") String tenantId) {
        InsuranceType type = InsuranceType.fromCode(insuranceTypeCode);
        List<ProductTemplateQueryResult> results = queryAppService.getTemplatesByInsuranceType(type, tenantId);
        return ApiResponse.success(results.stream().map(webMapper::toResponse).toList());
    }
}
