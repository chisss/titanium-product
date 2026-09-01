package com.titanium.product.web.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;

import com.titanium.product.api.ProductApi;
import com.titanium.product.api.response.config.IssuanceProcessConfigResponse;
import com.titanium.product.api.response.config.PolicyFormConfigResponse;
import com.titanium.product.api.response.config.UnderwritingConfigResponse;
import com.titanium.product.api.response.product.InsuranceProductDefinitionResponse;

/** 产品流程配置 API 的强类型契约测试，防止回退为 ApiResponse<Object>。 */
class ProductApiContractTypeTest {

    @Test
    void configEndpointsExposeTypedResponses() throws NoSuchMethodException {
        assertEquals(IssuanceProcessConfigResponse.class, responseType("getIssuanceConfig"));
        assertEquals(UnderwritingConfigResponse.class, responseType("getUnderwritingConfig"));
        assertEquals(UnderwritingConfigResponse.class, responseType("getUnderwritingConfigByCode"));
        assertEquals(PolicyFormConfigResponse.class, responseType("getPolicyFormConfig"));
    }

    @Test
    void definitionCatalogEndpointExposesTypedResponse() throws NoSuchMethodException {
        Type returnType = ProductApi.class.getMethod("listDefinitions").getGenericReturnType();
        ParameterizedType apiResponse = (ParameterizedType) returnType;
        ParameterizedType listType = (ParameterizedType) apiResponse.getActualTypeArguments()[0];
        assertEquals(InsuranceProductDefinitionResponse.class, listType.getActualTypeArguments()[0]);
    }

    private Class<?> responseType(String methodName) throws NoSuchMethodException {
        Type returnType = ProductApi.class.getMethod(methodName, String.class, String.class).getGenericReturnType();
        ParameterizedType apiResponse = (ParameterizedType) returnType;
        Type dataType = apiResponse.getActualTypeArguments()[0];
        return (Class<?>) dataType;
    }
}
