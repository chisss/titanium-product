package com.titanium.product.valueobject.config;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.DomainException;

/**
 * 保额档位值对象（责任级保额档次配置的最小单元）
 * <p>
 * 一个档位 = 「一个可选的保额取值 + 该取值允许的保障期限集合 + 展示顺序」。保额档次按
 * <b>责任（coverageCode）</b>维度配置：同一产品下每个责任各有一套档位，而不是全产品共用一套。
 * </p>
 * <p>
 * <b>为何不在这里校验档位单调性与区间</b>：那些是「一组档位相对其区间」的性质，无法由单个档位自身判定，
 * 归属 {@link CoverageSumInsuredConfig} 的紧凑构造器。本类只守自己的字段不变量。
 * </p>
 * <p>
 * <b>金额类型</b>：{@code BigDecimal}，与同域 {@code LifeProductSpec.SumInsuredRange} 对齐——
 * 保额是产品定义侧的配置值，不是已发生账务，无需 {@code Money} 的币种维度；币种由产品级配置承载。
 * </p>
 *
 * @param tierCode     档位编码（责任内唯一，供投保单录入侧回填与前端定位）
 * @param sumInsured   档位保额（必须大于 0）
 * @param allowedTerms 该档位允许的保障期限（年）；null 视为空列表，空列表表示不按档位限制期限
 * @param sortNo       展示顺序（可为 null，仅影响呈现，不参与任何业务判定）
 */
public record SumInsuredTier(String tierCode, BigDecimal sumInsured, List<Integer> allowedTerms, Integer sortNo) {

    /**
     * 紧凑构造器：校验单档字段不变量
     * <p>
     * 错误码统一 {@link ProductErrorCode#COVERAGE_SUM_INSURED_CONFIG_INVALID}（类别 {@code PARAMETER_INVALID}），
     * 消息带 {@code tierCode} 便于运营定位到具体哪一档。
     * </p>
     */
    public SumInsuredTier {
        if (tierCode == null || tierCode.isBlank()) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID, "档位编码不能为空");
        }
        if (sumInsured == null || sumInsured.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException(ProductErrorCode.COVERAGE_SUM_INSURED_CONFIG_INVALID,
                    "档位保额必须大于 0，tierCode=" + tierCode + "，sumInsured=" + sumInsured);
        }
        allowedTerms = allowedTerms == null ? List.of() : List.copyOf(allowedTerms);
    }
}
