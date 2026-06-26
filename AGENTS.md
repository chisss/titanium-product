# titanium-product 产品域 - 多 Agent 协作指南

> 版本: V1.0 ｜ 最后更新: 2026-06-23
> 配合根目录 `AGENTS.md` 使用；本文聚焦产品域内部协作与上下游边界。

---

## 一、模块定位与边界

产品域是**配置枢纽**，处于条款域与保单/核保域之间。

```
        ┌────────────┐   条款规则底座
        │  Clause域   │ ─────────────►  Product域 (本模块, 8082)
        └────────────┘   (ClauseEventListener 消费)
                                          │ 产品生效后输出配置
                          ┌───────────────┼────────────────┐
                          ▼               ▼                ▼
                    ┌──────────┐    ┌──────────┐    ┌──────────────┐
                    │ Policy域  │    │核保域      │    │ 其他消费方     │
                    │(出单配置) │    │(核保配置)  │    │              │
                    └──────────┘    └──────────┘    └──────────────┘
```

- **被调用**：Policy 域、Underwriting 域通过 Feign 拉取产品配置（出单/核保/保单形态）
- **主动消费**：监听 Clause 域事件（条款版本更新、条款停用）
- **发布事件**：产品创建/审核/下架事件供下游消费

> 现状：跨域 Feign 接口已定义，但 Kafka 事件发布（`ProductKafkaEventPublisher`）与消费（`ClauseEventListener`）均为 TODO 桩，尚未真正打通。

---

## 二、与其他域的交互点（真实代码）

### 2.1 对外提供的 Feign 接口（被下游调用）

| 接口类 | 路径前缀 | 关键方法 | 消费方 |
|--------|---------|---------|--------|
| `ProductApi` | `/api/products` | `getIssuanceConfig` | Policy 域（出单流程配置）|
| `ProductApi` | `/api/products` | `getPolicyFormConfig` | Policy 域（保单形态配置）|
| `ProductApi` | `/api/products` | `getUnderwritingConfig` | Underwriting 域（核保配置）|
| `ProductApi` | `/api/products` | `getProductById` / `createProduct` / 审核类 | 管理端/其他域 |
| `ProductTemplateApi` | `/api/product-templates` | `getByProductId` / `getByCode` / `getById` | 需模板配置的域 |

> 两个 Feign client 的 `name` 均为 `titanium-product-service`，由 `ProductController`/`ProductTemplateController` 本地实现（自调用模式）。`@EnableFeignClients(basePackages="com.titanium.product.api")`。

### 2.2 发布的领域事件（Kafka topic 已规划）

| 事件 | 规划 topic | 消费方 |
|------|-----------|--------|
| `ProductCreatedEvent` | `titanium.product.created` | Policy 域、Clause 域 |
| `ProductAuditedEvent` | `titanium.product.audited` | Policy 域、Underwriting 域 |
| `ProductInvalidatedEvent` | `titanium.product.invalidated` | Policy 域 |

### 2.3 消费的外域事件（规划）

| 来源 | 事件 | 规划 topic | 本域处理 |
|------|------|-----------|---------|
| Clause 域 | `ClauseVersionUpdatedEvent` | `titanium.clause.version-updated` | 更新产品绑定条款版本 |
| Clause 域 | `ClauseDeactivatedEvent` | `titanium.clause.deactivated` | 预警受影响产品 |

---

## 三、文件锁定建议（高频冲突区）

并发改动时，以下文件**单写者**锁定，一次只允许一个 Agent 编辑：

