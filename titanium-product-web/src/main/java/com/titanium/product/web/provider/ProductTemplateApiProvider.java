package com.titanium.product.web.provider;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductTemplateApi;
import com.titanium.product.api.response.product.ProductTemplateResponse;
import com.titanium.product.application.query.ProductTemplateQueryAppService;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.web.mapper.ProductTemplateWebMapper;

import lombok.RequiredArgsConstructor;

/**
 * 产品模板契约实现（Provider）
 * <p>
 * 承接 {@link ProductTemplateApi} Feign 契约，面向其它微服务的远程调用。路径由
 * {@link ProductTemplateApi} 的 {@code @FeignClient(path="/api/v1/product-templates")} 唯一定义，
 * 本类通过 {@code implements} 继承，<b>不重复标注、不篡改</b>。契约仅暴露读操作（按产品ID/编码/ID查询），
 * 职责为读模型结果 → DTO 的协议转换 + 调用查询门面，零业务逻辑。模板的创建/激活/停用等写操作面向
 * 后台/端上，由 {@code ProductTemplateController} 承接，不进入远程契约。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/product-templates")
@RequiredArgsConstructor
public class ProductTemplateApiProvider implements ProductTemplateApi {

    private final ProductTemplateQueryAppService productTemplateQueryAppService;

    private final ProductTemplateWebMapper       productTemplateWebMapper;

    @Override
    public ApiResponse<ProductTemplateResponse> getByProductId(String productId, String tenantId) {
        ProductTemplateQueryResult result = productTemplateQueryAppService.getTemplateByProductId(productId, tenantId);
        return ApiResponse.success(productTemplateWebMapper.toResponse(result));
    }

    @Override
    public ApiResponse<ProductTemplateResponse> getByCode(String templateCode, String tenantId) {
        ProductTemplateQueryResult result = productTemplateQueryAppService.getTemplateByCode(templateCode, tenantId);
        return ApiResponse.success(productTemplateWebMapper.toResponse(result));
    }

    @Override
    public ApiResponse<ProductTemplateResponse> getById(String templateId, String tenantId) {
        ProductTemplateQueryResult result = productTemplateQueryAppService.getTemplateById(templateId, tenantId);
        return ApiResponse.success(productTemplateWebMapper.toResponse(result));
    }
}
