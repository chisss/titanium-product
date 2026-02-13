package com.titanium.product.application.query;

import java.util.List;

import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.domain.query.GetTemplateByCodeQuery;
import com.titanium.product.domain.query.GetTemplateByIdQuery;
import com.titanium.product.domain.query.GetTemplateByProductIdQuery;
import com.titanium.product.domain.query.GetTemplatesByInsuranceTypeQuery;
import com.titanium.product.query.entity.ProductTemplateQueryResult;

/**
 * 产品模板查询应用服务
 */
@Service
public class ProductTemplateQueryAppService {

    @Autowired
    private QueryGateway queryGateway;

    /**
     * 根据ID查询产品模板
     */
    public ProductTemplateQueryResult getTemplateById(String templateId, String tenantId) {
        return queryGateway.query(
                new GetTemplateByIdQuery(templateId, tenantId),
                ResponseTypes.instanceOf(ProductTemplateQueryResult.class)
        ).join();
    }

    /**
     * 根据产品ID查询产品模板
     */
    public ProductTemplateQueryResult getTemplateByProductId(String productId, String tenantId) {
        return queryGateway.query(
                new GetTemplateByProductIdQuery(productId, tenantId),
                ResponseTypes.instanceOf(ProductTemplateQueryResult.class)
        ).join();
    }

    /**
     * 根据模板编码查询产品模板
     */
    public ProductTemplateQueryResult getTemplateByCode(String templateCode, String tenantId) {
        return queryGateway.query(
                new GetTemplateByCodeQuery(templateCode, tenantId),
                ResponseTypes.instanceOf(ProductTemplateQueryResult.class)
        ).join();
    }

    /**
     * 根据险种类型查询产品模板列表
     */
    public List<ProductTemplateQueryResult> getTemplatesByInsuranceType(InsuranceType insuranceType, String tenantId) {
        return queryGateway.query(
                new GetTemplatesByInsuranceTypeQuery(insuranceType, tenantId),
                ResponseTypes.multipleInstancesOf(ProductTemplateQueryResult.class)
        ).join();
    }
}
