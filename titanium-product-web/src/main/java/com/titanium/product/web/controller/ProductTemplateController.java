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
import com.titanium.product.api.ProductTemplateApi;
import com.titanium.product.api.dto.ProductTemplateDTO;
import com.titanium.product.api.request.CreateProductTemplateRequest;
import com.titanium.product.api.response.ApiResponse;
import com.titanium.product.application.command.ProductTemplateCommandAppService;
import com.titanium.product.application.query.ProductTemplateQueryAppService;
import com.titanium.product.domain.command.ActivateProductTemplateCommand;
import com.titanium.product.domain.command.CreateProductTemplateCommand;
import com.titanium.product.domain.command.DeactivateProductTemplateCommand;
import com.titanium.product.query.entity.ProductTemplateQueryResult;
import com.titanium.product.web.mapper.ProductTemplateWebMapper;

import lombok.RequiredArgsConstructor;

/**
 * 产品模板控制器
 */
@RestController
@RequestMapping("/api/product-templates")
@RequiredArgsConstructor
public class ProductTemplateController implements ProductTemplateApi {

    private final ProductTemplateCommandAppService commandAppService;

    private final ProductTemplateQueryAppService queryAppService;

    private final ProductTemplateWebMapper webMapper;

    @Override
    public ApiResponse<ProductTemplateDTO> getByProductId(@PathVariable String productId,
                                                           @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductTemplateQueryResult result = queryAppService.getTemplateByProductId(productId, tenantId);
        return ApiResponse.success(webMapper.toDTO(result));
    }

    @Override
    public ApiResponse<ProductTemplateDTO> getByCode(@PathVariable String templateCode,
                                                      @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductTemplateQueryResult result = queryAppService.getTemplateByCode(templateCode, tenantId);
        return ApiResponse.success(webMapper.toDTO(result));
    }

    @Override
    public ApiResponse<ProductTemplateDTO> getById(@PathVariable String templateId,
                                                    @RequestHeader("X-Tenant-ID") String tenantId) {
        ProductTemplateQueryResult result = queryAppService.getTemplateById(templateId, tenantId);
        return ApiResponse.success(webMapper.toDTO(result));
    }

    /**
     * 创建产品模板
     */
    @PostMapping
    public ApiResponse<String> createTemplate(@RequestBody CreateProductTemplateRequest request,
                                               @RequestHeader("X-Tenant-ID") String tenantId) {
        CreateProductTemplateCommand command = webMapper.toCreateCommand(request, tenantId);
        String templateId = commandAppService.createTemplate(command);
        return ApiResponse.success(templateId);
    }

    /**
     * 激活产品模板
     */
    @PutMapping("/{templateId}/activate")
    public ApiResponse<Void> activateTemplate(@PathVariable String templateId,
                                               @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.activateTemplate(new ActivateProductTemplateCommand(templateId, tenantId));
        return ApiResponse.success(null);
    }

    /**
     * 停用产品模板
     */
    @PutMapping("/{templateId}/deactivate")
    public ApiResponse<Void> deactivateTemplate(@PathVariable String templateId,
                                                 @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.deactivateTemplate(new DeactivateProductTemplateCommand(templateId, tenantId));
        return ApiResponse.success(null);
    }

    /**
     * 根据险种类型查询模板列表
     */
    @GetMapping("/by-type/{insuranceTypeCode}")
    public ApiResponse<List<ProductTemplateDTO>> getByInsuranceType(@PathVariable String insuranceTypeCode,
                                                                     @RequestHeader("X-Tenant-ID") String tenantId) {
        InsuranceType type = InsuranceType.fromCode(insuranceTypeCode);
        List<ProductTemplateQueryResult> results = queryAppService.getTemplatesByInsuranceType(type, tenantId);
        return ApiResponse.success(results.stream().map(webMapper::toDTO).toList());
    }
}
