# titanium-product 产品域 - 模块开发规约

> 版本: V1.5 ｜ 最后更新: 2026-09-22（R8-01：条款绑定门禁收口——三条写入路径共用 `clauseIdsOf` + `validateClauseBindable`，条款绑定要求 **ACTIVE**；`ReviseProductCommand` 补 `tenantId` 并把 `newClauseRels` 的 null 语义定为**继承**（此前原样落库＝修订一次静默清空全部绑定）；实测量化代价为 4 个 V2A 产品，见 §7.14；上一版 V1.4 R7-02：条款绑定写入侧校验落地——`domain/port/clause/ClauseExistencePort` + `infrastructure/adapter/clause/ClauseExistenceAdapter`，并固化「新增跨域 Feign 契约三处同批登记」，见 §7.13；再上一版 m16-1904：`ActuarialBasis` 定性为「未落地契约」+ 更正臆造 javadoc，产品级 `factors` 补齐领域侧 `@Deprecated` 并登记修订路径缺口，见 §7.12；再上一版 m11-1405：`ProductTemplate.supportedCoverages/supportedExclusions` 定性登记为「未落地契约 🔶 契约/模型阶段」，见 §7.11；再上一版 m10-1304：Kafka 入站判定「不应接通」、空壳 `ClauseEventListener` 已删除，见 §7.5）
> 本文档为**产品域微服务级**规约，仅聚焦本模块。通用规范（DDD 分层、多租户、Liquibase 等）以根目录 `/Users/sunwei/titanium-project/CLAUDE.md` 为准，不再重复。

---

## 一、模块概述

产品域是保险产品的**商业化配置中心**，向上承接条款域（条款规则底座），向下支撑保单域（出单载体）与核保域（核保配置）。核心职责：

- **保险产品定义**：产品基础信息、形态（团险/个险）、险种、主险/附加险分类
- **产品模板（产品工厂）**：按险种类型沉淀标准化模板，决定出单模式、核保策略、理赔流程、保全规则
- **配置编排**：投保条件、保障期间、缴费方式、定价基础规则、销售渠道、附加险搭配、出单流程、保单形态、核保配置
- **生命周期管理**：草稿 → 提审 → 审核 → 生效 → 修订（版本化）/ 下架
- **条款绑定**：一个产品绑定多条条款，区分主条款与附加条款

在保险全生命周期中，产品域是**承上启下的配置枢纽**：产品生效后，保单域据其配置出单，核保域据其配置核保。

---

## 二、技术栈与端口

| 项目 | 值 |
|------|-----|
| JDK | Amazon Corretto 21（`/Users/sunwei/Library/Java/JavaVirtualMachines/corretto-21.0.4/Contents/Home`）|
| Spring Boot | 4.0.1 |
| Axon Framework | 4.10.0（CQRS + Event Sourcing）|
| 服务名 | `titanium-product-service` |
| 端口 | **8082** |
| 数据库 | MySQL，库名 `titanium_insurance`（schema 同名）|
| Axon EventBus | `simple`（进程内，**Kafka 未接入**）|
| 写侧处理组 | `product-group`（subscribing 模式）|
| 读侧处理组 | `product-query-group`（投影专用）|
| 序列化 | Jackson |

> 注意：`application.yml` 仅声明了 `product-group` 处理器，而读模型投影 `ProductProjectionEventHandler` 使用 `product-query-group`，需确认该组的处理器配置是否齐全。

---

## 三、子模块分层结构

```
titanium-product/
├── titanium-product-common/          # 常量 ProductConstants、异常 ProductDomainException
├── titanium-product-domain/          # 领域层（核心）
│   ├── aggregate/                    # 聚合根：InsuranceProduct、ProductTemplate
│   ├── command/                      # 命令（record，14 个）
│   ├── event/                        # 事件（record，12 个）
│   ├── query/                        # 查询（record，7 个）
│   ├── entity/                       # 聚合内实体：ProductClauseRel
│   ├── valueobject/                  # 值对象（20+，如 InsureCondition/PricingBasicRule）
│   ├── repository/                   # 仓储接口：ProductRepository、ProductTemplateRepository
│   └── service/                      # 领域服务：ProductDomainService
├── titanium-product-infrastructure/  # 基础设施层
│   ├── entity/                       # 数据表实体：ProductDO/ProductEntity/ProductTemplateDO 等
│   ├── repository/ + repository/jpa/ # 仓储实现 + JPA Repository
│   ├── projection/                   # 模板投影 ProductTemplateProjection
│   ├── mapper/                       # ProductInfraMapper（MapStruct）
│   ├── event/                        # ProductKafkaEventPublisher（Kafka 外发，已实装）
│   └── init/                         # ProductTemplateDataInitializer（模板初始化）
├── titanium-product-application/     # 应用层（命令/查询编排）
│   ├── command/                      # ProductCommandAppService、ProductTemplateCommandAppService
│   └── query/                        # ProductQueryAppService、ProductTemplateQueryAppService
├── titanium-product-query/           # 查询层（CQRS 读侧）
│   ├── handler/                      # ProductDetailQueryHandler、ProductTemplateQueryHandler、ProductProjectionEventHandler
│   ├── entity/                       # ProductView（读模型）、ProductQueryResult、ProductTemplateQueryResult
│   ├── repository/                   # ProductViewRepository
│   └── mapper/                       # ProductQueryMapper
├── titanium-product-api/             # API 层（Feign 接口 + DTO/Request/Response）
│   ├── ProductApi / ProductTemplateApi   # @FeignClient 定义
│   ├── dto/ request/ response/
├── titanium-product-web/             # Web 层
│   ├── controller/                   # ProductController（实现 ProductApi）、ProductTemplateController
│   └── mapper/                       # ProductWebMapper、ProductTemplateWebMapper
└── titanium-product-bootstrap/       # 启动模块 ProductApplication（端口 8082）
```

