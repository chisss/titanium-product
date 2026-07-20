package com.titanium.product.query.query;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 根据条件查询产品命令 用于根据产品名称、形态、险种、状态等条件分页查询产品列表
 *
 * @param productName 产品名称（模糊匹配，可为空表示不限）
 * @param form 产品形态（可为空表示不限）
 * @param type 险种类型（可为空表示不限）
 * @param status 产品状态（可为空表示不限）
 * @param pageNum 页码（从 0 开始）
 * @param pageSize 每页条数
 */
public record FindProductByConditionQuery(String productName, ProductEnum.ProductForm form, InsuranceProductType type,
                                          ProductEnum.ProductStatus status, int pageNum, int pageSize) {
}
