# Migration from minikube to EKS

Theoretical documentation of the migration from minikube to EKS. It has
not been executed against a real AWS account — the goal is to record the
exact steps and the cost they carry, so it can be run the day this
project actually justifies it (see ADR-014).

## Prerequisites

```bash
# AWS CLI configured with valid credentials
aws sts get-caller-identity

# eksctl installed
brew install eksctl

# Images already published to ECR (see docs/aws-strategy.md, Phase 2/3)
# task-service, user-service, api-gateway, notification-service
```

## Step 1: Create the EKS cluster

```bash
eksctl create cluster \
  --name taskflow \
  --region eu-west-1 \
  --nodegroup-name taskflow-nodes \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed
```

This command takes ~15 minutes: it creates the control plane managed by
AWS, the VPC (if an existing one isn't passed in), the node Auto Scaling
Group, and configures `kubectl` locally against the new cluster.

Cost from this point on: ~$0.10/hour for the control plane (~$72/month)
plus the cost of the `t3.medium` node instances, regardless of whether
there's any load.

## Step 2: Verify the cluster connection

```bash
kubectl get nodes
```

Should show 2 nodes in `Ready` state. If `kubectl` can't find the
cluster, `eksctl` already updated `~/.kube/config`; check with
`kubectl config current-context`.

## Step 3: Update images in the manifests

The current Deployments (`infra/k8s/*/deployment.yaml`) reference local
minikube images:

```yaml
image: taskflow/task-service:latest
```

These need to be replaced with the ECR path:

```yaml
image: YOUR_ACCOUNT.dkr.ecr.eu-west-1.amazonaws.com/taskflow/task-service:latest
```

This applies to all four services: `task-service`, `user-service`,
`api-gateway` and `notification-service`. minikube doesn't need this step
because it builds the image directly inside its own internal Docker
daemon (`eval $(minikube docker-env)`); EKS has no such shared daemon, so
images must come from a registry the nodes can reach.

## Step 4: Apply the manifests (unchanged)

```bash
kubectl apply -f infra/k8s/
```

Exactly the same command as in minikube. ConfigMaps, Secrets,
Deployments and Services (except the gateway's type, see Step 5) require
no modification at all. This is the practical proof of today's lesson:
the YAML is portable, the underlying infrastructure is not.

## Step 5: Switch the gateway to LoadBalancer

In minikube, `api-gateway` uses `NodePort` because minikube has no way to
provision a real load balancer. In `infra/k8s/api-gateway/service.yaml`:

```yaml
# Before (minikube)
type: NodePort

# After (EKS)
type: LoadBalancer
```

```bash
kubectl get service api-gateway
```

After ~2 minutes, the `EXTERNAL-IP` column will show the DNS name of an
Application Load Balancer (ALB) provisioned automatically by the AWS
Load Balancer Controller / cloud-controller-manager on EKS. That ALB is
the only external entry point; all traffic still comes in through
`api-gateway`, exactly as specified in this repo's "Service ports"
section.

## Step 6: Tear down the cluster when it's not needed

```bash
eksctl delete cluster --name taskflow --region eu-west-1
```

**Important:** the EKS control plane charges ~$0.10/hour
(~$72/month) whether or not any workload is actually running. Unlike
ECS Fargate (zero cost with no active tasks) or a stopped EC2 instance,
an idle EKS cluster keeps generating a bill. That's why this project
does not keep an EKS cluster running permanently: the full process is
documented so it can be created, used and destroyed within a single
working session if it's ever needed to demonstrate this against a real
account.

## Comparison: EC2 vs ECS vs EKS

| Criterion            | EC2 + Compose        | ECS Fargate      | EKS                  |
|-----------------------|----------------------|------------------|----------------------|
| Fixed cost            | Instance cost only    | None             | ~$72/month           |
| Server management     | Manual                | None             | Partial              |
| Auto scaling          | No                    | Yes              | Yes                  |
| Portability           | No                    | AWS only         | Multi-cloud          |
| Learning curve        | Low                   | Medium           | High                 |
| When to use           | Learning / demo        | AWS production   | Multi-cloud / K8s    |

## What changes and what doesn't when moving from minikube to EKS

**Doesn't change:**
- The YAML manifests (Deployment, Service, ConfigMap, Secret) are
  identical except for the gateway's `Service` type.
- The `kubectl apply -f infra/k8s/` command is the same.
- The application structure, gateway routes, and each microservice's
  logic are untouched.

**Does change:**
- Cluster creation: `eksctl` / AWS console instead of `minikube start`.
- Image source: ECR instead of minikube's local Docker daemon.
- `LoadBalancer`-type `Service`s provision a real ALB (in minikube they
  do nothing useful).
- Pods can assume IAM roles (IRSA) to access RDS, Secrets Manager or S3
  without static credentials.
- Persistent storage: `hostPath` in minikube versus EBS/EFS in EKS.
- A fixed control-plane cost appears, present even with no load.

## Relationship to the AWS deployment strategy

This migration corresponds to Phase 3 described in
[`docs/aws-strategy.md`](aws-strategy.md): it is documented as a future
step, not as pending work for this learning project. The decision not to
run it against a real account is recorded in
[ADR-014](architecture-decisions.md#adr-014-eks-migration-documented-but-not-executed).