---

## 四、核心领域模型

### 4.1 聚合根 InsuranceProduct（充血模型）

事件溯源聚合根，封装产品全部商业配置。状态机：`DRAFT → AUDITING → EFFECTIVE → INVALID`，修订生成新版本回到 `DRAFT`。

- **命令处理器（9 个）**：`CreateProductCommand`(构造)、`SubmitProductForAuditCommand`、`AuditProductCommand`、`RejectProductAuditCommand`、`ReviseProductCommand`、`InvalidateProductCommand`、`UpdateProductClauseRelCommand`、`UpdateSalesChannelCommand`、`UpdateAttachProductCommand`
- **事件溯源处理器（8 个）**：对应上述除 `UpdateAttachProductCommand` 外的事件
- 每个命令处理器先做**状态前置校验**（如"仅草稿可提审"），再 `AggregateLifecycle.apply` 事件——业务行为内聚在聚合根内，符合充血模型

### 4.2 聚合根 ProductTemplate（产品工厂）

按险种类型提供标准化模板配置。

- **命令处理器（5 个）**：`CreateProductTemplateCommand`(构造)、`UpdateProductTemplateCommand`、`ConfigureLifeProductCommand`、`ActivateProductTemplateCommand`、`DeactivateProductTemplateCommand`
- **事件溯源处理器（5 个）**：对应上述五个事件（`ProductTemplateCreatedEvent` / `ProductTemplateUpdatedEvent` / `LifeProductConfiguredEvent` / `ProductTemplateActivatedEvent` / `ProductTemplateDeactivatedEvent`）
- 原 `reconstruct(...)` 静态工厂已于 m0-705 删除（零调用方，且字段残缺不恢复 `supportedCoverages` 等）

### 4.3 命令清单（Axon 命令 14 个 + pricing 子域应用层入参 17 个，均为 record）

**Axon 命令（14，经 `CommandGateway` 派发，处理器见 §七 7.1）**：`CreateProductCommand`、`SubmitProductForAuditCommand`、`AuditProductCommand`、`RejectProductAuditCommand`、`ReviseProductCommand`、`InvalidateProductCommand`、`UpdateProductClauseRelCommand`、`UpdateSalesChannelCommand`、`UpdateAttachProductCommand`、`CreateProductTemplateCommand`、`UpdateProductTemplateCommand`、`ConfigureLifeProductCommand`、`ActivateProductTemplateCommand`、`DeactivateProductTemplateCommand`

**pricing/精算/保全供给子域应用层入参（17，`command/pricing/**` + `command/maintenance`，JPA 同步服务入参，不经 Axon 总线）**：`CreateCalculationModelCommand`、`CreateChargeComponentCommand`、`CreateDynamicFactorCommand`、`CreateTaxPolicyCommand`、`CreateSurrenderValuePolicyCommand`、`CalculateSurrenderValueCommand`、`CreatePricingPlanDraftCommand`、`ReplacePricingTestCasesCommand`、`CreateRateTableDraftCommand`、`ReplaceRateTableRowsCommand`、`PremiumCalculationCommand`、`PremiumQuoteCommand`、`CreateMaintenancePremiumQuoteCommand`、`RecalculateRetroactivePremiumPeriodsCommand`、`CreatePremiumLifecycleAdjustmentCommand`、`CreatePremiumLifecycleReversalCommand`、`CreateProductMaintenanceOfferingCommand`

### 4.4 事件清单（12，record）

`ProductCreatedEvent`、`ProductSubmittedForAuditEvent`、`ProductAuditedEvent`、`ProductAuditRejectedEvent`、`ProductRevisedEvent`、`ProductInvalidatedEvent`、`ProductClauseRelUpdatedEvent`、`ProductSalesChannelUpdatedEvent`、`ProductTemplateCreatedEvent`、`ProductTemplateUpdatedEvent`、`ProductTemplateActivatedEvent`、`ProductTemplateDeactivatedEvent`

### 4.5 查询清单（7，record）

`FindProductByIdQuery`、`FindProductByConditionQuery`、`FindProductClauseByProductIdQuery`、`GetTemplateByIdQuery`、`GetTemplateByCodeQuery`、`GetTemplateByProductIdQuery`、`GetTemplatesByInsuranceTypeQuery`

### 4.6 CQRS 读模型

- 读模型表 `t_product_view`，由 `ProductProjectionEventHandler` 订阅产品域事件投影维护（`product-query-group`，与写侧隔离）
- `ProductDetailQueryHandler` 查询 `t_product_view`（已修复"重建写侧聚合根查询"的旧缺陷），复杂配置字段以 JSON 存取
- 模板侧 `ProductTemplateQueryHandler` 直接查 `ProductTemplateDO`（`t_product_template`）

