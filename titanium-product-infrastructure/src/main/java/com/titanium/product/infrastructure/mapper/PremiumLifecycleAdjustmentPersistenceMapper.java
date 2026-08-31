package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.titanium.product.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.infrastructure.pricing.entity.PremiumLifecycleAdjustmentDO;
import com.titanium.product.infrastructure.pricing.entity.PremiumLifecycleDifferenceLineDO;
import com.titanium.product.infrastructure.pricing.entity.PremiumLifecycleDifferenceLineId;
import com.titanium.product.valueobject.pricing.lifecycle.PremiumLifecycleDifferenceLine;

/**
 * 保费生命周期调整领域对象 → 持久化对象声明式映射。
 * 差额明细行复合主键 {@link PremiumLifecycleDifferenceLineId} 由调整ID与行标识构成。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        imports = PremiumLifecycleDifferenceLineId.class)
public interface PremiumLifecycleAdjustmentPersistenceMapper {

    /** 调整主表映射（同名字段自动映射）。 */
    PremiumLifecycleAdjustmentDO toDO(PremiumLifecycleAdjustment adjustment);

    /** 差额明细行映射：复合主键由调整ID与行标识构成。 */
    @Mapping(target = "id",
            expression = "java(new PremiumLifecycleDifferenceLineId(adjustmentId, line.lineId()))")
    PremiumLifecycleDifferenceLineDO toDO(String adjustmentId, PremiumLifecycleDifferenceLine line);
}
