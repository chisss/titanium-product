# Titanium Product Domain

This is the Product Domain service for the Titanium insurance platform. It provides core functionality for managing insurance products, including product creation, auditing, revision, and invalidation.

## Domain Overview

The Product Domain is a core sub-domain of the insurance platform, connecting the Clause Domain (rule foundation) and the Policy Domain (business carrier). Its core value is to encapsulate the commercial configuration of insurance products, supporting the entire process from product上架 (listing) to policy issuance and underwriting.

## Core Features

- **Product Lifecycle Management**: Create, audit, revise, and invalidate products
- **Version Control**: Maintain product versions, ensuring consistency for existing policies
- **Clause Association**: Bind multiple clauses to a product, with support for main and additional clauses
- **Pricing Configuration**: Define pricing rules and factors for different product types
- **Versioned Rate Tables**: Manage draft, validation, publication, retirement, and immutable rate rows
- **Versioned Pricing Plans**: Bind product versions to feature contracts, rate tables, fixed rule artifacts, effective periods, rounding, and release regression cases
- **Charge Components**: Define versioned customer-price and internal-cost components with visibility, payer, direction, source, accounting class, and effective period
- **Calculation Models**: Compose charge components as a validated DAG with deterministic execution and immutable content hashes
- **Tax Policies**: Define versioned tax, stamp-duty, and regulatory-levy policies with jurisdiction, tax base, inclusive/exclusive pricing, exemption features, and regulatory evidence
- **Actuarial Workbench APIs**: Manage charge components and calculation models through `/web/v2/actuarial`; the administration UI calls PricingPlan a “定价包” while Java/API names remain compatible
- **Premium Quotes**: Execute the effective PricingPlan and return non-posting premium quotes with replayable version evidence
- **Premium Confirmations**: Create immutable `ISSUANCE_CONFIRM` and `MAINTENANCE` calculations with customer-price lines, internal-cost lines, totals, and hash/version evidence
- **Lifecycle Premium Differences**: Compare an original confirmed calculation with a `MAINTENANCE` calculation and persist immutable debit/credit, tax, internal-cost, and line-level differences
- **Versioned Maintenance Premium Quotes**: Recalculate maintenance inputs against an exact product/pricing-plan version and return a 24-hour content-addressed quote without posting to Billing
- **Pricing Integration Ports**: Resolve versioned typed features and execute fixed-version rule artifacts through Product-owned ports
- **Versioned Maintenance Offerings**: Publish product/plan-specific maintenance item availability and resolve immutable evidence for Maintenance case creation
- **Multi-Tenancy Support**: Ensure data isolation across different tenants

## Project Structure

The project follows DDD (Domain-Driven Design) principles with a clear separation of layers:

```
titanium-product/
├── titanium-product-common/          # Common utilities and constants
├── titanium-product-domain/          # Domain layer - core business logic
│   └── src/main/java/com/titanium/product/
│       ├── aggregate/              # Aggregate roots (InsuranceProduct)
│       ├── command/                # Command definitions
│       ├── query/                  # Query definitions
│       ├── event/                  # Event definitions
│       ├── repository/             # Repository interfaces
│       ├── service/                # Domain services
│       ├── valueobject/            # Value objects
│       └── entity/                 # Aggregate entities
├── titanium-product-infrastructure/ # Infrastructure layer - persistence and external systems
│   └── src/main/java/com/titanium/product/
│       ├── repository/             # Repository implementations
│       ├── jpa/                    # JPA repositories
│       ├── entity/                 # Database entities
│       └── mapper/                 # Entity mappers
├── titanium-product-application/   # Application layer - business logic orchestration
│   └── src/main/java/com/titanium/product/
│       ├── command/                # Command services
│       └── query/                  # Query services
├── titanium-product-api/            # API layer - DTOs and interfaces
├── titanium-product-web/            # Web layer - REST controllers
├── titanium-product-bootstrap/      # Application entry point
└── titanium-product-query/          # Query layer - query handlers and services
```

## Core Components

### Aggregate Root

- **InsuranceProduct**: The main aggregate root representing an insurance product, containing all product configurations and business logic.
- **RateTableDefinition**: Owns rate-table metadata, row conflict validation, content hashing, publication, and retirement rules.
- **PricingPlanDefinition**: Owns pricing configuration lifecycle, immutable content hashing, release gates, effective periods, and regression cases.
- **ChargeComponentDefinition**: Owns a versioned fee/cost definition, visibility, applicability, lifecycle, and immutable published content.
- **CalculationModelDefinition**: Owns calculation nodes and edges, DAG validation, lifecycle, and version hash.
- **TaxPolicyDefinition**: Owns the lifecycle, effective period, exact tax base, price mode, exemption feature, regulatory evidence, and immutable content hash of a tax policy.
- **PremiumCalculation**: Stores the immutable confirmed pricing fact, totals, lines, and replay evidence.
- **PremiumLifecycleAdjustment**: Stores the immutable lifecycle difference between original and replacement calculation facts.

### Key Value Objects

- **InsureCondition**: Represents the eligibility criteria for a product, such as age range, occupation restrictions, and group size limits.
- **PricingBasicRule**: Defines the pricing structure for a product, including pricing type, base rate, and pricing factors.
- **PricingFactor**: Represents a specific factor affecting pricing, such as age or vehicle type.

### Commands

- **CreateProductCommand**: Creates a new product with basic configuration and clause associations.
- **AuditProductCommand**: Approves a product, making it effective for new policies.
- **ReviseProductCommand**: Creates a new version of an existing product with updated configuration.
- **InvalidateProductCommand**: Invalidates an effective product, preventing new policies from being created.
- **UpdateProductClauseRelCommand**: Updates the clause associations for a product (only allowed in draft status).

