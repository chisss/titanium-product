package com.titanium.product.valueobject.config;

import java.io.Serializable;
import java.util.List;

import com.titanium.metadata.enums.insurance.SubjectType;
import com.titanium.product.common.enums.LiabilityStructure;

/**
 * 保单结构配置值对象 定义产品的保单结构，包含标的类型、参与方角色、责任结构等
 *
 * @param subjectType 标的类型
 * @param subjectFieldsSchema 标的必填字段 (JSON Schema)
 * @param allowMultipleSubjects 是否允许多标的（团险）
 * @param partyRoles 参与方角色列表
 * @param requiredPartyRoles 必需的参与方角色
 * @param liabilityStructure 责任结构类型
 */
public record PolicyStructureConfig(SubjectType subjectType, String subjectFieldsSchema, boolean allowMultipleSubjects,
                                    List<String> partyRoles, List<String> requiredPartyRoles,
                                    LiabilityStructure liabilityStructure)
        implements
            Serializable {
}
