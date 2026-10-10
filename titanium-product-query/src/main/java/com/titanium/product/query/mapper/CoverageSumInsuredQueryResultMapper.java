package com.titanium.product.query.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.query.result.CoverageSumInsuredConfigQueryResult;
import com.titanium.product.query.result.CoverageSumInsuredTierResult;

/**
 * 责任级保额档次配置查询结果映射器（CQRS 读侧，声明式 MapStruct）
 * <p>
 * 把读模型 JSON 反序列化得到的<b>防腐条目</b>映射为查询结果 DTO。声明式映射：同名自动映射，
 * 无手写 {@code default} 方法逐字段 set（项目规约 §3.4.11 ② 禁止伪 MapStruct）。
 * </p>
 * <p>
 * <b>为何要防腐条目而不直接把 JSON 反序列化为 DTO</b>：读模型里的 JSON 是<b>写侧值对象的序列化形态</b>，
 * 其字段随写侧演进（如 {@code BigDecimal} 的 scale、新增可选分量）。若直接把 JSON 反序列化成
 * 对外 DTO 类型，写侧一次字段改动就会直接改变对外契约——中间的 record 承载「对端形态」，
 * 让契约变更必须显式经过映射器这一道关。
 * </p>
 * <p>
 * 🔴 <b>不在此做任何业务判定</b>：「这条录入保额算选档还是自由填」的裁决实现只有一份，
 * 在写侧值对象 {@code CoverageSumInsuredConfig#resolveSelection}。读侧复制一份必然漂移。
 * </p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CoverageSumInsuredQueryResultMapper {

    /**
     * 责任配置条目 → 责任配置查询结果
     * <p>
     * {@code tiers} 由 {@link #toTierResult} 逐元素转换（MapStruct 自动按元素类型选方法）。
     * </p>
     *
     * @param item 读模型 JSON 条目
     * @return 责任配置查询结果
     */
    CoverageSumInsuredConfigQueryResult toQueryResult(ConfigItem item);

    /**
     * 档位条目 → 档位查询结果
     *
     * @param item 读模型 JSON 条目
     * @return 档位查询结果
     */
    CoverageSumInsuredTierResult toTierResult(TierItem item);

    /**
     * 责任配置的防腐入站条目（读模型 JSON 的镜像形态，与写侧值对象解耦）
     *
     * @param coverageCode      责任编码
     * @param tiers             档位清单
     * @param minSumInsured     区间下界
     * @param maxSumInsured     区间上界
     * @param allowCustomAmount 是否允许自由填
     * @param defaultTierCode   默认档位编码
     */
    record ConfigItem(String coverageCode,
                      List<TierItem> tiers,
                      BigDecimal minSumInsured,
                      BigDecimal maxSumInsured,
                      Boolean allowCustomAmount,
                      String defaultTierCode) {
    }

    /**
     * 档位的防腐入站条目
     *
     * @param tierCode     档位编码
     * @param sumInsured   档位保额
     * @param allowedTerms 允许期限
     * @param sortNo       展示顺序
     */
    record TierItem(String tierCode, BigDecimal sumInsured, List<Integer> allowedTerms, Integer sortNo) {
    }
}
