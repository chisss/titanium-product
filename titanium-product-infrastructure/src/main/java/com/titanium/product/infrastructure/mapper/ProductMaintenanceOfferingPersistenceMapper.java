package com.titanium.product.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.infrastructure.maintenance.entity.ProductMaintenanceOfferingDO;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;

/**
 * 保全 Offering 领域对象 → 持久化对象声明式映射。
 * 允许状态/渠道/保全项集合 JSON 序列化落库。
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ProductMaintenanceOfferingPersistenceMapper {

    /** Offering 主表映射：三个允许集合 JSON 序列化，其余同名字段自动映射。 */
    @Mapping(target = "allowedPolicyStatusesJson", source = "allowedPolicyStatuses", qualifiedByName = "toJson")
    @Mapping(target = "allowedChannelsJson", source = "allowedChannels", qualifiedByName = "toJson")
    @Mapping(target = "allowedItemCodesJson", source = "allowedItemCodes", qualifiedByName = "toJson")
    ProductMaintenanceOfferingDO toDO(ProductMaintenanceOffering offering);

    /** 任意对象 → JSON 字符串（null 安全）。 */
    @Named("toJson")
    default String toJson(Object value) {
        return value != null ? JSON.toJSONString(value) : null;
    }
}
