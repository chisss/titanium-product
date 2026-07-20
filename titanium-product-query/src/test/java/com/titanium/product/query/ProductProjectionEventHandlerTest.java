package com.titanium.product.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.event.ProductSalesChannelUpdatedEvent;
import com.titanium.product.query.handler.projection.ProductProjectionEventHandler;
import com.titanium.product.query.mapper.ProductViewMapper;
import com.titanium.product.query.repository.ProductViewRepository;
import com.titanium.product.query.view.ProductView;
import com.titanium.product.valueobject.SalesChannelConfig;

/**
 * 产品读模型销售渠道投影测试
 * <p>
 * 覆盖 {@link ProductProjectionEventHandler#on(ProductSalesChannelUpdatedEvent)}： 验证「更新」语义——按
 * productId 加载存量读模型后，将销售渠道配置列表整体序列化写入 sales_channels_json； 读模型缺失时告警跳过不落库。仅用 mockito-core
 * 手动构造替身。
 * </p>
 */
class ProductProjectionEventHandlerTest {

    @Test
    @DisplayName("销售渠道更新事件：加载存量读模型后整列覆盖 sales_channels_json")
    void shouldProjectSalesChannelsToExistingView() {
        ProductViewRepository repository = mock(ProductViewRepository.class);
        ProductView existing = new ProductView();
        existing.setProductId("PROD_001");
        existing.setProductCode("CODE_001");
        when(repository.findById(eq("PROD_001"))).thenReturn(Optional.of(existing));
        ProductProjectionEventHandler handler =
                new ProductProjectionEventHandler(repository, mock(ProductViewMapper.class));

        List<SalesChannelConfig> channels = List.of(
                new SalesChannelConfig(ProductEnum.SalesChannel.AGENT, true, new BigDecimal("0.15")),
                new SalesChannelConfig(ProductEnum.SalesChannel.ONLINE, false, new BigDecimal("0.05")));
        handler.on(new ProductSalesChannelUpdatedEvent("PROD_001", channels));

        ArgumentCaptor<ProductView> captor = ArgumentCaptor.forClass(ProductView.class);
        verify(repository).save(captor.capture());
        ProductView saved = captor.getValue();
        assertEquals("CODE_001", saved.getProductCode());
        assertNotNull(saved.getSalesChannelsJson());
        List<SalesChannelConfig> parsed = JSON.parseArray(saved.getSalesChannelsJson(), SalesChannelConfig.class);
        assertEquals(2, parsed.size());
        assertEquals(ProductEnum.SalesChannel.AGENT, parsed.get(0).channelType());
        assertEquals(new BigDecimal("0.15"), parsed.get(0).commissionRate());
        assertEquals(ProductEnum.SalesChannel.ONLINE, parsed.get(1).channelType());
    }

    @Test
    @DisplayName("销售渠道更新事件：读模型缺失时告警跳过，不落库")
    void shouldSkipWhenViewMissing() {
        ProductViewRepository repository = mock(ProductViewRepository.class);
        when(repository.findById(eq("PROD_404"))).thenReturn(Optional.empty());
        ProductProjectionEventHandler handler =
                new ProductProjectionEventHandler(repository, mock(ProductViewMapper.class));

        handler.on(new ProductSalesChannelUpdatedEvent("PROD_404",
                List.of(new SalesChannelConfig(ProductEnum.SalesChannel.BROKER, true, BigDecimal.ONE))));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
