# Architecture Decision Records

## ADR-001: Start with a single service instead of microservices

**Date:** 2026-03-23  
**Status:** Accepted

**Context:**  
The domain is not fully defined yet. The team size is one person.
The priority is validating functionality quickly over operational decoupling.

**Decision:**  
Start with a single Spring Boot service (task-service) containing all
the business logic. Split into microservices only when a concrete need
appears (independent scaling, independent deployment, team boundaries).

**Consequences:**
- Lower operational overhead
- Simpler local development and debugging
- Will require refactoring when independent scaling is needed
- Allows us to validate the domain before deciding service boundaries

**Alternatives considered:**
- Microservices from day one: discarded due to premature complexity

---

## ADR-002: Package structure by domain, not by technical layer

**Date:** 2026-03-23  
**Status:** Accepted

**Context:**  
There are two common ways to organise packages in a Spring Boot project:
by technical layer (controllers/, services/, repositories/) or by domain
capability (task/, user/, notification/).

**Decision:**  
Organise by domain capability:

    com.taskflow.task
      controller/
      application/
      domain/
      infrastructure/
      config/

**Consequences:**
- All code related to a capability lives in one place
- Easier to extract into a separate microservice in the future
- Each package is cohesive and can evolve independently

**Alternatives considered:**
- Package by technical layer: discarded because it scatters related
  code across multiple packages and makes future extraction harder

## ADR-003: Separate JPA entity from API DTOs

**Date:** 2026-03-26
**Status:** Accepted

**Context:**
The API needs to expose task data to clients while persisting it in PostgreSQL.
The simplest approach would be to use the JPA entity directly as the API contract.

**Decision:**
Keep JPA entities and API DTOs as separate classes:
- `TaskEntity` — maps to the database table, lives in `domain/entity`
- `CreateTaskRequest` — defines the API input contract, lives in `controller/dto`
- `TaskResponse` — defines the API output contract, lives in `controller/dto`
- `TaskMapper` — handles transformation between them, lives in `application`

**Consequences:**
+ Database schema and API contract can evolve independently
+ No risk of accidentally exposing internal fields (audit fields, internal IDs)
+ Validation concerns (API) are separated from persistence concerns (database)
- More classes to maintain
- Mapping code required between layers

**Alternatives considered:**
- Expose entity directly: discarded because any database change would
  break the API contract, and serialization issues with JPA lazy loading
  are hard to control

---

## ADR-004: Use EnumType.STRING for status and priority persistence

**Date:** 2026-03-26
**Status:** Accepted

**Context:**
JPA offers two strategies for persisting enums: ORDINAL (stores the
position as an integer) and STRING (stores the name as text).

**Decision:**
Use `@Enumerated(EnumType.STRING)` for both `TaskStatus` and `TaskPriority`.

**Consequences:**
+ Database values are human-readable (TODO, HIGH instead of 0, 2)
+ Adding or reordering enum values never corrupts existing data
+ Easier to debug and query directly in the database
- Slightly more storage than ORDINAL

**Alternatives considered:**
- EnumType.ORDINAL: discarded because reordering enum values would
  silently corrupt all existing data

---

## ADR-005: Enum slug for API serialization with @JsonCreator

**Date:** 2026-03-26
**Status:** Accepted

**Context:**
Enums can be serialized/deserialized by Jackson using their Java name
(HIGH, IN_PROGRESS) or a custom value. The display text responsibility
belongs to the frontend via i18n.

**Decision:**
Add a `slug` field to enums with kebab-case values (e.g. "in-progress",
"high") and use `@JsonCreator` to deserialize from slug.
The slug is what travels in the API. Display text is a frontend i18n
responsibility.

**Consequences:**
+ API contract is stable even if enum names change internally
+ Slugs are URL-friendly and consistent with REST conventions
+ Frontend can use slugs as i18n keys without transformation
- Requires @JsonCreator on every enum

**Alternatives considered:**
- Use Java enum name directly: simpler but exposes internal naming
  conventions and makes URL usage awkward (IN_PROGRESS in a URL)
