package com.titanium.product.domain.query;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 根据条件查询产品命令 用于根据形态、险种、状态等条件分页查询产品列表
 */
public record FindProductByConditionQuery(ProductEnum.ProductForm form, InsuranceType type,
                                          ProductEnum.ProductStatus status, int pageNum, int pageSize) {
}
