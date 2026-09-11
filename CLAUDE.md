# titanium-product 产品域 - 模块开发规约

> 版本: V1.0 ｜ 最后更新: 2026-06-23
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
│   ├── event/                        # ProductKafkaEventPublisher（Kafka 发布，TODO 桩）
│   ├── listener/                     # ClauseEventListener（消费条款域事件，TODO 桩）
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

### 7.5 🟠 Kafka 跨域：**外发已实装、入站仍是桩**（唯一存量真缺口）

- **外发 ✅**：`infrastructure/event/ProductKafkaEventPublisher` 为真实 `@Component`（`KafkaTemplate`），`product-created` / `product-audited` / `product-invalidated` 三主题已发布，载荷含 `tenantId`（m0-714）。
- **入站 🟠**：`infrastructure/listener/ClauseEventListener` 的两个 `@KafkaListener` **仍被注释**，且指向的 `clause.version-updated` / `clause.deactivated` 与条款域实际主题 `clause-status-changed` 不符——条款变更同步产品的能力**未接通**。已登记于 [跨域事件目录-2026-09.md](../docs/技术文档/跨域事件目录-2026-09.md)（`clause-status-changed` 行：product 侧为 TODO 注释桩）。
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

> 修改聚合根 / 命令 / 事件后，务必同步：事件溯源处理器、读模型投影、QueryHandler、对应单元测试（见 AGENTS.md 协作检查清单）。

