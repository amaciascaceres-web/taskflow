# TaskFlow

Platform to manage tasks for technical teams.

Project built as a practical exercise to master microservices Docker, AWS, and Kubernetes
with Java and Spring Boot.

## Target

Build a real-world system that evolves from a simple prototype to a production-ready
software. Take technical decisions that are defensible in each stage.

## Current state

Week 1 - Single service with Spring Boot and PostgreSQL.

## Architecture

The system will evolve in the following phases:

### Phase 1 — Single service (weeks 1–2)
Just a single Spring Boot service with CRUD of tasks and PostgreSQL.

### Phase 2 — Microservices (weeks 2–3)
Split into multiple independient services by capacity of business.
- task-service: task management
- user-service: user and team management
- notification-service: events and notifications
- api-gateway: entry point for all services

### Phase 3 — Production (weeks 3–4)
- Dockerization of all services
- Deployment on AWS
- Orchestration with Kubernetes

## Technical stack

- Java 17
- Spring Boot 3.x
- Gradle
- PostgreSQL
- Docker + Docker Compose
- AWS
- Kubernetes

## Architecture decisions

The relevant technical decisions are documented in [docs/architecture-decisions.md](docs/architecture-decisions.md).

