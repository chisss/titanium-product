package com.titanium.product.query.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.aggregate.InsuranceProduct;
import com.titanium.product.query.entity.ProductQueryResult;

/**
 * 产品查询层映射器 用于领域层和查询层之间的数据转换
 */
@Mapper()
public interface ProductQueryMapper {

    ProductQueryMapper INSTANCE = Mappers.getMapper(ProductQueryMapper.class);

    /**
     * 将InsuranceProduct转换为ProductQueryResult
     * 
     * @param insuranceProduct 保险产品聚合根
     * @return 产品查询结果实体
     */
    @Mapping(source = "form", target = "form", qualifiedByName = "productFormToString")
    @Mapping(source = "insuranceType", target = "insuranceType", qualifiedByName = "insuranceTypeToString")
    @Mapping(source = "status", target = "status", qualifiedByName = "productStatusToString")
    @Mapping(source = "productId", target = "productId")
    ProductQueryResult toProductQueryResult(InsuranceProduct insuranceProduct);

    /**
     * 将ProductForm枚举转换为字符串
     * 
     * @param form 产品形态枚举
     * @return 产品形态字符串
     */
    @Named("productFormToString")
    default String productFormToString(ProductEnum.ProductForm form) {
        return form != null ? form.name() : null;
    }

    /**
     * 将InsuranceType枚举转换为字符串
     * 
     * @param insuranceType 险种类型枚举
     * @return 险种类型字符串
     */
    @Named("insuranceTypeToString")
    default String insuranceTypeToString(InsuranceType insuranceType) {
        return insuranceType != null ? insuranceType.name() : null;
    }

    /**
     * 将ProductStatus枚举转换为字符串
     * 
     * @param status 产品状态枚举
     * @return 产品状态字符串
     */
    @Named("productStatusToString")
    default String productStatusToString(ProductEnum.ProductStatus status) {
        return status != null ? status.name() : null;
    }
}
