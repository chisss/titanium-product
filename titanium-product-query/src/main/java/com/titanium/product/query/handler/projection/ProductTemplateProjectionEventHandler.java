package com.titanium.product.query.handler.projection;

import java.time.LocalDateTime;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.product.event.LifeProductConfiguredEvent;
import com.titanium.product.event.ProductTemplateActivatedEvent;
import com.titanium.product.event.ProductTemplateCreatedEvent;
import com.titanium.product.event.ProductTemplateDeactivatedEvent;
import com.titanium.product.event.ProductTemplateUpdatedEvent;
import com.titanium.product.query.repository.ProductTemplateViewRepository;
import com.titanium.product.query.view.ProductTemplateView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品模板读模型投影事件处理器（CQRS 读侧）
 * <p>
 * 订阅产品模板域领域事件，将模板状态变更投影到读模型表 {@code t_product_template_view}。 复杂值对象配置以 JSON
 * 整体序列化存储，查询侧拥有独立物化视图，与写侧持久化彻底隔离。
 * </p>
 * <p>
 * <b>处理组</b>：{@code product-query-group}，与写侧 {@code product-group} 隔离。
 * </p>
 */
@Slf4j
@Component
@ProcessingGroup("product-query-group")
@RequiredArgsConstructor
public class ProductTemplateProjectionEventHandler {

    private final ProductTemplateViewRepository templateViewRepository;

    /**
     * 投影产品模板创建事件：新建读模型记录
     */
    @EventHandler
    @Transactional
    public void on(ProductTemplateCreatedEvent event) {
        log.info("[读模型投影] 产品模板创建: templateId={}, tenantId={}", event.templateId(), event.tenantId());

        ProductTemplateView view =
                templateViewRepository.findById(event.templateId()).orElseGet(ProductTemplateView::new);
        LocalDateTime now = LocalDateTime.now();

        view.setTemplateId(event.templateId());
        view.setTemplateCode(event.templateCode());
        view.setTemplateName(event.templateName());
        view.setInsuranceType(event.insuranceType());
        view.setIssuanceMode(event.issuanceProcessConfig() != null
                ? JSON.toJSONString(event.issuanceProcessConfig()) : "DEFAULT");
        view.setUnderwritingConfigJson(toJson(event.underwritingConfig()));
        view.setMaintenanceConfigJson(toJson(event.maintenanceConfig()));
        view.setClaimConfigJson(toJson(event.claimsConfig()));
        view.setPolicyStructureJson(toJson(event.policyFormConfig()));
        view.setStatus(event.status());
        view.setTenantId(event.tenantId());
        if (view.getCreateTime() == null) {
            view.setCreateTime(now);
        }
        view.setUpdateTime(now);

        templateViewRepository.save(view);
    }

    /**
     * 投影产品模板更新事件
     */
    @EventHandler
    @Transactional
    public void on(ProductTemplateUpdatedEvent event) {
        templateViewRepository.findById(event.templateId()).ifPresentOrElse(view -> {
            view.setTemplateName(event.templateName());
            view.setIssuanceMode(event.issuanceMode() != null ? event.issuanceMode().name() : view.getIssuanceMode());
            view.setPolicyStagesJson(toJson(event.policyStages()));
            view.setUnderwritingConfigJson(toJson(event.underwritingConfig()));
            view.setPolicyStructureJson(toJson(event.policyStructure()));
            view.setMaintenanceConfigJson(toJson(event.maintenanceConfig()));
            view.setClaimConfigJson(toJson(event.claimConfig()));
            view.setBillingConfigJson(toJson(event.billingConfig()));
            view.setReinsuranceConfigJson(toJson(event.reinsuranceConfig()));
            view.setUpdateTime(LocalDateTime.now());
            templateViewRepository.save(view);
        }, () -> log.warn("[读模型投影] 模板更新失败：未找到读模型记录 templateId={}（可能事件乱序，将由DLQ重试）",
                event.templateId()));
    }

    /**
     * 投影寿险产品规格配置事件：将寿险规格（投保年龄/保额/缴费期/保障期）以 JSON 写入读模型
     */
    @EventHandler
    @Transactional
    public void on(LifeProductConfiguredEvent event) {
        templateViewRepository.findById(event.templateId()).ifPresentOrElse(view -> {
            view.setLifeProductSpecJson(toJson(event.lifeProductSpec()));
            view.setUpdateTime(LocalDateTime.now());
            templateViewRepository.save(view);
        }, () -> log.warn("[读模型投影] 寿险规格配置失败：未找到读模型记录 templateId={}（可能事件乱序，将由DLQ重试）",
                event.templateId()));
    }

    /**
     * 投影产品模板激活事件
     */
    @EventHandler
    @Transactional
    public void on(ProductTemplateActivatedEvent event) {
        applyStatus(event.templateId(), CommonStatus.ACTIVE, "模板激活");
    }

    /**
     * 投影产品模板停用事件
     */
    @EventHandler
    @Transactional
    public void on(ProductTemplateDeactivatedEvent event) {
        applyStatus(event.templateId(), CommonStatus.INACTIVE, "模板停用");
    }

    /**
     * 通用状态更新：查存量→改状态→刷新更新时间→保存；缺失时告警跳过保证幂等
     */
    private void applyStatus(String templateId, CommonStatus status, String action) {
        templateViewRepository.findById(templateId).ifPresentOrElse(view -> {
            view.setStatus(status);
            view.setUpdateTime(LocalDateTime.now());
            templateViewRepository.save(view);
        }, () -> log.warn("[读模型投影] {} 失败：未找到读模型记录 templateId={}（可能事件乱序，将由DLQ重试）", action,
                templateId));
    }

    /**
     * 值对象 → JSON 字符串（null 安全）
     */
    private String toJson(Object value) {
        return value != null ? JSON.toJSONString(value) : null;
    }
}