### Events

- **ProductCreatedEvent**: Published when a new product is created.
- **ProductAuditedEvent**: Published when a product is approved and becomes effective.
- **ProductRevisedEvent**: Published when a new version of a product is created.
- **ProductInvalidatedEvent**: Published when a product is invalidated.

### Queries

- **FindProductByIdQuery**: Retrieves a product by its ID.
- **FindProductByConditionQuery**: Retrieves products matching specified criteria (form, type, status).
- **FindProductClauseByProductIdQuery**: Retrieves the clauses associated with a product.

## Technology Stack

- Java 21
- Spring Boot 4.0.1
- Axon Framework 4.10.0 (CQRS and Event Sourcing)
- Spring Data JPA
- MapStruct for entity-DTO mapping
- Lombok for boilerplate code reduction
- MySQL for data persistence

## Getting Started

### Prerequisites

- JDK 21 or higher
- Maven 3.9 or higher
- MySQL 8.0 or higher

### Building the Project

```bash
cd /Users/sunwei/titanium-project/titanium-product
mvn clean install
```

### Running the Application

```bash
cd /Users/sunwei/titanium-project/titanium-product/titanium-product-bootstrap
mvn spring-boot:run
```

## API Documentation

Once the application is running, you can access the Swagger UI at:
```
http://localhost:8082/swagger-ui.html
```

Pricing endpoints:

- `POST /api/v1/products/{productId}/premium-quotes`: calculate a non-posting premium quote.
- `POST /api/v1/products/{productId}/premium-calculations`: confirm an issuance premium and create an immutable calculation fact.
- `GET /api/v1/premium-calculations/{calculationId}`: retrieve a tenant-scoped calculation fact for Policy/Billing verification.
- `POST /api/v1/premium-calculations/lifecycle-adjustments`: create an immutable lifecycle debit/credit difference fact.
- `GET /api/v1/premium-lifecycle-adjustments/{adjustmentId}`: retrieve a tenant-scoped lifecycle difference fact.
- `POST /api/v1/products/{productId}/maintenance-premium-quotes`: create a versioned maintenance quote from frozen before/proposed snapshots and complete pricing inputs.
- `/web/v1/products/{productId}/rate-tables`: manage rate-table drafts and versions for the administration backend.
- `/web/v1/products/{productId}/pricing-plans`: manage pricing-plan drafts, test cases, approval, release regression, publication, retirement, and queries.
- `/web/v1/products/{productId}/maintenance-offerings`: create, publish, and inspect versioned Product maintenance offerings.
- `GET /api/v1/products/{productId}/maintenance-offering`: resolve the unique published offering by product/plan version, policy status, channel, and business time.
- `/web/v2/actuarial/products/{productId}/charge-components`: manage versioned charge-component definitions and lifecycle.
- `/web/v2/actuarial/products/{productId}/calculation-models`: manage calculation DAG definitions and lifecycle.
- `/web/v2/actuarial/products/{productId}/tax-policies`: manage versioned tax-policy definitions and lifecycle.

Pricing result boundaries:

- `QUOTE` returns only customer-visible charge lines and never exposes internal cost details.
- `CONFIRM` persists the complete customer-price and internal-cost fact for authorized Product/actuarial users and downstream reconciliation.
- Maintenance premium quotes reuse `MAINTENANCE` confirmation and lifecycle-difference facts, return `quoteVersion=resultHash`, expire 24 hours after creation, and never call Billing or Payment.
- Product owns price definitions; Channel owns channel contracts and commission schemes; Billing owns receivables, commission payables, and ledger reconciliation.
- Tenant-scoped masking is applied by Admin when authorized users inspect confirmed internal details.

V2-A local acceptance data can be initialized idempotently from the project root:

```bash
./scripts/seed_actuarial_v2a_gold.sh
```

The script prepares one life, motor/property, short-term, and group product, advances all required assets to an executable state, and prints `READY` only after final assertions pass.

V2-B tax and full-receivable acceptance data can be initialized idempotently from the project root:

```bash
./scripts/seed_actuarial_v2b_tax_gold.sh
```

The script extends the same four product archetypes with exclusive tax, inclusive tax, and exemption scenarios, then verifies Product calculation evidence and Billing receivable/tax-ledger facts.

V2-D1 lifecycle differences and Billing balance postings can be accepted from the project root:

```bash
./scripts/accept_actuarial_v2d_lifecycle.sh
```

The script verifies both debit and credit changes, immutable evidence, idempotency conflicts, Product/Billing line reconciliation, and MySQL persistence.

Pricing integration boundaries:

- `FeatureResolutionPort` maps Product feature-contract snapshots to Feature Center `POST /api/v1/features:resolve`.
- `RuleComputationPort` executes Rule Engine `POST /api/v1/rule-artifacts/{code}/versions/{version}:compute` with an explicit artifact and input Schema version.
- Both adapters fail closed on remote failures, empty payloads, malformed typed values, or response identity mismatches.
- Premium quotes use the single published PricingPlan effective for the requested currency and business time, require an exact Product version and pricing-mode binding, and return PricingPlan/rate-table/feature-snapshot/rule-artifact evidence.
- Maintenance quotes additionally require the exact expected PricingPlan version, frozen before/proposed snapshot references, an original confirmed calculation, and a complete payload SHA-256. `ENDORSEMENT` and `SURRENDER` are supported lifecycle intents; `REVERSAL` remains a separate reversal contract.
- Products without a published PricingPlan continue to use the Phase 1 `RATE_TABLE` path; non-rate-table products fail closed until a PricingPlan is published.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
