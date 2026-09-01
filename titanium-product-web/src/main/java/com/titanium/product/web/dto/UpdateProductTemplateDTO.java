package com.titanium.product.web.dto;

import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.PolicyStage;
import com.titanium.product.valueobject.config.BillingConfig;
import com.titanium.product.valueobject.config.ClaimConfig;
import com.titanium.product.valueobject.config.DividendConfig;
import com.titanium.product.valueobject.config.MaintenanceConfig;
import com.titanium.product.valueobject.config.PolicyStructureConfig;
import com.titanium.product.valueobject.config.ReinsuranceConfig;
import com.titanium.product.valueobject.config.UnderwritingConfig;

import lombok.Data;

/**
 * 更新产品模板行为配置请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上，承载产品模板的行为配置（出单模式/出单阶段/核保/保单结构/保全/理赔/缴费/再保/分红）。
 * 由 {@code ProductTemplateWebMapper} 翻译为领域命令 {@code UpdateProductTemplateCommand}。字段直接承载领域值对象
 * （web 层可依赖 domain/valueobject），避免重复维护并行的手写 Request 结构；值对象的合法性由其紧凑构造器兜底。
 * 未承载的字段置空，由聚合根按更新语义处理（就地覆盖，null 表示不变更该项由投影层保留原值）。
 * </p>
 */
@Data
public class UpdateProductTemplateDTO {

    /** 模板名称 */
    private String templateName;
    /** 出单模式（ONE_STEP/TWO_STEP/THREE_STEP/CUSTOM） */
    private ProductEnum.IssuanceMode issuanceMode;
    /** 出单阶段定义列表 */
    private List<PolicyStage> policyStages;
    /** 核保配置（核保模式/自动核保条件/转人工阈值/必需材料/时效/加费/特别约定） */
    private UnderwritingConfig underwritingConfig;
    /** 保单结构配置（标的类型/标的字段Schema/多标的/参与方角色/责任结构） */
    private PolicyStructureConfig policyStructure;
    /** 保全配置（允许保全类型/犹豫期/退保规则/批改规则） */
    private MaintenanceConfig maintenanceConfig;
    /** 理赔配置（理赔阶段/报案时效/等待期/理赔规则集/所需材料） */
    private ClaimConfig claimConfig;
    /** 缴费配置（允许缴费方式/宽限期/失效天数/自动扣款） */
    private BillingConfig billingConfig;
    /** 再保险配置（是否自动分保/自留保额上限/默认再保合约） */
    private ReinsuranceConfig reinsuranceConfig;
    /** 分红配置（红利分配方式/低中高档演示利率，分红险专属） */
    private DividendConfig dividendConfig;
}
