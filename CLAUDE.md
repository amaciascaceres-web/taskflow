# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

TaskFlow is a microservices-based task management platform built as a learning exercise for Java, Spring Boot, Docker, AWS, and Kubernetes. Currently in Week 3 of development.

## Build and run commands

Each service is an independent Gradle project. Run commands from inside the service directory (e.g., `cd task-service`).

```bash
# Build a service
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.taskflow.task.TaskServiceApplicationTests"

# Build the JAR without running tests
./gradlew bootJar -x test

# Run locally (requires SPRING_PROFILES_ACTIVE=dev)
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

## Full stack with Docker Compose

```bash
# Start all services (postgres, redis, task-service, user-service, api-gateway)
docker compose -f infra/docker/docker-compose.yml up --build

# Teardown
docker compose -f infra/docker/docker-compose.yml down
```

## Local development without Docker

Prerequisites: PostgreSQL on port 5433, Redis on 6379.

```bash
# Create databases (one-time setup)
psql -U postgres -p 5433 -c "CREATE USER taskflow WITH PASSWORD 'taskflow';"
psql -U taskflow -p 5433 -c "CREATE DATABASE taskflow;"
psql -U taskflow -p 5433 -c "CREATE DATABASE taskflow_users;"
```

Each service must run with `SPRING_PROFILES_ACTIVE=dev`. All other variables have defaults in `application-dev.yml`.

## Service ports

| Service              | Port  |
|----------------------|-------|
| api-gateway          | 8090  |
| task-service         | 8080  |
| user-service         | 8081  |
| notification-service | 8082  |
| PostgreSQL           | 5433  |
| Redis                | 6379  |

All external traffic should go through the api-gateway. Routes: `/api/tasks/**` → task-service, `/api/users/**` → user-service.

## Architecture

### Package structure (ADR-002)

All services follow the same domain-first package layout:

```
com.taskflow.<service>
  controller/         HTTP layer — @RestController, DTOs (request/response records)
  application/        Business logic — @Service, mappers
  domain/             Enums, domain exceptions, pure domain types
  infrastructure/     JPA entities, repositories, external HTTP clients
  config/             Spring configuration beans
  shared/             Cross-cutting concerns (GlobalExceptionHandler, ApiErrorResponse)
```

### Key design decisions

- **Entity/DTO separation (ADR-003):** `TaskEntity` never leaves the infrastructure layer. `TaskMapper` (in `application/mapper/`) converts between entity and `TaskResponse`/`CreateTaskRequest`.
- **Enum slugs (ADR-005):** Enums use kebab-case `slug` fields for API serialization (`"in-progress"`, `"high"`). `@JsonCreator` on `fromSlug()` handles deserialization. Never use the Java enum name in the API.
- **`ddl-auto: update` only in dev (ADR-006):** The `dev` profile enables automatic schema updates. Production will use Flyway migrations.
- **EnumType.STRING (ADR-004):** All enums are persisted as strings, not ordinals.

### Inter-service communication

- **task-service → user-service:** Synchronous `RestTemplate` call to validate `assigneeId` on task creation. 3000ms timeout. Returns 503 if user-service is unreachable, 404 if user not found. No circuit breaker yet (see ADR-007).
- **task-service → notification-service:** Fire-and-forget HTTP POST. Failures are logged as WARN and never propagated to the caller (see ADR-008).

### Caching (task-service)

Redis cache — single cache name:
- `tasks` — keyed by task `id`, evicted on update/delete via `@CacheEvict`

List queries (`GET /api/tasks` with filters and pagination) are not cached: with
`status × assigneeId × priority` combinations the key space explodes, and any write
would need `allEntries=true` eviction defeating the purpose.

`RedisCacheConfig` uses a custom `ObjectMapper` with `DefaultTyping.NON_FINAL` and type validation restricted to `com.taskflow.task` to support polymorphic deserialization of cached `TaskResponse` records.

### Authentication (partial — ADR-012)

Stateless JWT strategy. Spring Security is wired in `task-service` but currently permits all requests (`anyRequest().permitAll()`). Week 5 will change this to `.authenticated()` and add JWT filter chain validation at the api-gateway.

## Architecture decisions

All significant technical decisions are documented in [`docs/architecture-decisions.md`](docs/architecture-decisions.md). Read it before proposing architectural changes.
