package com.titanium.product.domain.aggregate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.command.AuditProductCommand;
import com.titanium.product.domain.command.CreateProductCommand;
import com.titanium.product.domain.command.InvalidateProductCommand;
import com.titanium.product.domain.command.ReviseProductCommand;
import com.titanium.product.domain.entity.ProductClauseRel;
import com.titanium.product.domain.event.ProductAuditedEvent;
import com.titanium.product.domain.event.ProductCreatedEvent;
import com.titanium.product.domain.event.ProductInvalidatedEvent;
import com.titanium.product.domain.event.ProductRevisedEvent;
import com.titanium.product.domain.valueobject.InsureCondition;
import com.titanium.product.domain.valueobject.PricingBasicRule;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 保险产品聚合根 核心聚合根，封装产品的基础信息、形态、险种、绑定条款、定价基础规则等核心配置
 */
@Getter
@Builder(builderMethodName = "builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 为 Builder 提供全参构造函数
@Aggregate
public class InsuranceProduct {
    // 聚合根ID：产品编号（如 P2024_CAR_IND_001：2024年车险个险产品001）
    @AggregateIdentifier
    private String                    productId;
    // 产品名称（如 2024版个人机动车综合险）
    private String                    productName;
    // 产品形态（团险/个险）
    private ProductEnum.ProductForm   form;
    // 险种类型（车险/寿险/意外险/宠物险/财产险/投连险）
    private InsuranceType             insuranceType;
    // 产品版本（如 V1.0/V2.0，修订时递增）
    private String                    version;
    // 产品状态（DRAFT-草稿/AUDITING-审核中/EFFECTIVE-生效/INVALID-下架）
    private ProductEnum.ProductStatus status;
    // 生效时间（生效状态有效）
    private LocalDateTime             effectiveTime;
    // 下架时间（下架状态有效）
    private LocalDateTime             invalidTime;
    // 投保条件（值对象，如年龄范围、职业限制）
    private InsureCondition           insureCondition;
    // 产品绑定的条款关联列表（聚合内实体，含条款ID、版本、是否主条款）
    private List<ProductClauseRel>    clauseRels;
    // 定价基础规则（值对象，不同险种形态差异化配置）
    private PricingBasicRule          pricingBasicRule;

    // 聚合根初始化：创建产品（CommandHandler）
    @CommandHandler
    public InsuranceProduct(CreateProductCommand command) {
        // 业务校验：核心参数非空、条款列表非空、投保条件合法
        validateCreateCommand(command);
        // 初始化基础属性
        this.productId = command.productId();
        this.productName = command.productName();
        this.form = command.form();
        this.insuranceType = command.insuranceType();
        this.version = "V1.0";
        this.status = ProductEnum.ProductStatus.DRAFT;
        this.insureCondition = command.insureCondition();
        // 封装条款关联（关联条款ID、版本，标记主条款）
        this.clauseRels = command
                .clauseIds().stream().map(clauseId -> new ProductClauseRel(clauseId,
                        command.clauseVersionMap().get(clauseId), clauseId.equals(command.mainClauseId())))
                .collect(Collectors.toList());
        // 绑定定价基础规则
        this.pricingBasicRule = command.pricingBasicRule();
        // 发布领域事件
        AggregateLifecycle.apply(new ProductCreatedEvent(productId, productName, form, insuranceType, version,
                ProductEnum.ProductStatus.DRAFT, LocalDateTime.now(), insureCondition, clauseRels, pricingBasicRule));
    }

    // 业务方法：产品审核通过（生效）
    @CommandHandler
    public void handle(AuditProductCommand command) {
        // 规则校验：仅草稿/审核中状态可审核通过
        if (!this.status.equals(ProductEnum.ProductStatus.DRAFT)
                && !this.status.equals(ProductEnum.ProductStatus.AUDITING)) {
            throw new IllegalStateException("仅草稿、审核中产品可审核通过");
        }
        // 状态更新
        this.status = ProductEnum.ProductStatus.EFFECTIVE;
        this.effectiveTime = LocalDateTime.now();
        // 发布事件（同步通知条款域、保单域）
        AggregateLifecycle
                .apply(new ProductAuditedEvent(productId, ProductEnum.ProductStatus.EFFECTIVE, effectiveTime));
    }

    // 业务方法：产品修订（生成新版本）
    @CommandHandler
    public void handle(ReviseProductCommand command) {
        // 规则校验：仅生效状态产品可修订
        if (!this.status.equals(ProductEnum.ProductStatus.EFFECTIVE)) {
            throw new IllegalStateException("仅生效产品可修订");
        }
        // 生成新版本（如 V1.0 → V2.0）
        String newVersion = "V" + (Integer.parseInt(this.version.substring(1)) + 1) + ".0";
        // 发布修订事件（新版本聚合根由事件溯源生成）
        AggregateLifecycle.apply(new ProductRevisedEvent(command.newProductId(), this.productId, newVersion,
                command.newProductName(), command.newForm(), command.newInsuranceType(), command.newInsureCondition(),
                command.newClauseRels(), command.newPricingBasicRule()));
    }

    // 业务方法：产品下架（废止）
    @CommandHandler
    public void handle(InvalidateProductCommand command) {
        if (!this.status.equals(ProductEnum.ProductStatus.EFFECTIVE)) {
            throw new IllegalStateException("仅生效产品可下架");
        }
        this.status = ProductEnum.ProductStatus.INVALID;
        this.invalidTime = LocalDateTime.now();
        AggregateLifecycle
                .apply(new ProductInvalidatedEvent(productId, ProductEnum.ProductStatus.INVALID, invalidTime));
    }

    // 事件溯源处理器：同步聚合根状态
    @EventSourcingHandler
    public void on(ProductCreatedEvent event) {
        this.productId = event.productId();
        this.productName = event.productName();
        this.form = event.form();
        this.insuranceType = event.insuranceType();
        this.version = event.version();
        this.status = event.status();
        this.insureCondition = event.insureCondition();
        this.clauseRels = event.clauseRels();
        this.pricingBasicRule = event.pricingBasicRule();
    }

    @EventSourcingHandler
    public void on(ProductAuditedEvent event) {
        this.status = event.status();
        this.effectiveTime = event.effectiveTime();
    }

    @EventSourcingHandler
    public void on(ProductRevisedEvent event) {
        this.productId = event.newProductId();
        this.productName = event.newProductName();
        this.version = event.newVersion();
        this.form = event.newForm();
        this.insuranceType = event.newInsuranceType();
        this.insureCondition = event.newInsureCondition();
        this.clauseRels = event.newClauseRels();
        this.pricingBasicRule = event.newPricingBasicRule();
        this.status = ProductEnum.ProductStatus.DRAFT; // 新版本默认草稿状态
    }

    @EventSourcingHandler
    public void on(ProductInvalidatedEvent event) {
        this.status = event.status();
        this.invalidTime = event.invalidTime();
    }

    // 私有校验方法
    private void validateCreateCommand(CreateProductCommand command) {
        if (command.productId() == null || command.productName() == null) {
            throw new IllegalArgumentException("产品编号、名称不能为空");
        }
        if (command.clauseIds() == null || command.clauseIds().isEmpty()) {
            throw new IllegalArgumentException("产品必须绑定至少一条条款");
        }
        if (command.insureCondition() == null) {
            throw new IllegalArgumentException("投保条件不能为空");
        }
    }

    // 无参构造器（Axon 反射必备）
    public InsuranceProduct() {
    }
}