---

## 五、编码规约（本模块实例）

继承根 `CLAUDE.md`，本模块强约束如下：

- **命令/查询/事件用 record**：如 `CreateProductCommand`，禁止用普通类
- **命令用 `@CommandHandler`、查询用 `@QueryHandler`、事件溯源用 `@EventSourcingHandler`、读模型投影用 `@EventHandler`**
- **构造器注入优先**：参考 `ProductCommandAppService`、`ProductDetailQueryHandler`（`@RequiredArgsConstructor`）。⚠️ `ProductTemplateCommandAppService`、`ProductTemplateQueryHandler`、`ProductTemplateProjection` 仍用 `@Autowired` 字段注入，待整改
- **跨层转换用 MapStruct**：`ProductInfraMapper`（Entity↔Domain）、`ProductWebMapper`（Request↔Command/VO）、`ProductQueryMapper`，禁止手写实体互转
- **充血模型**：业务规则写进 `InsuranceProduct` 命令处理器，Application 层只做编排（`commandGateway.sendAndWait`），不写业务逻辑
- **中文注释 + 英文标识符**；对外 DTO/Request/Response 用 `@Schema`，内部类用 `/** */`
- **SLF4J `{}` 占位符**：参考 `ProductProjectionEventHandler` 日志写法，禁止字符串拼接
- **枚举集中**：险种 `InsuranceType`、产品状态/形态/类别 `ProductEnum` 等来自 `titanium-metadata`，本模块独有常量放 `ProductConstants`
- **租户贯穿**：所有命令/查询/表均带 `tenantId`，Feign 接口统一 `X-Tenant-ID` 头

---

## 六、构建与运行

```bash
# 设置 JDK 21
export JAVA_HOME=/Users/sunwei/Library/Java/JavaVirtualMachines/corretto-21.0.4/Contents/Home

# 构建产品域（域目录即 Maven reactor；跨域依赖须已在本地仓库，首次请先跑根规约 §15.2 全量构建）
cd /Users/sunwei/titanium-project/titanium-product
mvn clean install -DskipTests

# 单独启动产品服务（端口 8082）
cd titanium-product-bootstrap
mvn spring-boot:run
```

依赖前置：MySQL（库 `titanium_insurance`）需就绪，Liquibase 自动执行 `liquibase/changelog/changelog-master.xml`（含 `t_product_view` 投影表迁移）。

---

## 七、已知缺陷与注意事项（基于代码实况）

> 🔴 **2026-09-11 实读复核**：原第 1–4、6、7 条系**静态扫描误判/已过时**，逐条更正如下（教训：本清单须实时复核，勿沿用旧判断；判「命令无处理器」前**必须先确认它是否经 `CommandGateway` 派发**）。

### 7.1 ✅ 原「聚合命令处理器大面积缺口（17 个）」不成立（m0-715 复核）

全模块**只有 2 个类派发 Axon 命令**：`application/command/ProductCommandAppService`、`application/command/ProductTemplateCommandAppService`（另有 `application/init/ProductTemplateDataInitializer`），合计覆盖 14 个命令（`CreateProduct` / `SubmitProductForAudit` / `AuditProduct` / `RejectProductAudit` / `ReviseProduct` / `InvalidateProduct` / `UpdateProductClauseRel` / `UpdateSalesChannel` / `UpdateAttachProduct` / `CreateProductTemplate` / `UpdateProductTemplate` / `ConfigureLifeProduct` / `ActivateProductTemplate` / `DeactivateProductTemplate`）——**处理器全部齐备**（`InsuranceProduct`、`ProductTemplate`）。

原被点名的 17 个 `XxxCommand` 属 **pricing / 精算 / 费率 / 保全供给子域**，该子域是**普通 JPA 同步应用服务**形态（非 Axon 事件溯源），命令仅作**应用层入参模型**：

```
web Controller|Provider → application/command|query/pricing/*XxxService
                        → application/orchestration/pricing/*ApplicationService
                        → domain 对象（RateTableDefinition / PricingPlanDefinition / …）
                        → infrastructure/pricing/repository 的 save(...)
```

全链路**从不触碰 `CommandGateway`**，故不存在 `NoHandlerForCommandException`；领域实现（`pricing/aggregate` 下 9 个定义类 + `PricingPlanCalculator`/`PricingEvidenceHasher`/各 `validateForPublish`）**均已落地**，与 [Titanium全景建设规划-2026-09.md](../docs/Titanium全景建设规划-2026-09.md) §产品线「精算工作台四条链路可用」的记载一致。
> 判据：`command/` 包内的 record 若只被 `orchestration` 消费、且下游无 `apply` 事件，就是应用层入参，**不要按 Axon 命令去找处理器**。

### 7.2 ✅ 产品查询处理器齐备

`FindProductByConditionQuery`、`FindProductClauseByProductIdQuery` **已有 `@QueryHandler`**；`PremiumCalculationQuery`、`PremiumQuoteQuery` 与 7.1 同理，是 pricing 同步读服务的入参（`PremiumCalculationQueryAppService` / `PremiumPricingQueryAppService` 直查 JPA），非 Axon 查询。

### 7.3 ✅ `CopyProductCommand` 已不存在（文件已删除，悬空项自然消解）

