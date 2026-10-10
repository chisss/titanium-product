package com.titanium.product.query.result;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 保额档位查询结果（CQRS 读侧 DTO）
 * <p>
 * 表达单条责任下的一个可选保额档位，作为查询返回的稳定契约。
 * </p>
 */
@Getter
@Setter
public class CoverageSumInsuredTierResult {

    /** 档位编码（责任内唯一，投保单录入侧按此回填） */
    private String tierCode;

    /** 档位保额 */
    private BigDecimal sumInsured;

    /** 该档位允许的保障期限（年）；空表示不按档位限制期限 */
    private List<Integer> allowedTerms;

    /** 展示顺序 */
    private Integer sortNo;
}
