package com.titanium.product.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.config.IssuanceProcessConfig;

/**
 * 产品领域服务 处理跨聚合根的纯领域校验逻辑，如产品条款绑定校验、出单配置一致性校验、附加险兼容性校验等。 无 CommandGateway、无 Port
 * 依赖、无基础设施依赖，保持领域服务纯净性。
 */
@Service
public class ProductDomainService {

    /**
     * 校验条款列表是否包含主条款
     */
    public boolean validateMainClause(List<String> clauseIds, String mainClauseId) {
        if (clauseIds == null || clauseIds.isEmpty()) {
            return false;
        }
        return clauseIds.contains(mainClauseId);
    }

    /**
     * 校验出单流程配置与出单模式的一致性
     */
    public boolean validateIssuanceConfig(IssuanceProcessConfig config) {
        if (config == null || config.issuanceMode() == null) {
            return false;
        }
        if (config.steps() == null || config.steps().isEmpty()) {
            return false;
        }
        ProductEnum.IssuanceMode mode = config.issuanceMode();
        int stepCount = config.steps().size();
        return switch (mode) {
            case ONE_STEP -> stepCount >= 1;
            case TWO_STEP -> stepCount >= 2;
            case THREE_STEP -> stepCount >= 3;
            case CUSTOM -> stepCount >= 1;
        };
    }

    /**
     * 校验附加险是否可以搭配主险 附加险的险种类型必须与主险一致或兼容
     */
    public boolean validateAttachProductCompatibility(String mainInsuranceType, String attachInsuranceType) {
        // 简单校验：同险种类型可搭配
        // 后续可扩展为更复杂的兼容矩阵
        return mainInsuranceType != null && mainInsuranceType.equals(attachInsuranceType);
    }
}
