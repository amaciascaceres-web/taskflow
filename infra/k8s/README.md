# Kubernetes manifests

## ConfigMap vs Secret

Each service's non-sensitive configuration lives in a `configmap.yaml`
(URLs, ports, profile, DB host/name — plain text, safe to commit).
Credentials live in a `secret.yaml` (`DB_USER`, `DB_PASSWORD`) which is
**not committed** — it's listed in the root `.gitignore`
(`infra/k8s/**/secret.yaml`).

Kubernetes Secrets are only base64-encoded, not encrypted — anyone with
cluster access can read them. Keeping them out of git avoids leaking
credentials through the repo history; it does not make them secure on
its own. A real production setup would replace `secret.yaml` with AWS
Secrets Manager, Vault, or Sealed Secrets (see
[`docs/architecture-decisions.md`](../../docs/architecture-decisions.md)).

Deployments load both via `envFrom`, so the container gets every key
from the ConfigMap and Secret as environment variables — no `env:`
list to keep in sync by hand.

## First-time setup after cloning

`secret.yaml` doesn't exist yet in a fresh clone — create it from the
example for each service that has one:

```bash
cp infra/k8s/task-service/secret.yaml.example infra/k8s/task-service/secret.yaml
cp infra/k8s/user-service/secret.yaml.example infra/k8s/user-service/secret.yaml
```

Note the template is named `secret.yaml.example`, not `secret.example.yaml` —
`kubectl apply -R -f infra/k8s/` only picks up files ending in `.yaml`/`.yml`/`.json`,
so this naming keeps the placeholder `CHANGE_ME` credentials from ever being applied
to the cluster by accident.

Edit the `DB_USER` / `DB_PASSWORD` values if they differ from the
local dev defaults (`taskflow` / `taskflow`, matching the root
`README.md` database setup). `api-gateway` has no `secret.yaml` — it
holds no credentials, only service URLs.

## Applying manifests

```bash
kubectl apply -R -f infra/k8s/
```

`-R` is required — manifests live in per-service subdirectories.

`kubectl apply` is safe to run repeatedly — it only updates resources
that changed.
