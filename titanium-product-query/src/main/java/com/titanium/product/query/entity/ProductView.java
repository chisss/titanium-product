package com.titanium.product.query.entity;

import java.time.LocalDateTime;

import com.titanium.common.jpa.BaseView;
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
 * 产品读模型实体（CQRS Projection）
 * <p>
 * 对应读模型表 {@code t_product_view}，与写侧聚合根持久化表 {@code t_product} 物理隔离。 由
 * {@link com.titanium.product.query.handler.ProductProjectionEventHandler} 订阅领域事件投影而来。
 * </p>
 * <p>
 * <b>设计说明</b>：可查询的标量字段（编码/名称/状态/险种/时间）独立建列以支持索引与条件查询； 复杂值对象配置（投保条件/保障期间/缴费/定价规则等）以 JSON
 * 形式整体存储，查询时反序列化， 与现有 {@code ProductTemplateEntity} 的 JSON 列模式保持一致。
 * </p>
 * <p>
 * 继承 {@link BaseView}，复用租户ID、创建/更新时间、乐观锁版本等读模型公共字段。
 * </p>
 */
@Entity
@Table(name = "t_product_view")
@Getter
@Setter
public class ProductView extends BaseView {

    /** 产品ID（聚合根ID，读模型主键） */
    @Id
    @Column(name = "product_id", nullable = false, length = 36)
    private String        productId;

    /** 产品编码 */
    @Column(name = "product_code", length = 64)
    private String        productCode;

    /** 产品名称 */
    @Column(name = "product_name", length = 128)
    private String        productName;

    /** 产品描述 */
    @Column(name = "product_desc", length = 512)
    private String        productDesc;

    /** 产品形态（来源 ProductForm 枚举名） */
    @Enumerated(EnumType.STRING)
    @Column(name = "form", length = 32)
    private ProductEnum.ProductForm form;

    /** 险种类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_type", length = 32)
    private InsuranceType insuranceType;

    /** 产品类别 */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32)
    private ProductEnum.ProductCategory category;

    /** 版本号 */
    @Column(name = "version_no", length = 32)
    private String        versionNo;

    /** 产品状态（DRAFT/AUDITING/EFFECTIVE/INVALID） */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProductEnum.ProductStatus status;

    /** 修订溯源：原始产品ID */
    @Column(name = "original_product_id", length = 36)
    private String        originalProductId;

    /** 生效时间 */
    @Column(name = "effective_time")
    private LocalDateTime effectiveTime;

    /** 下架/失效时间 */
    @Column(name = "invalid_time")
    private LocalDateTime invalidTime;

    /** 销售起期 */
    @Column(name = "sale_start_time")
    private LocalDateTime saleStartTime;

    /** 销售止期 */
    @Column(name = "sale_end_time")
    private LocalDateTime saleEndTime;

    /** 投保条件配置（JSON） */
    @Lob
    @Column(name = "insure_condition_json", columnDefinition = "TEXT")
    private String        insureConditionJson;

    /** 保障期间配置（JSON） */
    @Lob
    @Column(name = "coverage_period_json", columnDefinition = "TEXT")
    private String        coveragePeriodJson;

    /** 缴费配置（JSON） */
    @Lob
    @Column(name = "payment_config_json", columnDefinition = "TEXT")
    private String        paymentConfigJson;

    /** 定价基础规则（JSON） */
    @Lob
    @Column(name = "pricing_basic_rule_json", columnDefinition = "TEXT")
    private String        pricingBasicRuleJson;

    /** 出单流程配置（JSON） */
    @Lob
    @Column(name = "issuance_process_config_json", columnDefinition = "TEXT")
    private String        issuanceProcessConfigJson;

    /** 保单形态配置（JSON） */
    @Lob
    @Column(name = "policy_form_config_json", columnDefinition = "TEXT")
    private String        policyFormConfigJson;

    /** 核保配置（JSON） */
    @Lob
    @Column(name = "underwriting_config_json", columnDefinition = "TEXT")
    private String        underwritingConfigJson;

    /** 审核信息（JSON） */
    @Lob
    @Column(name = "audit_info_json", columnDefinition = "TEXT")
    private String        auditInfoJson;

    /** 业务创建时间（来源事件） */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
