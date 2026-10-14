package com.titanium.product.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.titanium.metadata.enums.Reserved;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.request.ncd.NcdResolutionRequest;
import com.titanium.product.api.response.ncd.NcdResolutionResponse;

import jakarta.validation.Valid;

/**
 * Product NCD（无赔款优待）反算远程契约（G11/g11-03）
 * <p>
 * 命名主键取业务概念 {@code Ncd}（NCD 反算不是某个聚合根的 CRUD，而是「按车辆历史赔案反算系数」的
 * 独立能力入口），与 {@code ProductSurrenderValueApi} 同法；同域多 {@code @FeignClient} 共用
 * {@code name=titanium-product-service}，故各有唯一 {@code contextId}。
 * </p>
 * <p>
 * 🔴 失败语义（AC-03「取不到不得回落 1.0」的对外表达）：一切取不到/命中不了都以**失败信封**返回，
 * 各错误码语义互斥——{@code NCD_RULE_NOT_CONFIGURED}(60000180) 无规则集 /
 * {@code NCD_HISTORY_UNAVAILABLE}(60000181) 历史赔案取不到 /
 * {@code NCD_BAND_NOT_MATCHED}(60000182) 次数未命中档位 /
 * {@code NCD_SUBJECT_IDENTIFIER_REQUIRED}(60000183) 缺标的标识 /
 * {@code NCD_RULE_CONFLICT}(60000185) 档位重叠。调用方**不得**在收到失败信封后自行按 1.0 继续，
 * 那会让「查不到」与「确无赔款」不可区分。
 * </p>
 */
@FeignClient(name = "titanium-product-service", contextId = "productNcdApi", path = "/api/v1")
public interface ProductNcdApi {

    /**
     * 按车辆历史赔案反算 NCD 系数。
     *
     * @param request 反算请求（产品 + 车辆标识 + 车型 + 业务时点）
     * @return 反算结果（含条数/区间/规则版本等取证字段）；取不到即失败信封
     */
    @PostMapping("/ncd-resolutions")
    @Reserved("g16-04: 跨域零消费者（预留）；登记见 docs/技术文档/契约登记表.md")
    ApiResponse<NcdResolutionResponse> resolveNcd(@Valid @RequestBody NcdResolutionRequest request);
}
