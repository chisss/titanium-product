package com.titanium.product.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * 停用产品模板命令
 */
public record DeactivateProductTemplateCommand(@TargetAggregateIdentifier String templateId, String tenantId) {
}
