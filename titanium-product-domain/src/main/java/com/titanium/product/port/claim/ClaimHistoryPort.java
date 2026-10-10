package com.titanium.product.port.claim;

import java.util.List;

/**
 * 历史赔案取数端口（G11/g11-03 NCD 反算的驱动侧出口）
 *
 * <p>NCD（无赔款优待）由**被保车辆的历史赔案次数**反算，而赔案事实归理赔域所有，产品域只持有
 * 产品配置。反算因此必须向理赔域取数：按车辆标识与统计区间检索出险案件。</p>
 *
 * <p>六边形定位：接口是内核边界概念，与 {@code aggregate} 平级置于 {@code domain/port}，
 * 并按对端域分包（项目规约 §3.4.6 / §3.4.13）；实现 {@code ClaimHistoryAdapter} 置于
 * infrastructure 层（§3.4.5，禁止放 application）。</p>
 *
 * <p>🔴 <b>失败语义（本端口的核心契约，g11-03 AC-03「取不到不得回落 1.0」的落点）</b>——
 * 三种结局必须严格分离：</p>
 * <ul>
 *   <li><b>查询成功且区间内确无赔案</b>：返回**空列表**。这是「无赔款」的合法业务事实，是
 *       NCD 优待的**依据**，会正常参与反算（命中 0 次档位）；</li>
 *   <li><b>查询成功且有赔案</b>：返回对应条目列表；</li>
 *   <li><b>取不到</b>（理赔服务不可达 / 5xx / 响应无效）：**抛出异常**（实现侧表达为
 *       {@code NCD_HISTORY_UNAVAILABLE}）。🔴 <b>绝不可返回空列表</b>——「查不到历史」与
 *       「确无赔款」是两种事实，混同会让反算把基础设施故障当成无赔款优待，静默给出偏低系数，
 *       直接造成保费少收且事后无从察觉。</li>
 * </ul>
 *
 * <p>🔴 标的标识缺失（VIN 与 subjectId 皆空）也**不得**返回空列表：缺键属调用方错误
 * （{@code NCD_SUBJECT_IDENTIFIER_REQUIRED}），按「无赔款」处理等于凭空给优惠。</p>
 */
public interface ClaimHistoryPort {

    /**
     * 按车辆标识与统计区间检索计入 NCD 统计的历史赔案。
     *
     * @param query    取数条件（含 VIN/subjectId 与出险日期区间）
     * @param tenantId 租户ID（随请求头贯穿；亦用于失败日志定位）
     * @return 区间内命中的赔案条目；**空列表是合法返回值**，表示确无赔款
     * @throws RuntimeException 取不到历史时抛出（实现侧表达为 {@code NCD_HISTORY_UNAVAILABLE}），
     *                          不得降级为空列表、不得让调用方以默认系数继续
     */
    List<ClaimHistoryItem> findIncidents(ClaimHistoryQuery query, String tenantId);
}
