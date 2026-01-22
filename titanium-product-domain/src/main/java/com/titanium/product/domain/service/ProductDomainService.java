package com.titanium.product.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * 产品领域服务 处理跨聚合根的业务逻辑，如产品条款绑定校验、版本号生成等
 */
@Service
public class ProductDomainService {
    /**
     * 生成新版本号
     *
     * @param currentVersion 当前版本号
     * @return 新版本号
     */
    public String generateNewVersion(String currentVersion) {
        // 版本号格式：V1.0, V2.0等
        int currentMajorVersion = Integer.parseInt(currentVersion.substring(1));
        int newMajorVersion = currentMajorVersion + 1;
        return "V" + newMajorVersion + ".0";
    }

    /**
     * 校验条款列表是否包含主条款
     *
     * @param clauseIds 条款ID列表
     * @param mainClauseId 主条款ID
     * @return 是否包含主条款
     */
    public boolean validateMainClause(List<String> clauseIds, String mainClauseId) {
        if (clauseIds == null || clauseIds.isEmpty()) {
            return false;
        }
        return clauseIds.contains(mainClauseId);
    }
}