### 7.4 ✅ `UpdateAttachProductCommand` 已发事件

`InsuranceProduct:354-360` 经草稿/生效状态前置校验后 `apply(new ProductAttachUpdatedEvent(productId, command.attachProductIds()))`，事件溯源重放与读模型投影均正常。

### 7.5 ✅ Kafka 跨域：外发已实装、入站判定**不应接通**（2026-09-14 m10-1304 定论，唯一存量疑点已闭合）

- **外发 ✅**：`infrastructure/event/ProductKafkaEventPublisher` 为真实 `@Component`（`KafkaTemplate`），`titanium.product.created` / `audited` / `invalidated` 三主题已发布，载荷含 `tenantId`（m0-714）。
- 🔴 **三主题仓内零消费者，已判定「不应接线」（2026-09-14 m6-907）**：全仓 `@KafkaListener` 清单中无一条订阅 product 主题。判定理由——① 各域对产品配置的取用一律是**业务时点的同步 Feign 实时查询**（policy `ProductServicePort`、maintenance `ProductMaintenancePremiumQuoteAdapter` 等），不是长期状态订阅；② 产品在售状态已由出单必经的 CONFIRM 试算**同步强校验**（`PremiumQuoteApplicationService.validateProductAvailability` 对非 `EFFECTIVE` 抛 `PRICING_PLAN_NOT_EFFECTIVE` 阻断出单），异步订阅属降级；③ 已出保单为产品快照，下架**不回溯**。三主题定位为**对外集成出口**（三方系统 / 数据平台）。原 `ProductKafkaEventPublisher` 类注释记「Policy/Underwriting/Clause 消费」为**不实记载**，已修正。判据见 [跨域事件目录 §六.15](../docs/技术文档/跨域事件目录-2026-09.md)。
- ✅ **入站「条款变更同步产品」判定为不应接通，空壳已删除（m10-1304，2026-09-14）**：`infrastructure/listener/ClauseEventListener`（零方法空壳——`@Component` + 两个被注释的 `@KafkaListener` + TODO；且注释里的 `titanium.clause.version-updated` / `titanium.clause.deactivated` 两个主题名**在 `CrossDomainTopics` 中根本不存在**，属孤儿命名残留）已按 m0-705 判据删除，`infrastructure/listener` 目录随之清空。
    - 🔴 **判据一（设计语义相反，决定性证据）**：`ProductClauseRel.clauseVersion` 的 javadoc 原文——「条款版本（**绑定具体版本的条款，避免条款修订影响产品**）」。本域**刻意**以版本绑定隔离条款变更对产品的影响；「监听条款状态变更去改产品绑定」与该设计**方向相反**。
    - **判据二（无消费基础设施）**：本域零 `@EnableKafka` / `ConsumerFactory` / `kafkaListenerContainerFactory`，`application.yml` 只有 `producer` 段——接通须新建整条消费链路，不是「补个监听器」。
    - **判据三（无反查能力）**：`ProductClauseRelViewRepository` 无按 `clauseId` 反查产品的方法（现有查询 `FindProductClauseByProductIdQuery` 方向相反）。
    - **判据四（同类先例对称）**：product 三个出站主题已由 m6-907 判定「仓内零消费者且不应接线」，定位为对外集成出口。
    - **附带清理**：`titanium-product-domain/pom.xml` 的 `titanium-clause-api` 依赖**全仓零引用**（`grep com.titanium.clause` 在 product 全域含 test 零命中），一并删除。
    - ⚠️ **该依赖已于 2026-09-21（R7-02）重新引入**，理由是**出站 Feign 调用**（条款存在性校验），与上述「入站 Kafka 不应接通」是两个方向、不冲突：删掉的是入站监听，重加的是出站查询。详见 §7.13。
    - 判据全文见 [跨域事件目录 §六.17](../docs/技术文档/跨域事件目录-2026-09.md)。
- `axon.eventbus.type=simple` 为**进程内事件总线**（Axon 自身分发），与跨域 Kafka 外发是两条独立通道，不影响上述结论。

### 7.6 ✅ 包结构已重组，无 DO/Entity 冗余

`infrastructure/entity` 目录**已不存在**；写侧持久化实体按子域归位为 `infrastructure/pricing/entity/{calculation,pricing,premium}` 与 `infrastructure/maintenance/entity`，命名统一 `XxxDO`（无 `XxxEntity` 残留）。产品/模板聚合为**纯事件溯源**，无 JPA 写实体。

### 7.7 ✅ 处理组配置齐备

`application.yml:45-53` 已声明 `product-group`（写侧）、`product-kafka-group`（跨域 Kafka 外发）、`product-query-group`（读侧投影，含 DLQ）。

### 7.8 ✅ 退保价值只读查询端点（m0-717，2026-09-11）

`GET /api/v1/surrender-value-policies/cash-value`（`ProductSurrenderValueApi.queryCashValue`）——**纯查表、零副作用**的现金价值只读查询，供计费域垫缴判定（日级高频扫描）调用。

- **与退保试算的分工（🔴 不得混用）**：
  | | 只读查询（本端点） | 退保试算 `POST /premium-calculations/surrender-values` |
  |---|---|---|
  | 副作用 | 无（不落库） | 有：创建 `PremiumCalculation` + `PremiumLifecycleAdjustment` 生命周期贷项 |
  | 前置 | 仅需产品要素 | 需 `originalCalculationId`（出单确认记录） |
  | 用途 | 高频只读判定（垫缴、试算展示） | 正式退保出账 |
