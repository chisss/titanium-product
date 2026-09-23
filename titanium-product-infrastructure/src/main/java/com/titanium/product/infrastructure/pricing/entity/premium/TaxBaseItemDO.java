package com.titanium.product.infrastructure.pricing.entity.premium;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
    /**
     * 乐观锁版本（JPA 自动维护）。
     * <p>
     * m20-09（审查报告 PER-01）补齐：原先写侧 DO 无 {@code @Version}，并发更新走无条件 UPDATE，
     * 后写者静默覆盖前写者、数据丢失。加此字段后 JPA 的 UPDATE 会带 {@code WHERE version = ?}，
     * 冲突时抛 {@code ObjectOptimisticLockingFailureException}（由异常处理器映射为 409）。
     * </p>
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
