package com.titanium.product.infrastructure.pricing.adapter.ncd;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.mapper.NcdRuleSetPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.ncd.NcdRuleSetDO;
import com.titanium.product.infrastructure.pricing.repository.ncd.NcdRuleJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.ncd.NcdRuleSetJpaRepository;
import com.titanium.product.pricing.aggregate.ncd.NcdRule;
import com.titanium.product.pricing.aggregate.ncd.NcdRuleSet;
import com.titanium.product.repository.NcdRuleSetRepository;

import lombok.RequiredArgsConstructor;

/** NCD 规则集关系型仓储适配器（G11/g11-03）。 */
@Repository
@RequiredArgsConstructor
public class JpaNcdRuleSetRepository implements NcdRuleSetRepository {

    private final NcdRuleSetJpaRepository jpaRepository;
    private final NcdRuleJpaRepository ruleJpaRepository;
    private final NcdRuleSetPersistenceMapper persistenceMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBusinessKey(String tenantId, String productId, String ruleSetCode, String ruleSetVersion) {
        return jpaRepository.existsByTenantIdAndProductIdAndRuleSetCodeAndRuleSetVersion(
                tenantId, productId, ruleSetCode, ruleSetVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NcdRuleSet> findById(String tenantId, String productId, String ruleSetId) {
        return jpaRepository.findByRuleSetIdAndTenantIdAndProductId(ruleSetId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NcdRuleSet> findPublished(String tenantId, String productId, LocalDateTime businessTime) {
        return jpaRepository.findPublished(tenantId, productId, businessTime).map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(NcdRuleSet ruleSet) {
        jpaRepository.save(persistenceMapper.toDO(ruleSet));
        // 档位整体重写仅限草稿：已发布规则集冻结，编辑须另起版本（与费率表 save 同法）
        if (ruleSet.getStatus() == ActuarialDefinitionStatus.DRAFT) {
            ruleJpaRepository.deleteByRuleSetIdAndTenantId(ruleSet.getRuleSetId(), ruleSet.getTenantId());
            ruleJpaRepository.flush();
            ruleJpaRepository.saveAll(ruleSet.getRules().stream()
                    .map(rule -> persistenceMapper.toDO(ruleSet, rule))
                    .toList());
        }
    }

    private NcdRuleSet toDomain(NcdRuleSetDO dataObject) {
        List<NcdRule> rules = ruleJpaRepository
                .findByRuleSetIdAndTenantIdOrderByMinCountAscRuleIdAsc(
                        dataObject.getRuleSetId(), dataObject.getTenantId())
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
        return NcdRuleSet.restore(
                dataObject.getRuleSetId(), dataObject.getProductId(), dataObject.getRuleSetCode(),
                dataObject.getRuleSetVersion(), dataObject.getLookbackMonths(), rules,
                dataObject.getEffectiveFrom(), dataObject.getEffectiveTo(),
                dataObject.getTenantId(), dataObject.getStatus(), dataObject.getContentHash());
    }
}
