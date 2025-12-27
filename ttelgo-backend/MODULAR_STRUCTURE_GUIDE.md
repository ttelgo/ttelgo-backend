# Backend Modular Structure (R1)

This backend uses a **feature/domain-based modular structure**. Every feature lives in its own top-level package under `com.tiktel.ttelgo`.

## Module layout

Each module should follow (when applicable):

- `api`: Controllers + request/response DTOs + API mappers
- `application`: Use-cases / services / orchestration, depends on domain + ports
- `domain`: Entities/value objects/enums + domain rules (no Spring/web/db concerns)
- `infrastructure`: DB repositories, adapters, external clients, module configs

Example:

- `com.tiktel.ttelgo.order.api`
- `com.tiktel.ttelgo.order.application`
- `com.tiktel.ttelgo.order.domain`
- `com.tiktel.ttelgo.order.infrastructure`

## Shared modules

These are shared, cross-cutting packages:

- `com.tiktel.ttelgo.common`
- `com.tiktel.ttelgo.config`
- `com.tiktel.ttelgo.security`
- `com.tiktel.ttelgo.integration`

They should stay **generic** and must not depend on feature modules.

## Enforced by tests

Architecture rules are enforced by ArchUnit in:

- `src/test/java/com/tiktel/ttelgo/architecture/ModularArchitectureTest.java`

If a change violates modular boundaries (layer violations, cyclic module dependencies, etc.), `mvn test` will fail.


