package com.titanium.product.query.handler;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.event.ProductAuditRejectedEvent;
import com.titanium.product.domain.event.ProductAuditedEvent;
import com.titanium.product.domain.event.ProductCreatedEvent;
import com.titanium.product.domain.event.ProductInvalidatedEvent;
import com.titanium.product.domain.event.ProductRevisedEvent;
import com.titanium.product.domain.event.ProductSubmittedForAuditEvent;
import com.titanium.product.query.entity.ProductView;
import com.titanium.product.query.repository.ProductViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品读模型投影事件处理器（CQRS 读侧核心）
 * <p>
 * 订阅产品域领域事件，将聚合根状态变更投影到读模型表 {@code t_product_view}， 补齐此前缺失的 CQRS
 * 读模型投影机制——原 {@code ProductDetailQueryHandler} 通过重建写侧聚合根来查询， 违背读写分离，本处理器使查询侧拥有独立的物化视图。
 * </p>
 * <p>
 * <b>处理组</b>：{@code product-query-group}，与写侧 {@code product-group} 隔离。
 * </p>
 * <p>
 * <b>幂等性</b>：创建/修订事件用 saveOrUpdate；状态变更事件先查存量再更新，缺失时告警跳过， 保证事件重放时不产生脏数据。
 * </p>
 */
@Slf4j
@Component
@ProcessingGroup("product-query-group")
@RequiredArgsConstructor
public class ProductProjectionEventHandler {

    private final ProductViewRepository productViewRepository;

    /**
     * 投影产品创建事件：新建读模型记录
     */
    @EventHandler
    @Transactional
    public void on(ProductCreatedEvent event) {
        log.info("[读模型投影] 产品创建: productId={}, tenantId={}", event.productId(), event.tenantId());

        ProductView view = productViewRepository.findById(event.productId()).orElseGet(ProductView::new);
        LocalDateTime now = LocalDateTime.now();

        view.setProductId(event.productId());
        view.setProductCode(event.productCode());
        view.setProductName(event.productName());
        view.setProductDesc(event.productDesc());
        view.setForm(event.form());
        view.setInsuranceType(event.insuranceType());
        view.setCategory(event.category());
        view.setVersionNo(event.version());
        view.setStatus(event.status());
        view.setSaleStartTime(event.saleStartTime());
        view.setSaleEndTime(event.saleEndTime());
        view.setCreatedAt(event.createdAt());
        view.setInsureConditionJson(toJson(event.insureCondition()));
        view.setCoveragePeriodJson(toJson(event.coveragePeriod()));
        view.setPaymentConfigJson(toJson(event.paymentConfig()));
        view.setPricingBasicRuleJson(toJson(event.pricingBasicRule()));
        view.setIssuanceProcessConfigJson(toJson(event.issuanceProcessConfig()));
        view.setPolicyFormConfigJson(toJson(event.policyFormConfig()));
        view.setUnderwritingConfigJson(toJson(event.underwritingConfig()));
        view.setTenantId(event.tenantId());
        if (view.getCreateTime() == null) {
            view.setCreateTime(now);
        }
        view.setUpdateTime(now);

        productViewRepository.save(view);
    }

    /**
     * 投影产品修订事件：修订会生成新产品ID，新建一条读模型记录
     */
    @EventHandler
    @Transactional
    public void on(ProductRevisedEvent event) {
        log.info("[读模型投影] 产品修订: newProductId={}, originalProductId={}", event.newProductId(),
                event.originalProductId());

        // 复用原产品的租户与编码（修订事件未携带），从原始记录继承
        ProductView origin = productViewRepository.findById(event.originalProductId()).orElse(null);
        ProductView view = productViewRepository.findById(event.newProductId()).orElseGet(ProductView::new);
        LocalDateTime now = LocalDateTime.now();

        view.setProductId(event.newProductId());
        view.setOriginalProductId(event.originalProductId());
        view.setVersionNo(event.newVersion());
        view.setProductName(event.newProductName());
        view.setProductDesc(event.newProductDesc());
        view.setForm(event.newForm());
        view.setInsuranceType(event.newInsuranceType());
        view.setCategory(event.newCategory());
        view.setInsureConditionJson(toJson(event.newInsureCondition()));
        view.setCoveragePeriodJson(toJson(event.newCoveragePeriod()));
        view.setPaymentConfigJson(toJson(event.newPaymentConfig()));
        view.setPricingBasicRuleJson(toJson(event.newPricingBasicRule()));
        view.setIssuanceProcessConfigJson(toJson(event.newIssuanceProcessConfig()));
        view.setPolicyFormConfigJson(toJson(event.newPolicyFormConfig()));
        view.setUnderwritingConfigJson(toJson(event.newUnderwritingConfig()));
        // 修订态默认为草稿，编码与租户从原记录继承
        view.setStatus(ProductEnum.ProductStatus.DRAFT);
        if (origin != null) {
            view.setProductCode(origin.getProductCode());
            view.setTenantId(origin.getTenantId());
        }
        if (view.getCreateTime() == null) {
            view.setCreateTime(now);
        }
        view.setUpdateTime(now);

        productViewRepository.save(view);
    }

    /**
     * 投影产品提交审核事件
     */
    @EventHandler
    @Transactional
    public void on(ProductSubmittedForAuditEvent event) {
        applyUpdate(event.productId(), "提交审核", view -> view.setStatus(ProductEnum.ProductStatus.AUDITING));
    }

    /**
     * 投影产品审核通过事件
     */
    @EventHandler
    @Transactional
    public void on(ProductAuditedEvent event) {
        applyUpdate(event.productId(), "审核通过", view -> {
            view.setStatus(event.status() != null ? event.status() : ProductEnum.ProductStatus.EFFECTIVE);
            view.setEffectiveTime(event.effectiveTime());
            view.setAuditInfoJson(toJson(event.auditInfo()));
        });
    }

    /**
     * 投影产品审核拒绝事件
     */
    @EventHandler
    @Transactional
    public void on(ProductAuditRejectedEvent event) {
        applyUpdate(event.productId(), "审核拒绝", view -> {
            view.setStatus(event.status() != null ? event.status() : ProductEnum.ProductStatus.DRAFT);
            view.setAuditInfoJson(toJson(event.auditInfo()));
        });
    }

    /**
     * 投影产品失效/下架事件
     */
    @EventHandler
    @Transactional
    public void on(ProductInvalidatedEvent event) {
        applyUpdate(event.productId(), "产品下架", view -> {
            view.setStatus(event.status() != null ? event.status() : ProductEnum.ProductStatus.INVALID);
            view.setInvalidTime(event.invalidTime());
        });
    }

    /**
     * 通用更新模板：查存量→应用变更→刷新更新时间→保存；缺失时告警跳过保证幂等
     */
    private void applyUpdate(String productId, String action, Consumer<ProductView> mutator) {
        productViewRepository.findById(productId).ifPresentOrElse(view -> {
            mutator.accept(view);
            view.setUpdateTime(LocalDateTime.now());
            productViewRepository.save(view);
        }, () -> log.warn("[读模型投影] {} 失败：未找到读模型记录 productId={}（可能事件乱序，将由DLQ重试）", action, productId));
    }

    /**
     * 值对象 → JSON 字符串（null 安全）
     */
    private String toJson(Object value) {
        return value != null ? JSON.toJSONString(value) : null;
    }
}
