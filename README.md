# TaskFlow

Platform to manage tasks for technical teams.

Project built as a practical exercise to master microservices, Docker, AWS, and Kubernetes
with Java and Spring Boot.

## Target

Build a real-world system that evolves from a simple prototype to a production-ready
software. Take technical decisions that are defensible in each stage.

---

## Quick start — Docker Compose (recommended)

Requires Docker Desktop running.

```bash
# 1. Clone and enter the repo
git clone <repo-url>
cd taskflow

# 2. Start the full stack (PostgreSQL, Redis, all services, api-gateway)
docker compose -f infra/docker/docker-compose.yml up --build

# 3. Verify everything is up
curl http://localhost:8090/api/tasks/ping
```

To tear down:

```bash
docker compose -f infra/docker/docker-compose.yml down
```

---

## Local development (without Docker)

### Prerequisites

- JDK 17+
- PostgreSQL running on port **5433**
- Redis running on port **6379**

### One-time database setup

```bash
psql -U postgres -p 5433 -c "CREATE USER taskflow WITH PASSWORD 'taskflow';"
psql -U taskflow -p 5433 -c "CREATE DATABASE taskflow;"
psql -U taskflow -p 5433 -c "CREATE DATABASE taskflow_users;"
```

### Start each service

Each service is an independent Gradle project. Open a terminal per service:

```bash
# Terminal 1 — task-service (port 8080)
cd task-service
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# Terminal 2 — user-service (port 8081)
cd user-service
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# Terminal 3 — notification-service (port 8082)
cd notification-service
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# Terminal 4 — api-gateway (port 8090)
cd api-gateway
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

All configuration variables have defaults in each service's `application-dev.yml`.

### Run tests

```bash
cd task-service && ./gradlew test
cd user-service && ./gradlew test
cd notification-service && ./gradlew test
```

---

## Service ports

| Service              | Port |
|----------------------|------|
| api-gateway          | 8090 |
| task-service         | 8080 |
| user-service         | 8081 |
| notification-service | 8082 |
| PostgreSQL           | 5433 |
| Redis                | 6379 |

All external traffic should go through the **api-gateway** on port 8090.

---

## API endpoints

All requests go through the api-gateway. Routes: `/api/tasks/**` → task-service, `/api/users/**` → user-service.

### task-service — `/api/tasks`

| Method | Path             | Description                                      |
|--------|------------------|--------------------------------------------------|
| GET    | /api/tasks/ping  | Health check                                     |
| POST   | /api/tasks       | Create a task                                    |
| GET    | /api/tasks/{id}  | Get task by id                                   |
| GET    | /api/tasks       | List tasks (filterable, paginated — see below)   |
| PUT    | /api/tasks/{id}  | Update a task                                    |
| DELETE | /api/tasks/{id}  | Delete a task                                    |

**GET /api/tasks query parameters**

| Parameter    | Type       | Example                          | Description                     |
|--------------|------------|----------------------------------|---------------------------------|
| `status`     | string[]   | `?status=todo&status=in-progress`| Filter by one or more statuses  |
| `assigneeId` | long[]     | `?assigneeId=1&assigneeId=2`     | Filter by one or more assignees |
| `priority`   | string[]   | `?priority=high`                 | Filter by one or more priorities|
| `page`       | int        | `?page=0`                        | Page number (0-based, default 0)|
| `size`       | int        | `?size=20`                       | Page size (default 20)          |
| `sort`       | string     | `?sort=createdAt,desc`           | Sort field and direction        |

Response shape:

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 143,
  "totalPages": 8
}
```

Valid status slugs: `todo`, `in-progress`, `done`
Valid priority slugs: `low`, `medium`, `high`

### user-service — `/api/users`

| Method | Path              | Description        |
|--------|-------------------|--------------------|
| GET    | /api/users/health | Health check       |
| POST   | /api/users        | Create a user      |
| GET    | /api/users        | List all users     |
| GET    | /api/users/{id}   | Get user by id     |
| PUT    | /api/users/{id}   | Update a user      |
| DELETE | /api/users/{id}   | Delete a user      |

### notification-service — internal only

| Method | Path                                 | Description                              |
|--------|--------------------------------------|------------------------------------------|
| POST   | /internal/notifications/task-created | Called by task-service (not via gateway) |

---

## Current state

- **Week 1** — Single service with Spring Boot and PostgreSQL.
- **Week 2** — Split into microservices (task-service + user-service), inter-service communication via RestTemplate, API Gateway with Spring Cloud Gateway.
- **Week 3** — notification-service with fire-and-forget HTTP, Docker Compose full stack, health/readiness/liveness probes, Redis caching, database indexes, full test pyramid (unit + integration + E2E + architecture), dynamic multi-value filtering, and pagination on `GET /api/tasks`.

---

## Architecture

The system will evolve in the following phases:

### Phase 1 — Single service (weeks 1–2)
Just a single Spring Boot service with CRUD of tasks and PostgreSQL.

### Phase 2 — Microservices (weeks 2–3)
Split into multiple independent services by business capability:
- task-service: task management
- user-service: user and team management
- notification-service: events and notifications
- api-gateway: entry point for all services

### Phase 3 — Production (weeks 4–5)
- Deployment on AWS
- Orchestration with Kubernetes
- Full JWT authentication at the gateway

---

## Technical stack

- Java 17
- Spring Boot 3.x
- Gradle
- PostgreSQL
- Redis
- Docker + Docker Compose
- AWS (planned)
- Kubernetes (planned)

---

## Architecture decisions

Significant technical decisions are documented in [docs/architecture-decisions.md](docs/architecture-decisions.md).