- **公式唯一**：两端点共用 `SurrenderValuePolicy.calculate`，不得另立口径造成现金价值与退保试算漂移。
- **入参**：`productId` / `policyYear` / `customerPayable`（保单总保费口径）/ `policyEffectiveDate` / `valuationDate` / 可选 `businessTime`；**保单年度由保单域推导后传入**，本域不自算。
- **出参**：`CashValueQueryResponse`（含 `cashValueRate` / `cashValue` / `withinCoolingOff` / `refundType` 与策略取证字段 `policyCode`/`policyVersion`/`policyContentHash`）。
- **无适用策略**：返回业务码 `SURRENDER_VALUE_POLICY_NOT_FOUND`（60000170，60 段）。
- 实现：`CashValueQueryAppService`（application/query/pricing/surrender）→ `SurrenderValuePolicyRepository.findPublished` → 聚合 `calculate`。

### 7.9 🟡 存量待整改（与命令无关，保留）

- `ProductTemplateCommandAppService`、`ProductTemplateQueryHandler`、`ProductTemplateProjection` 仍用 `@Autowired` 字段注入，应改构造器注入。
- 产品/模板读模型与模板侧查询走 `t_product_view` / `t_product_template`，模板查询**绕过读模型直查 DO**（见 §4.6），后续如需统一 CQRS 读侧应一并收敛。

### 7.10 ✅ 规则引擎计算的业务上下文透传（m8-1104，2026-09-14）

`RuleEngineComputationAdapter` 调 `RuleArtifactApi.compute` 时补两个可选请求头：`businessId = request.executionId()`（**本次定价计算的执行标识**，形如 `定价方案:用例`，与 `PricingFeatureResolutionRequest.requestId` 同源），`businessType = BusinessDomainType.PRODUCT.getCode()`（常量，端口固有属性）。

- **修复前**：规则引擎侧 `t_rule_execution_log` 的 `business_id`/`business_type` 恒为空，**无法从一条规则执行反查它属于哪个定价方案 / 哪个用例**；**修复后**可经 `findByBusinessIdAndBusinessTypeAndTenantId` 反查。
- 🔴 契约走可选请求头（`X-Business-Id`/`X-Business-Type`），**不污染 `RuleArtifactComputeRequest` 请求体**——业务上下文与规则变量是两个语义空间。
- 🔴 `executionId` 在计算链路中另有「响应标识一致性校验」职责（`validateIdentity` 比对请求/响应的 `executionId`），两处取值同源、语义一致，**不可混填其它标识**。
- 回归：`RuleEngineComputationAdapterTest` 4 处桩 + 1 处 verify 已同步为六参并断言请求头透传。

### 7.11 🟡 `ProductTemplate.supportedCoverages/supportedExclusions` 属**未落地契约**（m11-1405 定性登记）

> 🔴 **定性**：既**不是缺陷**、也**不是"待补校验"**，而是「字段 / 命令 / 事件已定义，写入入口与取值域均未落地」的**半成品契约**——按《Titanium全景建设规划-2026-09.md》§12 判据属 **🔶「契约/模型阶段」**（模型存在但无生产调用链，不得计入完成度）。登记于此，防后续把它当作"已建成的能力"施工。

- **引用面（实读，2026-09-14）**：全域 Java 仅 **3 个文件**——`domain/aggregate/ProductTemplate.java:67,68,102,103,120,121`（字段 + 命令装配 + 事件回放）、`command/CreateProductTemplateCommand.java:24,25`、`event/ProductTemplateCreatedEvent.java:24,25,34,35,38`。
- 🔴 **无写入入口**：web 层 `CreateProductTemplateDTO` **无该字段**；`ProductTemplateWebMapper.java:102-103` 与 `infrastructure/init/ProductTemplateDataInitializer.java:107-108` 一律**硬编码 `List.of()`**——所有创建路径都只能写入空列表。`UpdateProductTemplateCommand` 亦无该字段，故**建成后也无法修改**（一次性死值）。
- 🔴 **无读模型列**：全仓 Liquibase / DDL / XML **零 `supported_coverages` 命中**，读侧不承载该数据。
- 🔴 **无消费方**：无任何校验、计算或查询读取该字段。
- **若要落地，前置有三（缺一不可）**：① 补 DTO/Mapper 写入入口**并覆盖更新路径**（否则仍是一次性死值）；② **先定义取值域**——"模板支持的保障责任"指条款责任编码、`CoverageSnapshot` 编码，还是别的标识？并划清与 `PolicyStructureConfig.LiabilityStructure` 的语义边界（二者当前极易混淆）；③ 明确消费场景与失败语义。
- 🔴 **勿直接按"纳入产品创建校验"施工**：《Titanium全景建设规划-2026-09.md》§6 待办原第 141 项的前提（条款责任 ⊆ 模板支持范围）**当前不成立**——连写入入口与取值域定义都没有，无从校验；该项已在该文档改写为「前提不成立」。

### 7.12 🟡 `ActuarialBasis` 属**未落地契约**（🔴 对外承诺已下架）+ 产品级 `factors` 双层 `@Deprecated`（m16-1904 处置，2026-09-27 g03-05 下架）

