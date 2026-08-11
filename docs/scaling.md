# Horizontal scaling: task-service

## Why task-service can scale horizontally

task-service is stateless: it holds no request state in memory between
calls. All state lives in PostgreSQL, and the Redis cache
(`RedisCacheConfig`, see `CLAUDE.md` "Caching") is shared across every
instance rather than kept per-pod. Because of that, running N replicas
requires no code change — Kubernetes just needs to know how many
resources each pod is allowed to use.

## Requests and limits

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

- **requests** is the minimum Kubernetes guarantees the pod before
  scheduling it onto a node. Without it, the scheduler can pack pods
  onto a node that doesn't actually have room for them.
- **limits** is the ceiling. CPU above the limit is throttled; memory
  above the limit gets the pod `OOMKilled`.
- Requests are set to half the limits, matching the ratio recommended
  for a service with a moderate, fairly steady load: enough headroom
  to absorb bursts without over-reserving capacity on the node.

`user-service` and `api-gateway` got the same memory budget
(256Mi/512Mi) since all three are JVM/Spring Boot processes with a
similar baseline footprint (heap + metaspace), but a lower CPU budget
(150m/300m) since they do less per-request work than task-service —
no DB writes to validate, no cache logic, no filtering/pagination.

## Scaling task-service to 3 replicas

```bash
# Declarative: replicas already set to 3 in the Deployment manifest
kubectl apply -f infra/k8s/task-service/deployment.yaml

# Imperative alternative, no YAML change needed
kubectl scale deployment task-service --replicas=3

# Verify
kubectl get pods -l app=task-service
```

## Verifying zero downtime during scaling

```bash
# Terminal 1: continuous requests against the gateway
while true; do
  curl -s http://localhost:8090/api/tasks
  sleep 0.5
done

# Terminal 2: scale while requests are in flight
kubectl scale deployment task-service --replicas=3
```

Requests keep succeeding throughout: Kubernetes only adds capacity
here (it doesn't kill existing pods to reach a higher replica count),
and the `task-service` Service load-balances across whichever pods are
`Ready` at any given moment — new pods only receive traffic once their
readiness probe passes.

## The bottleneck that always shows up: PostgreSQL

Scaling task-service to N replicas doesn't scale the database — every
replica still talks to the same single PostgreSQL instance. With more
replicas doing concurrent queries, PostgreSQL's connection limit and
I/O throughput become the actual ceiling, not application CPU.

Options when that becomes real, in increasing order of effort:

- **Redis cache (already in place):** `tasks` cache absorbs repeated
  reads of individual tasks, reducing load for the most common access
  pattern. List queries are deliberately not cached (see `CLAUDE.md`),
  so they still hit PostgreSQL directly on every request.
- **Connection pooling (PgBouncer):** caps the number of real
  PostgreSQL connections regardless of how many application pods
  exist, avoiding connection-limit exhaustion as replicas grow.
- **Read replicas (RDS):** move read-heavy queries (list/filter
  endpoints) to one or more read replicas, keeping writes on the
  primary. Requires the application to route reads vs. writes
  differently — not a transparent change.

None of these are implemented yet; task-service today points every
replica at the single `postgres` instance defined in
`infra/k8s/postgres/`. This is fine at current load and is noted here
so the next scaling step has a documented starting point.
