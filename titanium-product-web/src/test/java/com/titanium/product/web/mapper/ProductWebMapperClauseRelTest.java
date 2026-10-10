package com.titanium.product.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.titanium.product.api.request.product.UpdateProductClauseRelRequest;
import com.titanium.product.command.UpdateProductClauseRelCommand;
import com.titanium.product.entity.ProductClauseRel;
import com.titanium.product.web.dto.product.ClauseRelDTO;
import com.titanium.product.web.dto.product.UpdateProductClauseRelDTO;

/**
 * 条款绑定「整体替换」两条写入口的映射对称性测试（G01/AC-03）。
 *
 * <p>🔴 <b>判据是「同一个命令」而非「各自能跑通」</b>：web 入口（HTTP {@code DTO}）与 api 入口（Feign
 * {@code Request}）对<b>同一份逻辑输入</b>必须产出 productId / tenantId / 绑定清单逐字段相同的
 * {@link UpdateProductClauseRelCommand}。两条入口平行收敛到同一个 application 薄门面
 * （{@code ProductCommandAppService#updateProductClauseRel}），只要某一侧漏传租户、漏映射主条款标记
 * 或把空清单折成 null，命令形状就会分叉——而两侧各自的「能跑通」测试<b>都发现不了</b>这种分叉，
 * 故对称性必须由本类显式比对断言。</p>
 *
 * <p>⚠️ <b>唯一允许不同的字段是 {@code bindTime}</b>：两侧各自经 {@link ProductClauseRel} 三参便捷构造器
 * 取 {@code now()}，它不来自入参，故不参与对称性比对（但须断言非空）。其余四个字段（{@code relId}
 * 为派生标识、{@code clauseId}、{@code clauseVersion}、{@code isMainClause}）<b>必须逐字段相同</b>。</p>
 */
class ProductWebMapperClauseRelTest {

    private static final String PRODUCT_ID = "product-1";
    private static final String TENANT_ID  = "tenant-1";

    private final ProductWebMapper mapper = Mappers.getMapper(ProductWebMapper.class);

    @Test
    @DisplayName("🔴 两条写入口对同一份逻辑输入必须产出同一个命令（AC-03 对称性）")
    void webEntryAndApiEntryMustProduceSameCommand() {
        UpdateProductClauseRelCommand fromWeb = mapper.toCommand(webRequest(), PRODUCT_ID, TENANT_ID);
        UpdateProductClauseRelCommand fromApi = mapper.toCommand(apiRequest(), PRODUCT_ID, TENANT_ID);

        assertThat(fromWeb.productId()).isEqualTo(PRODUCT_ID);
        assertThat(fromApi.productId()).isEqualTo(PRODUCT_ID);
        assertThat(fromApi.tenantId()).isEqualTo(fromWeb.tenantId());

        // 除 bindTime 外逐字段比对（bindTime 两侧各自取 now()，不来自入参）
        assertThat(signatures(fromApi)).isEqualTo(signatures(fromWeb));
        assertThat(signatures(fromWeb)).containsExactly(
                "C-MAIN_V1.0|C-MAIN|V1.0|true",
                "C-RIDER_V1.0|C-RIDER|V1.0|false");
        assertThat(fromWeb.newClauseRels())
                .allSatisfy(rel -> assertThat(rel.bindTime()).isNotNull());
    }

    @Test
    @DisplayName("isMainClause 为 null 时两侧同判据（视同非主条款），不得一侧 true 一侧 false")
    void nullIsMainClauseMustBeTreatedAsNonMainOnBothEntries() {
        UpdateProductClauseRelDTO webDto = new UpdateProductClauseRelDTO();
        webDto.setClauseRels(List.of(new ClauseRelDTO("C1", "V1.0", null)));

        UpdateProductClauseRelRequest apiRequest = new UpdateProductClauseRelRequest();
        apiRequest.setClauseRels(List.of(new UpdateProductClauseRelRequest.ClauseRel("C1", "V1.0", null)));

        assertThat(mapper.toCommand(webDto, PRODUCT_ID, TENANT_ID).newClauseRels())
                .singleElement()
                .extracting(ProductClauseRel::isMainClause).isEqualTo(false);
        assertThat(mapper.toCommand(apiRequest, PRODUCT_ID, TENANT_ID).newClauseRels())
                .singleElement()
                .extracting(ProductClauseRel::isMainClause).isEqualTo(false);
    }

    @Test
    @DisplayName("🔴 空清单与未携带必须可区分：空清单保持空清单，不得被折叠成 null")
    void emptyClauseRelsMustStayEmptyAndAbsentMustStayNull() {
        UpdateProductClauseRelDTO emptyWeb = new UpdateProductClauseRelDTO();
        emptyWeb.setClauseRels(List.of());
        UpdateProductClauseRelRequest emptyApi = new UpdateProductClauseRelRequest();
        emptyApi.setClauseRels(List.of());

        // 空清单 = 「替换为空清单」这一明确输入，折叠成 null 会与「未携带」混同
        assertThat(mapper.toCommand(emptyWeb, PRODUCT_ID, TENANT_ID).newClauseRels()).isEmpty();
        assertThat(mapper.toCommand(emptyApi, PRODUCT_ID, TENANT_ID).newClauseRels()).isEmpty();

        // 未携带（null）保持 null——本命令无「继承」语义，null 由聚合/端口裁决，映射层不得擅自换义
        UpdateProductClauseRelDTO absentWeb = new UpdateProductClauseRelDTO();
        UpdateProductClauseRelRequest absentApi = new UpdateProductClauseRelRequest();
        assertThat(absentWeb.getClauseRels()).isNull();
        assertThat(absentApi.getClauseRels()).isNull();
        assertThat(mapper.toCommand(absentWeb, PRODUCT_ID, TENANT_ID).newClauseRels()).isNull();
        assertThat(mapper.toCommand(absentApi, PRODUCT_ID, TENANT_ID).newClauseRels()).isNull();
    }

    /** 两侧同形请求：主条款 + 附加条款各一（api 契约形态与 web 形态逐字段对应）。 */
    private UpdateProductClauseRelDTO webRequest() {
        UpdateProductClauseRelDTO dto = new UpdateProductClauseRelDTO();
        dto.setClauseRels(List.of(
                new ClauseRelDTO("C-MAIN", "V1.0", Boolean.TRUE),
                new ClauseRelDTO("C-RIDER", "V1.0", Boolean.FALSE)));
        return dto;
    }

    private UpdateProductClauseRelRequest apiRequest() {
        UpdateProductClauseRelRequest request = new UpdateProductClauseRelRequest();
        request.setClauseRels(List.of(
                new UpdateProductClauseRelRequest.ClauseRel("C-MAIN", "V1.0", Boolean.TRUE),
                new UpdateProductClauseRelRequest.ClauseRel("C-RIDER", "V1.0", Boolean.FALSE)));
        return request;
    }

    /** 命令内绑定清单的「对称性签名」：除 bindTime 外的全部字段按序拼接。 */
    private List<String> signatures(UpdateProductClauseRelCommand command) {
        List<String> signatures = new ArrayList<>();
        for (ProductClauseRel rel : command.newClauseRels()) {
            signatures.add(rel.relId() + "|" + rel.clauseId() + "|" + rel.clauseVersion()
                    + "|" + rel.isMainClause());
        }
        return signatures;
    }
}
