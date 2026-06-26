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

- **命令处理器（仅 1 个）**：`CreateProductTemplateCommand`(构造)
- **事件溯源处理器（1 个）**：`on(ProductTemplateCreatedEvent)`
- 提供 `reconstruct(...)` 静态工厂供仓储重建

### 4.3 命令清单（14，record）

`CreateProductCommand`、`SubmitProductForAuditCommand`、`AuditProductCommand`、`RejectProductAuditCommand`、`ReviseProductCommand`、`InvalidateProductCommand`、`UpdateProductClauseRelCommand`、`UpdateSalesChannelCommand`、`UpdateAttachProductCommand`、`CopyProductCommand`、`CreateProductTemplateCommand`、`UpdateProductTemplateCommand`、`ActivateProductTemplateCommand`、`DeactivateProductTemplateCommand`

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

# 在项目根编译安装（首次需先装依赖模块如 titanium-metadata）
cd /Users/sunwei/titanium-project
mvn -pl titanium-product -am clean install -DskipTests

# 单独启动产品服务（端口 8082）
cd titanium-product/titanium-product-bootstrap
mvn spring-boot:run
```

依赖前置：MySQL（库 `titanium_insurance`）需就绪，Liquibase 自动执行 `liquibase/changelog/changelog-master.xml`（含 `t_product_view` 投影表迁移）。

---

## 七、已知缺陷与注意事项（基于代码实况）

1. **🔴 ProductTemplate 命令处理器缺失**：`ProductTemplateCommandAppService` 通过 `commandGateway` 派发 `UpdateProductTemplateCommand`/`ActivateProductTemplateCommand`/`DeactivateProductTemplateCommand`，但 `ProductTemplate` 聚合根**只实现了 `CreateProductTemplateCommand`**，其余三命令无 `@CommandHandler`，运行时将抛 `NoHandlerForCommandException`。对应事件（Updated/Activated/Deactivated）已有投影处理，属"半成品"。
2. **🔴 产品查询无 QueryHandler**：`ProductQueryAppService` 派发 `FindProductByConditionQuery`、`FindProductClauseByProductIdQuery`，但全模块仅 `ProductDetailQueryHandler`(FindProductById) 与模板 4 个查询有 `@QueryHandler`，这两条查询**无处理器**，运行时失败。
3. **🟠 CopyProductCommand 为死命令**：定义了命令但 `InsuranceProduct` 无对应处理器，应用层也无方法调用，悬空。
4. **🟠 UpdateAttachProductCommand 不发事件**：该命令处理器直接改聚合根字段且**未 `apply` 事件**，事件溯源重放时该变更丢失，读模型也无法感知。应补 `ProductAttachUpdatedEvent`。
5. **🟠 Kafka 跨域未接入**：`ProductKafkaEventPublisher`、`ClauseEventListener` 均为 TODO 注释桩，`axon.eventbus.type=simple` 为进程内总线，跨域事件实际未发布/未消费。
6. **🟡 数据表实体冗余**：`infrastructure/entity` 下并存 `ProductDO` 与 `ProductEntity`，职责需厘清，避免误用。
7. **🟡 处理组配置缺口**：`application.yml` 仅配 `product-group`，读侧 `product-query-group` 未显式声明，需确认投影处理器正常装配。

> 修改聚合根 / 命令 / 事件后，务必同步：事件溯源处理器、读模型投影、QueryHandler、对应单元测试（见 AGENTS.md 协作检查清单）。