> 与 §7.11 同属 **🔶「契约/模型阶段」**判定：模型与传输链路齐备、消费端未建。登记以防后续把它当作"已建成的能力"施工。
> 🟠 2026-09-27 g03-05 处置（R-4 裁示「下架承诺」）：**对外可配面已移除**——admin-web 修订表单不再携带该字段（后端「不传即继承」保留存量值）、列表页字段级权限提示与版本对比三标签（预定利率/费用附加率/生命表引用）同步移除，前端三要素键 grep 归零（AC-06 判据）；后端字段**保留为内部备案**（事件载荷固化不可删），待接入前提三者齐备后随精算计算能力一并恢复。

#### (1) `ActuarialBasis` —— 未落地契约，**保留**，虚构 javadoc 已更正

- **判据（m10 死 Port 两类判据）**：① **有需求来源**——《各域开发需求/product-产品域开发需求.md:90》明列该值对象三要素（预定利率 / 预定死亡率表引用 / 附加费用率）；② **非零引用**——贯通写入/持久化/回放/后台回显：`InsuranceProduct.java:144,295-303,450` → `ProductCreatedEvent:37` / `ProductRevisedEvent:33` → `ProductQueryResult:79` / `ProductViewMapper:53,82` → admin 镜像 `ProductMirror:172-174`；③ **🔴 不可删**——字段已固化在 `ProductCreatedEvent`/`ProductRevisedEvent` 载荷中，**非尾部删除会令存量事件回放错位**（删除前必须评估反序列化兼容性）。⇒ 结论：**未落地契约，非死壳**（对照 §7.5 删除的 `ClauseEventListener` 空壳：那是零引用且设计方向相反，判据相反）。
- 🟠 **仍无任何计算消费点**：`ACTUARIAL_FORMULA` 模式的保费由**外部规则工件**算出，后台配置的预定利率等参数**不影响任何保费结果** ⇒ 对运营是「配了没用」的误导，**不得对外承诺精算口径**。唯一业务方法 `hasValidInterestRate()` **零调用**。
- ✅ **已修（m16-1904）**：原 javadoc 称「提供 `LifePremiumCalculationDomainService` 计算均衡净保费…billing 经 `ProductRatePort` 取到本对象传给精算领域服务」——**这两个类型全仓均不存在**（除该 javadoc 自身外零命中），属**臆造的链路承诺**，已删除；改写为显式的「未落地契约 + **接入前提三条**」：① 精算计算领域服务落地（均衡净保费 APV / 年金因子）；② 计费域经端口取到本对象并作为计算入参（跨域取数契约，当前不存在）；③ 参数值域校验接入命令校验链（如 `hasValidInterestRate()`）。
- 守护测试：`ActuarialBasisTest`（3 例——三参工厂 `of()` 留空评估利率为 `null`（回落由消费方完成，本值对象不做隐式回落，否则无法区分「未配置」与「等于定价利率」）+ 利率值域开区间 (0,1) 边界）。

#### (2) 产品级 `factors` —— 两级标注对齐 + 新发现修订路径缺口

- ✅ **双层 `@Deprecated` 已成对**：web 侧 `PricingBasicRuleDTO.factors`（`PricingBasicRuleDTO.java:41-42`）+ 领域侧值对象 `PricingBasicRule.factors`（record 头，**m16-1904 补标**）。保留组件而非删除，是让误用者在**两个入口**都收到可执行的拒绝提示（`PRICING_FACTORS_DEPRECATED`），而不是被静默忽略；删除须评估事件载荷兼容性（同上）。
- **守护测试**：`PricingBasicRuleTest`（2 例——`factors` 组件须持续标注 `@Deprecated` 且传播到字段与访问器；**废弃面仅限 factors**，其余组件被误标即失败）+ web 侧 `ProductNestedInputDTOContractTest#factorsFieldMustStayMarkedDeprecated`。
- 🟠 **新发现残留（m16-1904，只登记未改行为）**：G7 守卫**只挂在创建路径**——`validateCreateCommand`（`InsuranceProduct.java:480-517`，由创建构造器入口 `:172` 调用），而 `ReviseProductCommand` 处理器（`:284-310`）**未调用**该校验，`ReviseProductDTO.newPricingBasicRule`（`:53`）可携带同型 `PricingBasicRuleDTO` ⇒ **经「修订」仍可写入 factors 且不被拒**，G7「防止运营误以为生效」的意图在修订路径落空。⚠️ 补校验会使既有修订调用开始失败（**行为变更**），须先确认调用方再施工。

### 7.13 🔴 新增跨域 Feign 契约必须三处同批登记（R7-02 立规，2026-09-21）

本条由 R7-02「条款绑定写入侧校验」落地时踩出的坑固化：本域引入 `titanium-clause-api` 后，**单测全绿、却有两处运行时装配失效**——编译器与单测都看不见，只有 `mvn test` 起真实上下文、或真机启动才暴露。

