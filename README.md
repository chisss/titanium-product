# Titanium Product Domain

This is the Product Domain service for the Titanium insurance platform. It provides core functionality for managing insurance products, including product creation, auditing, revision, and invalidation.

## Domain Overview

The Product Domain is a core sub-domain of the insurance platform, connecting the Clause Domain (rule foundation) and the Policy Domain (business carrier). Its core value is to encapsulate the commercial configuration of insurance products, supporting the entire process from product上架 (listing) to policy issuance and underwriting.

## Core Features

- **Product Lifecycle Management**: Create, audit, revise, and invalidate products
- **Version Control**: Maintain product versions, ensuring consistency for existing policies
- **Clause Association**: Bind multiple clauses to a product, with support for main and additional clauses
- **Pricing Configuration**: Define pricing rules and factors for different product types
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
http://localhost:8080/swagger-ui.html
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