- Include displayName in the API response: discarded because it
  couples the API to a specific language

---

## ADR-006: ddl-auto strategy per environment

**Date:** 2026-03-26
**Status:** Accepted

**Context:**
Hibernate can manage the database schema automatically. The strategy
needs to balance development speed with production safety.

**Decision:**
Use `ddl-auto: update` for local development only.
When the project reaches AWS deployment, migrate to Flyway with
versioned migrations and set `ddl-auto: none`.

**Consequences:**
+ Fast iteration in local development without manual schema management
- `update` is not safe for production (can make destructive changes)
+ Flyway will provide controlled, versioned, reversible migrations
  when needed

**Alternatives considered:**
- validate: useful to detect schema mismatches but does not create
  tables, too restrictive for early development
- Flyway from day one: discarded to keep focus on infrastructure
  topics (Docker, AWS, Kubernetes) rather than migration tooling

## ADR-007: Resilience strategy for user-service calls

**Context:**
task-service calls user-service synchronously to validate
the assigneeId when creating tasks. This dependency introduces
a potential point of failure.

**Decision:**
Implemented basic differentiated error handling (404 vs 503)
with a 3000ms timeout on RestTemplate. Circuit Breaker, Retry
and Fallback patterns were not implemented in this phase.

**Reasons:**
- Current traffic volume does not justify the operational
  complexity of Resilience4j
- An explicit 503 is more honest than a silent fallback
  in a system without monitoring configured yet
- With a single instance of user-service, a Circuit Breaker
  adds no real value over a well-configured timeout

**Consequences:**
+ Simple and predictable code
+ Client receives clear errors and can retry
- If user-service degrades slowly, threads are exhausted
  until the timeout kicks in (no fail-fast behaviour)
- Without retry, a transient network failure returns 503
  even if user-service is healthy

**Future evolution:**
Add Resilience4j when the following conditions are met:
- user-service runs more than one replica
- The system has observability in place (circuit breaker metrics)
- Traffic volume justifies protecting Tomcat's thread pool

## ADR-008: Fire and Forget for Notifications

Context: notification-service is a secondary system. Its failure
must not prevent the primary operation of creating tasks.

Decision: HTTP fire-and-forget communication. Failures are logged
as WARN but not propagated.

Consequences:
+ Task creation does not depend on notification-service
- Notifications may be lost if the service is down
+ Next step: message queue (RabbitMQ/Kafka) for durability

Alternatives considered:
- Synchronous with error propagation: discarded, couples operations
  of different criticality levels
- Kafka from the start: discarded due to premature complexity

## ADR-009: Shared vs Per-Service Database

Context: Locally we use the same PostgreSQL instance for all
services for simplicity.

Decision: Separate database per name (taskflow, taskflow_users)
on the same local instance. In production, separate instances.

Consequences:
+ Logical isolation between services
+ Allows migration to separate instances without changing code
- Locally we share infrastructure (acceptable for development)

## ADR-010: When This System Would Justify Kafka

Context: We currently use synchronous HTTP between services.

Decision: Introduce Kafka when all of the following conditions are met:
- Notifications cannot be lost under any circumstances
- Event volume exceeds 1000/second sustained
- More than 2 consumers of the same event

Consequences:
+ Guaranteed event durability
+ Complete temporal decoupling between services
- Adds significant operational complexity
- Requires managing offsets, consumer groups and rebalancing

## ADR-011: When a Modular Monolith Would Be Sufficient

Context: TaskFlow in its current state has 3 microservices
with moderate load and a one-person team.

Decision: Microservices are introduced here for deliberate
learning purposes, not because load or team size demands it.

Cases where a modular monolith would be a better fit:
- Team smaller than 5 people
- Domain not yet fully defined
- Load under 100 sustained req/s
- No need for independent deployment by capacity

Consequences:
+ Honesty about technical decisions
+ Demonstrates judgment about when to apply each architecture

## ADR-012: JWT Authentication Strategy

Context: The system has multiple microservices. An authentication
strategy is needed that does not require shared state between
services.

Decision: Stateless JWT. The API Gateway validates the token and
internal services trust the propagated token without re-validating
against a central database.

