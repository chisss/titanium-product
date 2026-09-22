--liquibase formatted sql
--changeset weisun:product-clause-rel-view-clause-id-fix-1

-- 订正 t_product_clause_rel_view.clause_rels_json 中「把条款**编码**写进 clause_id 字段」的 2 行。
--
-- 【现象】产品详情页「条款与保障责任」面板显示「主条款（条款信息缺失）CL-MED-ZYES-V2 · - · 0 项保障」，
-- 并弹全局红条「资源不存在」。
--
-- 【根因】clause_rels_json 里 clauseId 存的是条款**编码** `CL-MED-ZYES-V2`，而非条款主键。
-- 该值由 `ProductClauseRel(clauseId, clauseVersion, isMainClause)` 便捷构造器派生 ——
-- 其 relId = clauseId + "_" + clauseVersion，坏数据 `relId = "CL-MED-ZYES-V2_null"` 反证
-- 调用方传的正是「编码 + 版本 null」。产品域创建路径
-- （`InsuranceProduct` 构造器）对 clauseIds 只校验「非空 + 主条款在其中」，**不校验条款是否存在**，
-- 故非法值一路落到事件载荷与读模型（写入侧校验缺口另由 R7-02 处置）。
--
-- 【影响】不止于显示：保单出单链路 `PolicyProductAssembler` 用 `ref.clauseId()` 调
-- `ClauseServicePort.fetchClauseSnapshot` / `fetchCoverageSnapshots` 取条款与责任快照，
-- 命中 404 ⇒ 该产品无法正常出单。
--
-- 【订正目标】`7d7c8bfe-c10f-4096-8424-9b9a6e38f982`（条款编码同为 CL-MED-ZYES-V2，版本 V1.0，状态 ACTIVE）。
-- 判据：同一编码下有 V1.0(ACTIVE) 与 V1.1(DRAFT) 两行，而产品端的条款选择器**只提供 ACTIVE 条款供绑定**
-- （product/create 表单按 status==='ACTIVE' 过滤）⇒ 可绑定版本唯一，取 ACTIVE 行。
-- 实测：该 id `GET /proxy/clauses/{id}` 200、`/coverages` 返回 4 条（与需求文档记载的 4 项责任一致）；
-- 而编码 `GET /proxy/clauses/CL-MED-ZYES-V2` 返回 404（业务码 74007000）。
--
-- 【为何不新建版本、只改 clauseId】clauseVersion 原值即 null（调用方未传），本脚本**不臆造**业务数据，
-- 保持 null；relId 随 clauseId 同步替换后为 `<新id>_null`，仍满足「relId = clauseId + "_" + clauseVersion」
-- 的派生不变式（且 relId 全仓无生产读取点，仅一处单测断言其派生格式）。
--
-- 【已知残留 · 未在本脚本处置】事件存储 `axon_domain_event_entry` 中两个产品的
-- `ProductCreatedEvent` 载荷仍是坏值（sequence_number = 0），故**读模型整体重放会把坏值写回**。
-- 不改事件载荷是刻意取舍：事件流是不可变历史，改它比本缺陷本身危险得多；EFFECTIVE 产品也无法经
-- `UpdateProductClauseRelCommand`（仅限 DRAFT）走正规命令订正。长期防线是写入侧校验（R7-02）。
--
-- 幂等：替换后本行不再匹配 LIKE 谓词，重复执行影响 0 行。
UPDATE t_product_clause_rel_view
SET clause_rels_json = REPLACE(clause_rels_json, 'CL-MED-ZYES-V2', '7d7c8bfe-c10f-4096-8424-9b9a6e38f982')
WHERE clause_rels_json LIKE '%CL-MED-ZYES-V2%';

--rollback UPDATE t_product_clause_rel_view SET clause_rels_json = REPLACE(clause_rels_json, '7d7c8bfe-c10f-4096-8424-9b9a6e38f982', 'CL-MED-ZYES-V2') WHERE clause_rels_json LIKE '%7d7c8bfe-c10f-4096-8424-9b9a6e38f982%';
