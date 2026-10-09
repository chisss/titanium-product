package com.titanium.product.api.response.config;

import java.math.BigDecimal;
import java.util.List;

/**
 * 保额档位响应（api 契约，镜像领域值对象 SumInsuredTier）
 *
 * @param tierCode     档位编码（在所属责任内唯一，默认档位以它指代）
 * @param sumInsured   档位保额（严格递增，且落在所属责任的保额区间内）
 * @param allowedTerms 该档位允许的保障期限（空列表表示不按期限限制）
 * @param sortNo       展示排序号（仅用于呈现，不参与任何裁决）
 */
public record CoverageSumInsuredTierResponse(String tierCode,
                                             BigDecimal sumInsured,
                                             List<Integer> allowedTerms,
                                             Integer sortNo) {
}