Consequences:
+ No shared state: each service validates the signature locally
+ Horizontal scaling without coordination between instances
- Tokens cannot be revoked before expiration: validation is purely
  signature + `exp` claim, with no server-side check, so a token
  remains valid for its full lifetime regardless of anything that
  happens afterward. Shortening that window requires either a
  blacklist (reintroduces shared state) or short-lived access tokens
- The payload travels in every request (minimal but real overhead)

Alternatives considered:
- Database sessions: discarded, requires shared state
- Full OAuth2 with authorization server: reserved for
  when the system has multiple clients or SSO

Future considerations:
- Short-lived access token + refresh token: would shrink the exposure
  window of a leaked token (minutes instead of `jwt.expiration-ms`,
  currently 24h) and reintroduce revocability — but only on the
  refresh path, not on every request. Not implemented — the system
  has no session-invalidating action yet (no logout endpoint) for a
  revocation list to react to, so there's nothing to revoke against
  today.

Status: Partial implementation (Day 16). Trust boundary for internal
services refined in ADR-015.

---

## ADR-013: Database indexes for task query filters

**Date:** 2026-07-02
**Status:** Accepted

**Context:**
`GET /api/tasks` now supports dynamic filtering by `status`, `assigneeId`, and `priority`,
resolved via JPA Specifications. Without indexes, any filtered query requires a full table
scan. As the tasks table grows (completed sprints accumulate), this becomes a bottleneck.
The most common real-world query pattern is "find my open tasks" — filtered by both
`assigneeId` and `status` simultaneously.

**Decision:**
Add three indexes to the `tasks` table via `@Index` on `TaskEntity`:

- `idx_tasks_status` on `(status)` — single-column filter by status
- `idx_tasks_assignee_id` on `(assignee_id)` — single-column filter by assignee
- `idx_tasks_assignee_status` on `(assignee_id, status)` — composite for the
  "my open tasks" query pattern; the leading column (`assignee_id`) also serves
  single-column assignee lookups, making `idx_tasks_assignee_id` technically
  redundant but kept for clarity and for queries that filter on assignee alone
  without a status constraint (planner may choose either index depending on selectivity)

`priority` is intentionally not indexed: it has only 3 values (low/medium/high) so
cardinality is too low for an index to help — a full scan with a filter is faster.

**Consequences:**
+ Filtered queries on status, assigneeId, or both avoid full table scans
+ No additional application code — indexes are managed by Hibernate `ddl-auto: update`
  in dev; Flyway migration will be added when moving to production
- Small write overhead on every INSERT and UPDATE to the tasks table
- Adds a composite index for the dominant query pattern; re-evaluate if query
  patterns shift significantly

**Alternatives considered:**
- Index on `priority`: discarded due to low cardinality (3 values); sequential scan + filter is faster
- Composite `(status, assignee_id)`: leading column reversed; less useful since
  status-only queries are less selective than assignee-only queries in practice
- No indexes: only acceptable while the table is small (early development)

---

## ADR-014: EKS migration documented but not executed

**Date:** 2026-08-03
**Status:** Accepted

**Context:**
TaskFlow currently runs on minikube locally, with the AWS deployment strategy
(`docs/aws-strategy.md`) already covering EC2 + Docker Compose (Phase 1) and
outlining ECS Fargate (Phase 2) and EKS (Phase 3) as future steps. EKS is
worth understanding for this project's learning goals, but its control plane
has a fixed cost of ~$0.10/hour (~$72/month) regardless of whether any
workload is running — unlike ECS Fargate, which costs nothing with no active
tasks. There is no production load, multi-cloud requirement, or team size
that justifies that cost today.

**Decision:**
Document the full migration from minikube to EKS with real, commented
commands (`docs/eks-migration.md`), but do not provision an EKS cluster
against a real AWS account. The Kubernetes manifests already written for
minikube (`infra/k8s/`) are kept as the single source of truth: they are
portable as-is, so no EKS-specific fork of them is maintained.

**Consequences:**
+ No recurring AWS cost for a cluster that would sit idle between
  learning sessions
