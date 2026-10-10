package com.titanium.product.infrastructure.pricing.repository.ncd;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.ncd.NcdRuleDO;

/** NCD 规则档位 JPA 仓储（G11/g11-03）。 */
public interface NcdRuleJpaRepository extends JpaRepository<NcdRuleDO, String> {

    /**
     * 取规则集下全部档位。
     * <p>
     * 排序只为**确定性**（同一次数据换次查询结果一致），不承载语义：反算的命中判定与内容指纹
     * 都与行序无关（见 {@code NcdRuleSet#resolve} / {@code #canonicalContent()}）。
     * </p>
     */
    List<NcdRuleDO> findByRuleSetIdAndTenantIdOrderByMinCountAscRuleIdAsc(String ruleSetId, String tenantId);

    /** 按规则集整体替换前先清空旧档位（草稿可编辑，已发布规则集冻结不动）。 */
    void deleteByRuleSetIdAndTenantId(String ruleSetId, String tenantId);
}
