package com.titanium.product.infrastructure.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 税基费用项关系。
 */
@Entity
@Table(name = "t_product_tax_base_item")
@Getter
@Setter
public class TaxBaseItemDO {

    @Id
    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;
    @Column(name = "policy_id", nullable = false, length = 36)
    private String policyId;
    @Column(name = "component_code", nullable = false, length = 64)
    private String componentCode;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