+ The manifests stay minikube-first, with EKS-specific changes (image
  registry, gateway Service type) called out explicitly rather than
  duplicated into a parallel set of files
+ The migration can be executed on demand in a single session
  (`eksctl create cluster` → validate → `eksctl delete cluster`) if it
  ever needs to be demonstrated against a real account
- The documented steps are unverified against a live EKS cluster; some
  details (IRSA setup, ALB controller behavior) may need adjustment
  when actually executed
- Revisit this decision if the project reaches the conditions already
  listed in `docs/aws-strategy.md` Phase 3 (10+ microservices,
  multi-cloud need, or a team already operating Kubernetes elsewhere)

**Alternatives considered:**
- Provision a real EKS cluster temporarily to validate the steps: rejected
  for a learning project — the cost isn't justified just to confirm
  commands that are well-documented AWS behavior
- Skip EKS entirely and stop at ECS Fargate: rejected — understanding the
  EKS migration path is a stated learning goal, so it's documented even
  though not executed

---

## ADR-015: Trust boundary for propagated identity headers

**Date:** 2026-08-18
**Status:** Accepted

**Context:**
The first JWT pass (Day 31 practice) put full signature validation
*and* a `UserDetailsService.loadUserByUsername()` database lookup inside
`JwtAuthFilter` in task-service. That contradicts ADR-012, which already
states the API Gateway validates the token and internal services trust
it without re-validating against a central database. Re-deriving the
user from a DB row on every request is closer to re-authentication than
to trusting a signed token, and it would couple task-service to
user-service's schema — something ADR-009 (per-service databases)
already rules out.

**Decision:**
JWT signature validation happens exactly once, in api-gateway. On a
valid token, the gateway injects trusted headers (`X-User-Email`,
`X-User-Role`) into the request before forwarding it downstream, after
stripping any `X-User-*` headers the client sent itself. Internal
services (task-service, user-service, notification-service) never see
the raw token or the signing key; they read the trusted headers and
build a `SecurityContext` directly from them — no signature check, no
database lookup.

This is only safe because internal services are not reachable from
outside the cluster: no Ingress, NodePort, or LoadBalancer Service
exposes task-service, user-service, or notification-service directly,
so a client cannot hit them and forge `X-User-Role: ADMIN` without
going through the gateway. If that ever changes (e.g. a service gets
its own public endpoint), this trust boundary breaks and that service
would need to validate the JWT itself again.

**Consequences:**
+ Matches ADR-012 as originally written: one validation point, no
  per-service DB coupling for authentication
+ Removes `jjwt-*` and the `UserDetailsService` dependency from
  task-service entirely — the JWT signing key never needs to exist
  outside api-gateway
