package com.titanium.product.domain.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.valueobject.IssuanceProcessConfig;

/**
 * 产品领域服务
 * 处理跨聚合根的业务逻辑，如产品条款绑定校验、版本号生成、出单配置校验等
 */
@Service
public class ProductDomainService {

    /**
     * 生成新版本号
     */
    public String generateNewVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isBlank()) {
            return "V1.0";
        }
        int dotIndex = currentVersion.indexOf('.');
        if (dotIndex > 0) {
            int majorVersion = Integer.parseInt(currentVersion.substring(1, dotIndex));
            return "V" + (majorVersion + 1) + ".0";
        }
        int currentMajorVersion = Integer.parseInt(currentVersion.substring(1));
        return "V" + (currentMajorVersion + 1) + ".0";
    }

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
     * 生成产品ID
     */
    public String generateProductId() {
        return "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
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
     * 校验附加险是否可以搭配主险
     * 附加险的险种类型必须与主险一致或兼容
     */
    public boolean validateAttachProductCompatibility(String mainInsuranceType, String attachInsuranceType) {
        // 简单校验：同险种类型可搭配
        // 后续可扩展为更复杂的兼容矩阵
        return mainInsuranceType != null && mainInsuranceType.equals(attachInsuranceType);
    }
}
