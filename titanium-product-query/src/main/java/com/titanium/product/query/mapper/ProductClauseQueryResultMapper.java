package com.titanium.product.query.mapper;

import java.time.LocalDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.query.result.ProductClauseQueryResult;

/**
 * 产品条款关联 → 查询结果声明式映射（MapStruct）。
 * <p>
 * 读模型 JSON 清单按键名解析为 {@link ClauseRelItem} 防腐结构后，
 * 此处仅做 {@code isMainClause} → {@code mainClause} 的差异字段映射，其余同名字段自动映射。
 * </p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductClauseQueryResultMapper {

    /** 条款关联条目 → 查询结果（isMainClause 键名差异映射）。 */
    @Mapping(target = "mainClause", source = "isMainClause")
    ProductClauseQueryResult toQueryResult(ClauseRelItem item);

    /** 读模型 JSON 清单的防腐入站条目结构（镜像 t_product_clause_rel_view 的 JSON 键名）。 */
    record ClauseRelItem(String clauseId, String clauseVersion, Boolean isMainClause, LocalDateTime bindTime) {
    }
}