- Internal services fully trust the gateway's headers; a compromised or
  misconfigured internal network (no NetworkPolicy, a debug port opened
  to the cluster's Pod network) becomes an authentication bypass, not
  just a data leak
- Headers must be stripped and re-set on every hop, not just added —
  any gateway filter that forgets to strip inbound `X-User-*` first
  reopens the spoofing path

**Alternatives considered:**
- Keep signature validation in every internal service (original ADR-012
  intent, "each service validates the signature locally"): rejected for
  now — it's more defense-in-depth, but duplicates the signing key
  across every service's config/secret, and this project already
  documents the DB-lookup mistake as the more urgent issue to fix
- Re-validate the DB user on every request in task-service: rejected —
  this is what the Day 31 practice code did; it defeats the stateless
  benefit JWT is supposed to provide and couples services via a shared
  schema

---

## ADR-016: RBAC with @PreAuthorize and domain object security

**Date:** 2026-08-27
**Status:** Accepted

**Context:**
URL-level authorization (`.requestMatchers(...).hasRole(...)` in
`SecurityConfig`) can express "only ADMIN may hit this path", but not
"only the assignee of *this specific task* may hit this path" — that
requires knowing which resource the URL points to, not just the URL
shape. TaskFlow needs both: `ROLE_USER` manages their own tasks,
`ROLE_ADMIN` manages everyone's.

The trusted principal `HeaderAuthenticationFilter` builds only carried
an email (ADR-015). `TaskEntity` has no `ownerEmail` — ownership is
`assigneeId`, a `Long` referencing user-service's internal id. Comparing
an email against a `Long` doesn't work, so an ownership check needs the
authenticated user's numeric id, not just their email.

**Decision:**
- Method-level authorization via `@EnableMethodSecurity` +
  `@PreAuthorize` on `TaskController`, not URL patterns: `create` only
  requires `isAuthenticated()`; `get`/`update`/`delete` require
  `hasRole('ADMIN') or @taskSecurity.isOwner(#id, authentication)`;
  `getAll` (list-everything) is `hasRole('ADMIN')` only.
- Ownership logic lives in `TaskSecurityService` (`@Service("taskSecurity")`),
  not inline SpEL — one `TaskRepository.findById` lookup, independently
  unit-testable with Mockito, no Spring context required.
- Extended the ADR-015 header set with `X-User-Id`: `JwtService`
  (user-service) adds an `id` claim, `JwtPropagationFilter` (api-gateway)
  strips-then-sets it exactly like the existing headers, and
  `HeaderAuthenticationFilter` (task-service) now builds an
  `AuthenticatedUser(Long id, String email)` record as the principal
  instead of a bare email string — so `TaskSecurityService.isOwner()`
  can compare `principal.id()` against `TaskEntity.assigneeId` directly,
  no extra lookup needed.

**Consequences:**
+ Authorization logic sits next to the code it protects and is testable
  in isolation (`TaskSecurityServiceTest`, pure Mockito, no HTTP context)
+ No network call to resolve ownership — the id travels in the same
  token/header chain the email and role already use
- `@EnableMethodSecurity` must be present in *any* Spring context that
  exercises `@PreAuthorize`, including test slices — a `@WebMvcTest`
  that forgets to `@Import` the config carrying it makes `@PreAuthorize`
  silently inert (every call falls through unauthorized), a mistake this
  project already made once wiring up `TaskControllerTest`
- `AccessDeniedException` needs its own `@ExceptionHandler` (403,
  `ACCESS_DENIED`) — in production `ExceptionTranslationFilter` catches
  it before `@RestControllerAdvice` ever sees it, but any test that
  disables the security filter chain (`addFilters = false`) loses that
  safety net and the exception falls through to the generic 500 handler
  unless one exists

**Alternatives considered:**
- Resolve `assigneeId` against user-service inside `TaskSecurityService`
  instead of propagating `X-User-Id`: rejected — adds a network call to
  every authorization check and couples task-service to user-service's
  availability for a decision that should be purely local (ADR-009)
- URL-level authorization only: rejected — cannot express per-resource
  ownership without inspecting the path variable, which is exactly what
  `@PreAuthorize` + SpEL is for

---

## Known gap: no request tracing

**Status:** Not implemented. Identified 2026-09-04, during Day 32 work.

No trace/correlation id is generated, propagated across services, or
attached to log lines anywhere in the system. `JwtPropagationFilter`
(api-gateway) rewrites `X-User-Id`/`X-User-Email`/`X-User-Role` on every
request but adds nothing like `X-Trace-Id`; `UserServiceClient`
(task-service → user-service) sends its outbound call with no
correlation header either. As a result, correlating a single request's
log lines across api-gateway, task-service, and user-service today means
grepping by `taskId`/`assigneeId` by hand — which doesn't work once two
requests for the same task overlap in time.

Worth revisiting once Kafka lands (Days 35-36), since a trace id is also
what makes producer→consumer log correlation possible across a broker.
Two options, not yet decided between:
- Manual `X-Trace-Id` header, generated at api-gateway if absent and
  propagated the same strip-then-set way as `X-User-Id` (ADR-015),
  picked up into SLF4J's MDC by each service so it shows up in every log
  line without touching call sites individually
- Micrometer Tracing (Spring Boot's Sleuth successor) + a backend like
  Zipkin, which instruments `RestTemplate` and Kafka automatically
  instead of wiring the header by hand — more setup, no per-call-site
  maintenance