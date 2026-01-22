package com.titanium.product.infrastructure.mapper;

import java.util.List;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.titanium.product.domain.aggregate.InsuranceProduct;
import com.titanium.product.domain.entity.ProductClauseRel;
import com.titanium.product.infrastructure.entity.ProductClauseRelDO;
import com.titanium.product.infrastructure.entity.ProductDO;

/**
 * 产品基础设施层映射器 用于基础设施层和领域层之间的数据转换
 */
@Mapper()
public interface ProductInfraMapper {

    ProductInfraMapper INSTANCE = Mappers.getMapper(ProductInfraMapper.class);

    /**
     * 将ProductDO转换为InsuranceProduct
     * 
     * @param productDO 产品数据库实体
     * @return 保险产品聚合根
     */
    @Mapping(source = "form", target = "form", qualifiedByName = "stringToProductForm")
    @Mapping(source = "insuranceType", target = "insuranceType", qualifiedByName = "stringToInsuranceType")
    @Mapping(source = "status", target = "status", qualifiedByName = "stringToProductStatus")
    @Mapping(source = "productId", target = "productId")
    InsuranceProduct toInsuranceProduct(ProductDO productDO);

    /**
     * 将InsuranceProduct转换为ProductDO
     * 
     * @param insuranceProduct 保险产品聚合根
     * @return 产品数据库实体
     */
    @Mapping(source = "form", target = "form", qualifiedByName = "productFormToString")
    @Mapping(source = "insuranceType", target = "insuranceType", qualifiedByName = "insuranceTypeToString")
    @Mapping(source = "status", target = "status", qualifiedByName = "productStatusToString")
    @Mapping(source = "productId", target = "productId")
    ProductDO toProductDO(InsuranceProduct insuranceProduct);

    /**
     * 将ProductClauseRelDO转换为ProductClauseRel
     * 
     * @param productClauseRelDO 产品条款关联数据库实体
     * @return 产品条款关联领域实体
     */
    @Mapping(source = "clauseId", target = "clauseId")
    @Mapping(source = "clauseVersion", target = "clauseVersion")
    @Mapping(source = "isMainClause", target = "isMainClause")
    ProductClauseRel toProductClauseRel(ProductClauseRelDO productClauseRelDO);

    /**
     * 将ProductClauseRel转换为ProductClauseRelDO
     * 
     * @param productClauseRel 产品条款关联领域实体
     * @return 产品条款关联数据库实体
     */
    @Mapping(source = "clauseId", target = "clauseId")
    @Mapping(source = "clauseVersion", target = "clauseVersion")
    @Mapping(source = "isMainClause", target = "isMainClause")
    ProductClauseRelDO toProductClauseRelDO(ProductClauseRel productClauseRel);

    /**
     * 将ProductClauseRelDO列表转换为ProductClauseRel列表
     * 
     * @param productClauseRelDOs 产品条款关联数据库实体列表
     * @return 产品条款关联领域实体列表
     */
    List<ProductClauseRel> toProductClauseRels(List<ProductClauseRelDO> productClauseRelDOs);

    /**
     * 将ProductClauseRel列表转换为ProductClauseRelDO列表
     * 
     * @param productClauseRels 产品条款关联领域实体列表
     * @return 产品条款关联数据库实体列表
     */
    List<ProductClauseRelDO> toProductClauseRelDOs(List<ProductClauseRel> productClauseRels);

    /**
     * 将字符串转换为ProductForm枚举
     * 
     * @param form 产品形态字符串
     * @return ProductForm枚举
     */
    @Named("stringToProductForm")
    default ProductEnum.ProductForm stringToProductForm(String form) {
        return form != null ? ProductEnum.ProductForm.valueOf(form) : null;
    }

    /**
     * 将ProductForm枚举转换为字符串
     * 
     * @param form ProductForm枚举
     * @return 产品形态字符串
     */
    @Named("productFormToString")
    default String productFormToString(ProductEnum.ProductForm form) {
        return form != null ? form.name() : null;
    }

    /**
     * 将字符串转换为InsuranceType枚举
     * 
     * @param insuranceType 险种类型字符串
     * @return InsuranceType枚举
     */
    @Named("stringToInsuranceType")
    default InsuranceType stringToInsuranceType(String insuranceType) {
        return insuranceType != null ? InsuranceType.valueOf(insuranceType) : null;
    }

    /**
     * 将InsuranceType枚举转换为字符串
     * 
     * @param insuranceType InsuranceType枚举
     * @return 险种类型字符串
     */
    @Named("insuranceTypeToString")
    default String insuranceTypeToString(InsuranceType insuranceType) {
        return insuranceType != null ? insuranceType.name() : null;
    }

    /**
     * 将字符串转换为ProductStatus枚举
     * 
     * @param status 产品状态字符串
     * @return ProductStatus枚举
     */
    @Named("stringToProductStatus")
    default ProductEnum.ProductStatus stringToProductStatus(String status) {
        return status != null ? ProductEnum.ProductStatus.valueOf(status) : null;
    }

    /**
     * 将ProductStatus枚举转换为字符串
     * 
     * @param status ProductStatus枚举
     * @return 产品状态字符串
     */
    @Named("productStatusToString")
    default String productStatusToString(ProductEnum.ProductStatus status) {
        return status != null ? status.name() : null;
    }

}
