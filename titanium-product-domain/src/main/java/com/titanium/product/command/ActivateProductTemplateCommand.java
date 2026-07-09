package com.titanium.product.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * 激活产品模板命令
 */
public record ActivateProductTemplateCommand(@TargetAggregateIdentifier String templateId, String tenantId) {
}
