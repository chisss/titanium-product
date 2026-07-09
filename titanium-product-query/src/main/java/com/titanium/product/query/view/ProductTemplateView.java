package com.titanium.product.query.view;

import com.titanium.common.jpa.BaseView;
import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 产品模板读模型实体（CQRS Projection）
 * <p>
 * 对应读模型表 {@code t_product_template_view}，由
 * {@link com.titanium.product.query.handler.projection.ProductTemplateProjectionEventHandler} 订阅模板域事件投影而来，
 * 与写侧模板持久化表 {@code t_product_template} 物理隔离。 复杂值对象配置以 JSON 形式整体存储，查询时反序列化。
 * </p>
 * <p>
 * 继承 {@link BaseView}，复用租户ID、创建/更新时间、乐观锁版本等读模型公共字段。
 * </p>
 */
@Entity
@Table(name = "t_product_template_view")
@Getter
@Setter
public class ProductTemplateView extends BaseView {

    /** 模板ID（读模型主键） */
    @Id
    @Column(name = "template_id", nullable = false, length = 36)
    private String                      templateId;

    /** 模板编码 */
    @Column(name = "template_code", length = 64)
    private String                      templateCode;

    /** 模板名称 */
    @Column(name = "template_name", length = 128)
    private String                      templateName;

    /** 险种大类 */
    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_category", length = 32)
    private ProductEnum.ProductCategory insuranceCategory;

    /** 险种类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_type", length = 32)
    private InsuranceType               insuranceType;

    /** 关联产品ID */
    @Column(name = "product_id", length = 36)
    private String                      productId;

    /** 出单模式 */
    @Column(name = "issuance_mode", length = 512)
    private String                      issuanceMode;

    /** 出单阶段定义（JSON） */
    @Lob
    @Column(name = "policy_stages_json", columnDefinition = "TEXT")
    private String                      policyStagesJson;

    /** 核保配置（JSON） */
    @Lob
    @Column(name = "underwriting_config_json", columnDefinition = "TEXT")
    private String                      underwritingConfigJson;

    /** 保单结构配置（JSON） */
    @Lob
    @Column(name = "policy_structure_json", columnDefinition = "TEXT")
    private String                      policyStructureJson;

    /** 保全配置（JSON） */
    @Lob
    @Column(name = "maintenance_config_json", columnDefinition = "TEXT")
    private String                      maintenanceConfigJson;

    /** 理赔配置（JSON） */
    @Lob
    @Column(name = "claim_config_json", columnDefinition = "TEXT")
    private String                      claimConfigJson;

    /** 计费配置（JSON） */
    @Lob
    @Column(name = "billing_config_json", columnDefinition = "TEXT")
    private String                      billingConfigJson;

    /** 再保险配置（JSON） */
    @Lob
    @Column(name = "reinsurance_config_json", columnDefinition = "TEXT")
    private String                      reinsuranceConfigJson;

    /** 模板状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private CommonStatus                status;
}