| 锁级 | 文件 | 说明 |
|------|------|------|
| 🔴 强锁 | `titanium-product-domain/.../aggregate/InsuranceProduct.java` | 主聚合根，命令/事件溯源处理器集中，改动牵一发动全身 |
| 🔴 强锁 | `titanium-product-domain/.../aggregate/ProductTemplate.java` | 模板聚合根 |
| 🟠 中锁 | `titanium-product-domain/.../command/`（整包） | 命令 record，新增/改签名影响聚合根与应用层 |
| 🟠 中锁 | `titanium-product-domain/.../event/`（整包） | 事件 record，影响聚合根、投影、QueryHandler |
| 🟠 中锁 | `titanium-product-query/.../handler/ProductProjectionEventHandler.java` | 读模型投影，事件变更必同步 |
| 🟡 弱锁 | `titanium-product-infrastructure/.../projection/ProductTemplateProjection.java` | 模板投影 |
| 🟡 弱锁 | `titanium-product-application/.../command/*AppService.java` | 命令编排，命令签名变更需同步 |

> 命令/事件 record 字段变更属"跨文件连锁修改"，应由**单个 Agent 串行完成**整条链路，禁止多 Agent 并行拆改同一命令链。

---

## 四、Agent 任务分工建议

| 角色 | 职责范围 | 主要文件 |
|------|---------|---------|
| **Lead** | 协调命令/事件链路变更，把关聚合根设计 | aggregate/、command/、event/ |
| **Worker-Write** | 写侧：聚合根命令处理器、事件溯源、应用层命令编排 | InsuranceProduct/ProductTemplate、command AppService |
| **Worker-Read** | 读侧：QueryHandler、读模型投影、ProductView | query/handler/、query/entity/ |
| **Worker-Infra** | 仓储、JPA、Entity、MapStruct、Kafka 桩 | infrastructure/ 整层 |
| **Worker-Web** | Controller、Feign 接口、Request/Response/DTO | web/、api/ |
| **Scout** | 探查依赖（metadata 枚举、跨域 Feign 契约），不写码 | 只读 |

并行安全切分：**写侧 / 读侧 / Web-API** 三条线可并行，但任何触及 command 或 event record 的改动须先经 Lead 串行处理。

---

## 五、协作检查清单

### 5.1 改动聚合根 InsuranceProduct / ProductTemplate 时，必须同步

- [ ] 新增/修改命令 → 在聚合根加 `@CommandHandler`，应用层 `*CommandAppService` 加编排方法
- [ ] 命令产生状态变更 → `AggregateLifecycle.apply` 对应**事件**，并加 `@EventSourcingHandler`（勿像 `UpdateAttachProductCommand` 那样漏发事件）
- [ ] 新增/修改事件 → 同步 `ProductProjectionEventHandler`（产品读模型）或 `ProductTemplateProjection`（模板）
- [ ] 影响查询字段 → 更新 `ProductView` / `t_product_view` 及 `ProductDetailQueryHandler`，必要时加 Liquibase 迁移
- [ ] 跨域影响 → 评估 `ProductKafkaEventPublisher` topic 与下游消费方
- [ ] 补/改单元测试：Application 层、Domain 层、Infrastructure 层（根规约硬性要求）

### 5.2 改动命令/事件 record 字段时

- [ ] 聚合根处理器 → 事件溯源处理器 → 投影 → QueryHandler → 应用层 → Web Mapper，**全链路一致**
- [ ] 已存事件的兼容性：Event Sourcing 重放历史事件，新增字段需保证反序列化向后兼容

### 5.3 新增 Feign 接口/端口时

- [ ] `ProductApi`/`ProductTemplateApi` 接口 + Controller 实现 + Request/Response/DTO
- [ ] 统一 `X-Tenant-ID` 请求头透传 tenantId
- [ ] 标注消费方（Policy/Underwriting/Clause），更新本文件 2.1 表

### 5.4 交付前核对（产品域已知缺陷，勿复刻）

- [ ] 新增模板类命令是否补齐聚合根 `@CommandHandler`（现有 Update/Activate/Deactivate 缺失）
- [ ] 新增产品查询是否补齐 `@QueryHandler`（现有 ByCondition/ClauseByProductId 缺失）
- [ ] 新增命令是否真正 `apply` 事件（勿复刻 `UpdateAttachProductCommand` 漏发事件）
- [ ] 注入方式用构造器（勿复刻模板侧 `@Autowired` 字段注入）
