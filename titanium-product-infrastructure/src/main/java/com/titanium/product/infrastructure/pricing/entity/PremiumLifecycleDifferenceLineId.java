package com.titanium.product.infrastructure.pricing.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 生命周期差额行复合主键。
 */
@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PremiumLifecycleDifferenceLineId implements Serializable {

    @Column(name = "adjustment_id", nullable = false, length = 36)
    private String adjustmentId;

    @Column(name = "line_id", nullable = false, length = 64)
    private String lineId;
}
