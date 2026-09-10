package com.titanium.product.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.product.command.ReviseProductCommand;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.web.dto.ReviseProductDTO;

/** 修订产品请求 → 修订命令的映射回归测试（存量产品补配核保配置链路） */
class ProductWebMapperReviseTest {

    private static final String PRODUCT_ID = "PROD_001";

    private final ProductWebMapper mapper = Mappers.getMapper(ProductWebMapper.class);

    @Test
    @DisplayName("newProductId 缺省时由雪花算法生成，被修订产品ID透传")
    void shouldGenerateNewProductIdWhenAbsent() {
        ReviseProductDTO request = new ReviseProductDTO();
        request.setNewProductName("测试产品V2");

        ReviseProductCommand command = mapper.toCommand(request, PRODUCT_ID);

        assertThat(command.productId()).isEqualTo(PRODUCT_ID);
        assertThat(command.newProductId()).isNotBlank();
        assertThat(command.newProductName()).isEqualTo("测试产品V2");
    }

    @Test
    @DisplayName("newProductId 显式传入时原样透传（不重新生成）")
    void shouldKeepExplicitNewProductId() {
        ReviseProductDTO request = new ReviseProductDTO();
        request.setNewProductId("PROD_001_V2");

        ReviseProductCommand command = mapper.toCommand(request, PRODUCT_ID);

        assertThat(command.newProductId()).isEqualTo("PROD_001_V2");
    }

    @Test
    @DisplayName("条款关联三元组经便捷构造器派生相对标识与绑定时间")
    void shouldDeriveClauseRelIdentity() {
        ReviseProductDTO request = new ReviseProductDTO();
        request.setNewClauseRels(List.of(
                new ReviseProductDTO.ClauseRelDTO("CLAUSE_001", "V1.0", true),
                new ReviseProductDTO.ClauseRelDTO("CLAUSE_002", "V2.0", null)));

        List<ProductClauseRel> rels = mapper.toCommand(request, PRODUCT_ID).newClauseRels();

        assertThat(rels).hasSize(2);
        assertThat(rels.get(0).relId()).isEqualTo("CLAUSE_001_V1.0");
        assertThat(rels.get(0).isMainClause()).isTrue();
        assertThat(rels.get(0).bindTime()).isNotNull();
        assertThat(rels.get(1).isMainClause()).isFalse();
    }

    @Test
    @DisplayName("条款关联为空列表时安全返回空列表，为 null 时透传 null")
    void shouldHandleNullClauseRelsSafely() {
        ReviseProductDTO emptyListRequest = new ReviseProductDTO();
        emptyListRequest.setNewClauseRels(List.of());
        assertThat(mapper.toCommand(emptyListRequest, PRODUCT_ID).newClauseRels()).isEmpty();

        ReviseProductDTO nullRequest = new ReviseProductDTO();
        assertThat(mapper.toClauseRels(nullRequest.getNewClauseRels())).isNull();
    }

    @Test
    @DisplayName("定价模式编码还原为枚举，null 安全")
    void shouldRestorePricingModeFromCode() {
        ReviseProductDTO request = new ReviseProductDTO();
        request.setNewPricingMode("RATE_TABLE");
        assertThat(mapper.toCommand(request, PRODUCT_ID).newPricingMode())
                .isEqualTo(PricingMode.RATE_TABLE);

        ReviseProductDTO nullRequest = new ReviseProductDTO();
        assertThat(mapper.toCommand(nullRequest, PRODUCT_ID).newPricingMode()).isNull();
    }
}
