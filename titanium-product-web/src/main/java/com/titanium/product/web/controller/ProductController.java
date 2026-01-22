package com.titanium.product.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.product.api.ProductApi;
import com.titanium.product.api.dto.ProductDTO;
import com.titanium.product.api.request.AuditProductRequest;
import com.titanium.product.api.request.CreateProductRequest;
import com.titanium.product.api.response.ApiResponse;
import com.titanium.product.application.command.ProductCommandAppService;
import com.titanium.product.application.query.ProductQueryAppService;
import com.titanium.product.domain.command.AuditProductCommand;
import com.titanium.product.domain.command.CreateProductCommand;
import com.titanium.product.domain.command.InvalidateProductCommand;
import com.titanium.product.web.mapper.ProductWebMapper;

/**
 * 产品控制器 处理产品相关的HTTP请求，实现ProductApi接口
 */
@RestController
public class ProductController implements ProductApi {

    @Autowired
    private ProductCommandAppService productCommandAppService;

    @Autowired
    private ProductQueryAppService   productQueryAppService;

    @Autowired
    private ProductWebMapper         productWebMapper;

    /**
     * 创建产品
     * 
     * @param request 创建产品请求
     * @param tenantId 租户ID
     * @return API响应
     */
    @Override
    public ApiResponse<String> createProduct(CreateProductRequest request, String tenantId) {
        // 转换请求为命令
        CreateProductCommand command = productWebMapper.toCreateProductCommand(request);

        // 执行命令
        String productId = productCommandAppService.createProduct(command);

        // 返回响应
        return ApiResponse.success(productId);
    }

    /**
     * 根据ID查询产品
     * 
     * @param productId 产品ID
     * @param tenantId 租户ID
     * @return API响应
     */
    @Override
    public ApiResponse<ProductDTO> getProductById(String productId, String tenantId) {
        // 查询产品
        var product = productQueryAppService.queryProductDetail(productId);

        // 转换为DTO
        ProductDTO productDTO = productWebMapper.toProductDTO(product);

        // 返回响应
        return ApiResponse.success(productDTO);
    }

    /**
     * 审核产品
     * 
     * @param productId 产品ID
     * @param request 审核产品请求
     * @param tenantId 租户ID
     * @return API响应
     */
    @Override
    public ApiResponse<Void> auditProduct(String productId, AuditProductRequest request, String tenantId) {
        // 转换请求为命令
        AuditProductCommand command = new AuditProductCommand(productId);
        // 执行命令
        productCommandAppService.auditProduct(command);
        // 返回响应
        return ApiResponse.success(null);
    }

    /**
     * 下架产品
     * 
     * @param productId 产品ID
     * @param tenantId 租户ID
     * @return API响应
     */
    @Override
    public ApiResponse<Void> invalidateProduct(String productId, String tenantId) {
        // 创建下架命令
        InvalidateProductCommand command = new InvalidateProductCommand(productId);

        // 执行命令
        productCommandAppService.invalidateProduct(command);

        // 返回响应
        return ApiResponse.success(null);
    }
}
