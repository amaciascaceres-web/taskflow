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