package com.titanium.product.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.titanium.product.aggregate.surrender.SurrenderValuePolicy;

/** 退保价值策略仓储端口。 */
public interface SurrenderValuePolicyRepository {

    Optional<SurrenderValuePolicy> findPublished(
            String tenantId, String productId, int policyYear, LocalDateTime businessTime);
}
