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
 * 产品查询层映射器
 * 用于领域层和查询层之间的数据转换
 */
@Mapper
public interface ProductQueryMapper {

    ProductQueryMapper INSTANCE = Mappers.getMapper(ProductQueryMapper.class);

    @Mapping(source = "form", target = "form", qualifiedByName = "productFormToString")
    @Mapping(source = "insuranceType", target = "insuranceType", qualifiedByName = "insuranceTypeToString")
    @Mapping(source = "status", target = "status", qualifiedByName = "productStatusToString")
    @Mapping(source = "category", target = "category", qualifiedByName = "productCategoryToString")
    @Mapping(source = "productId", target = "productId")
    ProductQueryResult toProductQueryResult(InsuranceProduct insuranceProduct);

    @Named("productFormToString")
    default String productFormToString(ProductEnum.ProductForm form) {
        return form != null ? form.name() : null;
    }

    @Named("insuranceTypeToString")
    default String insuranceTypeToString(InsuranceType insuranceType) {
        return insuranceType != null ? insuranceType.name() : null;
    }

    @Named("productStatusToString")
    default String productStatusToString(ProductEnum.ProductStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("productCategoryToString")
    default String productCategoryToString(ProductEnum.ProductCategory category) {
        return category != null ? category.name() : null;
    }
}
