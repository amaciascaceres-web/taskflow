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