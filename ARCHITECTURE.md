# TtelGo eSIM Reseller Platform - Architecture

## Overview

TtelGo is a production-ready eSIM reseller platform backend built with Spring Boot 3.x and PostgreSQL. The platform enables both B2C (direct customer) and B2B (vendor reseller) eSIM sales with comprehensive billing, payment processing, and vendor management capabilities.

## Architecture Principles

### 1. **Modular Architecture**
- Feature-based modules (order, payment, vendor, esim, etc.)
- Clear separation of concerns: API → Application → Domain → Infrastructure
- Each module is independently maintainable and testable

### 2. **Anti-Corruption Layer**
- All external integrations (eSIM Go, Stripe) go through dedicated ACL services
- External DTOs never leak into domain model
- Resilience patterns (circuit breaker, retry) at integration boundaries

### 3. **Domain-Driven Design**
- Rich domain models with business logic
- Repository pattern for data access
- Service layer for orchestration and business workflows

### 4. **API-First Design**
- RESTful APIs with OpenAPI/Swagger documentation
- Versioned endpoints (`/api/v1/...`)
- Consistent response format across all endpoints

## System Components

```
┌─────────────────────────────────────────────────────────────┐
│                        API Layer                            │
│  ┌─────────┐  ┌──────────┐  ┌─────────┐  ┌──────────┐    │
│  │  B2C    │  │   B2B    │  │  Admin  │  │ Webhooks │    │
│  │   API   │  │  Vendor  │  │   API   │  │   API    │    │
│  └────┬────┘  └────┬─────┘  └────┬────┘  └────┬─────┘    │
└───────┼───────────┼──────────────┼────────────┼───────────┘
        │           │              │            │
┌───────┼───────────┼──────────────┼────────────┼───────────┐
│       │  Application Services Layer           │           │
│  ┌────▼────┐ ┌──▼──────┐ ┌───▼─────┐ ┌──▼──────────┐    │
│  │  Order  │ │ Payment │ │ Vendor  │ │   Webhook   │    │
│  │ Service │ │ Service │ │ Service │ │   Service   │    │
│  └────┬────┘ └────┬────┘ └────┬────┘ └──────┬──────┘    │
└───────┼───────────┼───────────┼──────────────┼───────────┘
        │           │           │              │
┌───────┼───────────┼───────────┼──────────────┼───────────┐
│       │      Domain Layer     │              │           │
│  ┌────▼────┐ ┌───▼─────┐ ┌───▼──────┐ ┌────▼────┐      │
│  │  Order  │ │ Payment │ │  Vendor  │ │  eSIM   │      │
│  │ Domain  │ │ Domain  │ │  Domain  │ │  Domain │      │
│  └────┬────┘ └────┬────┘ └────┬─────┘ └────┬────┘      │
└───────┼───────────┼───────────┼──────────────┼───────────┘
        │           │           │              │
┌───────┼───────────┼───────────┼──────────────┼───────────┐
│       │  Infrastructure Layer │              │           │
│  ┌────▼────┐ ┌───▼─────┐ ┌───▼──────┐ ┌────▼────────┐  │
│  │   JPA   │ │Mappers  │ │   ACL    │ │  External   │  │
│  │Entities │ │         │ │ Services │ │ Integrations│  │
│  └────┬────┘ └─────────┘ └────┬─────┘ └────┬────────┘  │
└───────┼───────────────────────┼──────────────┼───────────┘
        │                       │              │
   ┌────▼─────┐         ┌───────▼──────┐  ┌──▼────────┐
   │PostgreSQL│         │   eSIM Go    │  │  Stripe   │
   │ Database │         │  API v2.4    │  │    API    │
   └──────────┘         └──────────────┘  └───────────┘
```

## Key Features

### 1. **Multi-Tenant B2B Platform**
- Vendor onboarding and management
- Hybrid billing: PREPAID (wallet) + POSTPAID (credit limit)
- Per-vendor API keys for authentication
- Vendor-specific rate limiting

### 2. **Payment Processing**
- Stripe PaymentIntent integration
- B2C: Customer purchases eSIM bundles
- B2B: Vendor wallet top-ups
- Webhook processing with signature verification
- Idempotency for payment operations

### 3. **eSIM Lifecycle Management**
- Order creation and provisioning
- eSIM activation tracking
- Usage monitoring
- Expiration handling
- QR code generation and caching

### 4. **Resilience & Reliability**
- Circuit breakers for external APIs (Resilience4j)
- Retry with exponential backoff
- Idempotency keys for critical operations
- Order reconciliation jobs
- Webhook retry mechanism

### 5. **Security**
- JWT authentication for users/admin
- API key authentication for vendors
- Rate limiting per client
- Security headers (OWASP recommendations)
- Comprehensive audit logging
- Input validation
- SQL injection prevention (JPA)

### 6. **Observability**
- Structured logging with correlation IDs
- Audit trail for all critical operations
- Metrics with Micrometer/Prometheus
- Health and readiness endpoints (Actuator)

