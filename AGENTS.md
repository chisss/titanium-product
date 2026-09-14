# titanium-product 产品域 - 多 Agent 协作指南

> 版本: V1.2 ｜ 最后更新: 2026-09-14（m11-1405：§5.4 补「`ProductTemplate.supportedCoverages/supportedExclusions` 属未落地契约、勿按补校验施工」核对项；上一版 m10-1304：Kafka 入站判定「不应接通」并删除空壳；连带修正 §2.2/§2.3/§5.4 过期记载）
> 配合根目录 `AGENTS.md` 使用；本文聚焦产品域内部协作与上下游边界。

---

## 一、模块定位与边界

产品域是**配置枢纽**，处于条款域与保单/核保域之间。

```
        ┌────────────┐   条款规则底座
        │  Clause域   │ ◄────────────  Product域 (本模块, 8082)
        └────────────┘   (同步 Feign 时点查询，非事件订阅)
                                          │ 产品生效后输出配置
                          ┌───────────────┼────────────────┐
                          ▼               ▼                ▼
                    ┌──────────┐    ┌──────────┐    ┌──────────────┐
                    │ Policy域  │    │核保域      │    │ 其他消费方     │
                    │(出单配置) │    │(核保配置)  │    │              │
                    └──────────┘    └──────────┘    └──────────────┘
```

- **被调用**：Policy 域、Underwriting 域通过 Feign 拉取产品配置（出单/核保/保单形态）
- **不消费 Clause 域事件**：产品以「绑定具体版本」**刻意隔离**条款变更对产品的影响（m10-1304 判定，见 §2.3）
- **发布事件**：产品创建/审核/下架三主题已实装外发，定位为**对外集成出口**（仓内零消费者，m6-907 判定）

> 现状（2026-09-14 实读核对，取代旧文「均为 TODO 桩」）：① **Feign 已通**（Policy/Underwriting 同步调用）；
> ② **Kafka 外发已实装**（`ProductKafkaEventPublisher`，三主题真实发送）；③ **Kafka 入站判定「不应接通」**——
> 原 `ClauseEventListener` 为空壳，已删除（m10-1304），`infrastructure/listener` 目录随之清空。

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

### 2.2 发布的领域事件（Kafka topic）

| 事件 | topic | 消费方 |
|------|-------|--------|
| `ProductCreatedEvent` | `titanium.product.created` | 无仓内消费者——**对外集成出口**（三方系统/数据平台），见《跨域事件目录-2026-09.md》§六.15 |
| `ProductAuditedEvent` | `titanium.product.audited` | 同上（在售状态已由出单试算**同步强校验**承担，异步订阅属降级）|
| `ProductInvalidatedEvent` | `titanium.product.invalidated` | 同上（存量保单为产品快照，下架**不回溯**）|

### 2.3 消费的外域事件（🔴 判定「不应接通」，勿再按「规划」施工）

| 来源 | 事件 | 曾规划 topic | 判定 |
|------|------|-------------|------|
| Clause 域 | `ClauseVersionUpdatedEvent` | ~~`titanium.clause.version-updated`~~ | 🔴 该主题名在 `CrossDomainTopics` 中**根本不存在**（孤儿命名）；本域**不消费** |
| Clause 域 | `ClauseDeactivatedEvent` | ~~`titanium.clause.deactivated`~~ | 同上 |

> **2026-09-14 m10-1304 定论**：clause 域实际发布的主题是 `clause-status-changed`（唯一），其候选承接方曾指向本域。
> 四个判据（设计语义相反／无消费基础设施／无反查能力／同类先例对称）判定**不应接通**，空壳
> `infrastructure/listener/ClauseEventListener` 已删除、目录随之清空。判据全文见
> [跨域事件目录 §六.17](../docs/技术文档/跨域事件目录-2026-09.md)。

🔴 **不要再新建「监听条款域事件的消费者」**。

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

### 5.4 交付前核对

- [ ] 注入方式用构造器（勿复刻模板侧 `@Autowired` 字段注入）
- [ ] 改 Kafka topic/载荷 → 同步《跨域事件目录-2026-09.md》§三 表与 `CrossDomainTopics`，否则构建期 `CrossDomainEventCatalogTest` 失败
- [ ] 勿新建「监听条款域事件」的消费者（已判定不应接通，见 §2.3）
- [ ] 🔴 **勿把 `ProductTemplate.supportedCoverages/supportedExclusions` 当已建成能力**（m11-1405 定性）：该字段属**未落地契约**——仅存在于聚合根/命令/事件三处，web DTO 无入口、Mapper 与 DataInitializer 硬编码 `List.of()`、DDL 无 `supported_coverages` 列、零消费方。落地前须先补写入入口（含更新路径）+ 定义取值域 + 与 `PolicyStructureConfig.LiabilityStructure` 语义划界，**勿按「补校验」直接施工**（详见 CLAUDE.md §7.11）

> ✅ 原列的三条「缺失」已由 m0-715 实读复核推翻：命令处理器齐备（14 个全覆盖）、查询处理器齐备、
> `UpdateAttachProductCommand` 已 `apply` 事件——详见 `CLAUDE.md` §7.1/§7.2/§7.4，勿再据此施工。