| # | 登记点 | 漏登记的后果 | 本次实测症状 |
|---|--------|-------------|-------------|
| 1 | `ProductApplication#@EnableFeignClients(basePackages=...)` 增补契约包 | Feign Bean 根本不被创建 | `No qualifying bean of type 'com.titanium.clause.api.ClauseApi'`（bootstrap 上下文测试 RED） |
| 2 | `ProductDeadLetterQueueContextTest#@MockitoBean(types=...)` 增补契约接口 | **惰性解析**失败拖垮整个上下文 | `No Feign Client for loadBalancing defined`（本地无 `spring-cloud-starter-loadbalancer`） |
| 3 | `docker/docker-compose.yml` 共享锚点 `SPRING_APPLICATION_JSON` 增补 `<contextId>.url` | 真机调用打到不存在的服务名 | 容器内 Feign 直连 `titanium-clause-service` 解析失败 |

- 🔴 **判据**：前两处由 `mvn test` 兜住（**必须跑全量，跑单模块会漏**——`-pl <单模块>` 会用本地仓库的旧 SNAPSHOT 依赖 jar，编译期就可能假阴性），第三处**只有真机复核能发现**。
- 🔴 **`-pl 单模块` 的陷阱（本次实测）**：改完 domain 后只跑 `mvn -pl <application 模块> test` 会从 `~/.m2` 取**旧 domain jar**，报「程序包不存在」；`tar` 还原源码不更新 mtime 还会让增量编译**跳过重编**（源码 mtime 早于 `.class` 即视为最新），旧字节码继续参与测试。
- 定位口径：条款域 **出站查询**（本文档 §7.5 的入站 Kafka 判定**不受影响**，二者方向相反）；契约 `ClauseApi` 已有 `contextId=clauseApi`，compose 锚点按 `contextId` 键生效。

### 7.14 🔴 条款绑定门禁：三条写入路径共用一个实现 + 修订路径的 null 语义（R8-01，2026-09-22）

> R7-02 只把门禁接在**创建**路径，而 UI 上唯一可达的改绑定入口是**修订**（`POST /web/v1/products/{id}/revise`）——门禁当时守的是一条休眠路径。本条固化收口结论与判据。

**(1) 条款绑定要求条款域状态为 `ACTIVE`**（用户 2026-09-22 裁定「最严」，与 `validateTemplateActive` 完全对称）

- 判定由 `ClauseExistencePort.checkBindable` 一次往返同时回答两件事：**不存在** 与 **当前未生效**。两类文案**必须可区分**——合并会让运营照「不存在」的提示去新建同编码条款，撞唯一约束。
- 🔴 **基础设施故障不得被表述成「不存在」**：仅 404 判为不存在，其余失败（Feign 5xx / 超时）原样上抛。
- 三条写入路径**共用同一实现**（`ProductCommandAppService.clauseIdsOf` + `validateClauseBindable`），不得再在任一路径内联抄一份（§3.4.11「同一个判断在两处各写一份 ⇒ 必漂移」）：

| 路径 | 入口 | 真机可达 | 门禁接入 |
|------|------|---------|---------|
| 创建 | `POST /web/v1/products` | ✅ | R7-02 |
| 整体替换 | `ProductCommandAppService.updateProductClauseRel` | 🔴 **无任何生产调用方**（无端点、无 Provider、前端零引用），休眠路径 | R8-01 |
| 修订 | `POST /web/v1/products/{id}/revise` | ✅ **UI 唯一可达的改绑定入口** | R8-01 |

**(2) 🔴 `ReviseProductCommand.newClauseRels` 为 null 表示「继承原绑定」，不是「清空」**

- 绑定非空是硬不变量，清空从来不是合法意图。此前该字段 `null` 被**原样落库** ⇒ 修订一次即**静默清空新版本的全部条款绑定**；现与 `newSalesChannels` / `newRateTableRef` / `newActuarialBasis` 三块同规则：命令未携带时继承 `this.clauseRels`。
- 🔴 **继承路径不重跑 ACTIVE 校验**（`clauseIdsOf(null)` 返回空列表 ⇒ 端口对空入参短路）。理由：存量已有产品绑 DRAFT 条款，重校验会让它们**永久无法修订**。这也意味着门禁只约束**新写入的绑定**，不追溯存量。

**(3) 🔴 `ReviseProductCommand` 补 `tenantId`（末位，共 20 分量）**

跨域校验发生在**命令到达聚合之前**的应用层，那时聚合内继承的 `this.tenantId` 还取不到。此前该字段缺失使修订路径完全绕开条款门禁——它恰是 UI 上唯一可达的改绑定入口。web 侧由 `ProductWebMapper.toCommand(request, productId, tenantId)` 透传。

**(4) 🔴 校验方法必须参数化命令名**：`validateClauseRels(commandName, newRels)` 的 `commandName` 由调用点传入（`ReviseProductCommand.class.getSimpleName()` / `UpdateProductClauseRelCommand.class.getSimpleName()`）。硬编码会让修订路径报错时误报成替换路径的命令名，运营按错误命令去查日志必然找不到。

**(5) ⚠️ 已实测量化的代价（用户知情接受）**：租户 1 有 **4 个 EFFECTIVE 产品绑 DRAFT 条款**，经 UI 修订**必被拒**「未生效」：

`V2A-GROUP-001`（惠企团体医疗险）/ `V2A-AUTO-001`（安行机动车保险）/ `V2A-LIFE-001`（金享定期寿险）/ `V2A-SHORT-001`（畅行短期意外险）