## Data Model

### Core Entities

1. **Users** - End customers and admin users
2. **Vendors** - B2B resellers with billing configuration
3. **Orders** - Purchase orders (B2C and B2B)
4. **Payments** - Stripe payment records
5. **eSIMs** - Provisioned eSIM inventory
6. **Vendor Ledger** - Double-entry accounting for vendors
7. **Idempotency Records** - Duplicate request prevention
8. **Audit Logs** - Complete audit trail
9. **Webhook Events** - Event store for Stripe webhooks
10. **Notifications** - System notifications and alerts

### Key Relationships

```
Users 1───* Orders *───1 eSIMs
Vendors 1───* Orders
Vendors 1───* Vendor_Ledger_Entries
Orders 1───1 Payments
Payments *───1 Stripe_Webhook_Events
```

## External Integrations

### 1. **eSIM Go API v2.4**
- **Purpose**: eSIM provisioning and lifecycle management
- **Pattern**: Anti-Corruption Layer with EsimGoService
- **Resilience**: Circuit breaker + retry
- **Endpoints Used**:
  - GET /bundles (catalogue)
  - GET /bundles/{id} (bundle details)
  - POST /order (create order)
  - GET /order/{id}/qrcode (QR code)
  - GET /order/{id} (order status)

### 2. **Stripe API**
- **Purpose**: Payment processing
- **Pattern**: Direct integration with StripeService
- **Flow**: PaymentIntent → Payment Element → Webhook confirmation
- **Webhooks Handled**:
  - `payment_intent.succeeded`
  - `payment_intent.payment_failed`
  - `charge.refunded`

## Async Jobs

1. **OrderReconciliationJob** - Retry failed/stuck orders (every 10 min)
2. **IdempotencyCleanupJob** - Remove expired records (hourly)
3. **EsimExpirationJob** - Mark expired eSIMs (daily)
4. **WebhookRetryJob** - Retry failed webhook processing (every 5 min)

## Configuration

Key configuration files:
- `application.yml` - Main configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production settings (create as needed)

Environment variables required:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `ESIMGO_API_KEY`
- `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`

## Deployment Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Load       │────▶│  Spring Boot │────▶│  PostgreSQL  │
│  Balancer    │     │    Apps      │     │   Database   │
│  (HTTPS)     │     │  (Multiple)  │     │   (Primary)  │
└──────────────┘     └──────────────┘     └──────────────┘
                            │                      │
                            │                      ▼
                            │              ┌──────────────┐
                            │              │  PostgreSQL  │
                            │              │  (Replica)   │
                            │              └──────────────┘
                            │
                     ┌──────▼───────┐
                     │    Redis     │
                     │   (Cache)    │
                     └──────────────┘
```

### Scaling Considerations

1. **Horizontal Scaling**: Stateless Spring Boot instances
2. **Database**: Read replicas for reporting queries
3. **Caching**: Redis for bundle catalogue, QR codes
4. **CDN**: Serve static assets and QR codes
5. **Background Jobs**: Separate job processing instances

## API Versioning

- Current version: `v1`
- All APIs under `/api/v1/`
- Future versions: `/api/v2/`, etc.
- Version included in base path (not headers)

## Testing Strategy

1. **Unit Tests**: Service and mapper logic
2. **Integration Tests**: API endpoints with MockMvc
3. **External API Mocking**: WireMock for eSIM Go/Stripe
4. **Security Tests**: OWASP validation
5. **Load Tests**: JMeter/Gatling (separate)

## Monitoring & Ops

1. **Health Checks**: `/actuator/health` (liveness), `/actuator/health/readiness`
2. **Metrics**: `/actuator/prometheus` (Prometheus format)
3. **Logs**: JSON structured logs to stdout
4. **Alerts**: Based on metrics (order failures, API errors, etc.)

## NFR Compliance

This architecture addresses all 57 Non-Functional Requirements:
- **Performance**: Caching, connection pooling, async processing
- **Security**: Authentication, authorization, encryption, audit
- **Reliability**: Circuit breakers, retries, idempotency
- **Scalability**: Stateless design, horizontal scaling
- **Maintainability**: Modular structure, clean code, documentation
- **Observability**: Logging, metrics, tracing

See `NFR_COMPLIANCE_MATRIX.md` for detailed mapping.

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL 14+
- **Migration**: Flyway
- **Security**: Spring Security, JWT
- **Resilience**: Resilience4j
- **Payments**: Stripe Java SDK
- **Testing**: JUnit 5, Mockito, MockMvc
- **Documentation**: SpringDoc OpenAPI
- **Metrics**: Micrometer, Prometheus
- **Build**: Maven

## Next Steps

1. Add JWT implementation for user authentication
2. Implement RBAC with Spring Security
3. Add Redis caching layer
4. Create comprehensive integration tests
5. Add API documentation with examples
6. Create deployment scripts and Docker images
7. Set up CI/CD pipeline
8. Implement monitoring and alerting