- **成因是 UI 的必然行为**，非偶发：修订页 `onMounted` 用 `getProductClauses(id)` 回填 `clauseRels`（`src/views/product/revise/index.vue:536`），`buildPayload` **无条件**把它们带成 `newClauseRels`（同文件 `:502`）——前端**发不出 null**，所以「继承」这条后路对 UI 不可用。
- 缓解途径（择一，均未施工）：① 在条款域把这 4 条条款审核激活；② 让门禁只校验**发生变化的**绑定（代价：存量未校验数据被永久豁免）。
- ✅ 影响面**不含种子产品**：`STD-PRD-*` 九个标准产品全绑 ACTIVE 条款，零受影响；受影响的 4 个均为 V2A 联调测试产物。

**(6) 真机复核结论（2026-09-22，镜像 `product-acceptance-20260922-v1`）**：创建路径 A1 不存在→400 `60000006`「不存在」/ A2 DRAFT→400「未生效」/ A3′ ACTIVE→**200 落库**；修订路径 B1 不存在→400（**错误文案正确点名 `ReviseProductCommand`**，即 (4) 的实测证据）/ B2 DRAFT→400「未生效」/ B3 ACTIVE→200 新版本落地；存量绑 DRAFT 产品 C1 带块→400「未生效」（= 代价实测成立）/ C2 不带块→**200 且新版本继承到原 DRAFT 绑定**（旧代码此处会清空）。**5 个负向用例零写入**（产品表 31→34、条款关联表 31→34，恰为 3 个正向用例各 1 条绑定）。

### 7.15 🔶 NCD 反算规则契约（G11/g11-03）：结构已交付、规则值与接入面双缺（R-3 阻断）

> 定性：**🔶「契约/模型阶段」**（参 §7.11/§7.12 先例）——模型、端口、编排、持久化结构齐备，**规则值与消费接入未落地**，且未落地是 **R-3 闸门**（NCD 规则口径与版本待业务确认）的**刻意状态**，不是缺陷。

- **已交付**（2026-10-10，`G11/g11-03`）：聚合 `NcdRuleSet`（`DRAFT→APPROVED→PUBLISHED→RETIRED`；`approve` 固化 `contentHash`，`publish` 后方可被反算取用）+ 档位 `NcdRule` → 出站端口 `domain/port/claim/ClaimHistoryPort`（+`ClaimHistoryQuery`/`ClaimHistoryItem`）+ `infrastructure/adapter/claim/ClaimHistoryAdapter`（**按消费方定义 Port** 的六边形落法，偏离 G11 文档所写「Port 落 claim 域」，详见该文档实施记录）→ 编排 `NcdResolutionOrchestrator`（`application/orchestration/pricing/ncd`）→ 契约 `ProductNcdApi`（`contextId=productNcdApi`）+ `NcdResolutionRequest/Response` → 两表零种子 DDL（`product_ncd_rule_set_202610101520_weisun_ddl.sql`）。
- 🔴 **fail-closed 六码**（`ProductErrorCode` 60 段）：`NCD_RULE_NOT_CONFIGURED`(60000180)/`NCD_HISTORY_UNAVAILABLE`(60000181)/`NCD_BAND_NOT_MATCHED`(60000182)/`NCD_SUBJECT_IDENTIFIER_REQUIRED`(60000183)/`NCD_RULE_INVALID`(60000184)/`NCD_RULE_CONFLICT`(60000185)。全链**绝不回落默认系数 1.0**：「查不到历史」（拒）与「确无赔款」（空列表 + 正常命中 0 次档位）严格分离——回落 1.0 会让二者不可区分，静默少收保费且事后无从察觉。
- 🔴 **三处 Feign 登记点已同批完成**（§7.13 规约）：① `ProductApplication#@EnableFeignClients` 加 `com.titanium.claim.api`；② `ProductDeadLetterQueueContextTest` mock 清单加 `ProductNcdApi`/`ClaimApi`；③ compose 锚点 `claimApi.url` **既已存在、未改动**。后续新增本契约相关 Feign 仍须按 §7.13 三处核查（前两处跑**全量** `mvn test` 兜、第三处只有真机暴露）。
- 🔴 **区间口径双轨，不可互相套用**：时间区间 = 日粒度**半开** `[from, to)`（`NcdLookbackWindow`：`from` = 业务日 − `lookbackMonths` 当日 0 时，`to` = 业务日次日 0 时 ⇒ 当天赔案计入）；次数档位 = **闭区间** `[minCount, maxCount]`（`NcdRule.covers`；离散计数用半开会让恰好等于上界的次数落空）。
- 🔴 **规则值导入与出单/试算接入受 R-3 阻断**，已登记索引文档 §五 第 10 条 `@Reserved` 清单（条目 2/3）；费率表既有 `ncd` 维度键（`RateTableRowDO.ncd`/`RateTableCriteria.ncd`/`RateTableRow.ncd`，null/ALL=通配）为受阻断的**试算侧**落点。**R-3 前不得向两表落种子**——空表 ⇒ 任何接入立即 `NCD_RULE_NOT_CONFIGURED` fail-closed，是正确形态。

---

> 修改聚合根 / 命令 / 事件后，务必同步：事件溯源处理器、读模型投影、QueryHandler、对应单元测试（见 AGENTS.md 协作检查清单）。

