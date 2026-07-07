# AWS Deployment Strategy

## Guiding principle
Start simple and add complexity only when there is a concrete need.
No premature over-engineering.

## Phase 1 — EC2 + Docker Compose (Day 23)
- A single t2.micro EC2 instance (free tier)
- Docker and Docker Compose installed on the instance
- The same docker-compose.yml used in local development
- Security Group: only port 8090 (gateway) exposed

Rationale: validate the deployment with the least friction.
Reuses exactly what already works locally.

## Phase 2 — ECS Fargate (future)
When to migrate: when we need auto-scaling or when the team grows
and managing EC2 instances becomes an operational burden.

What changes:
- Images in ECR instead of built on the instance
- Task Definitions instead of docker-compose.yml
- ALB as the entry point instead of the gateway directly
- Auto Scaling Groups to scale on demand

## Phase 3 — EKS (advanced future)
When to migrate: when we have 10+ microservices, need portability
across clouds, or the team already operates Kubernetes in other projects.

What changes compared to local:
- The same YAML manifests work without modification
- Configure IAM roles for access to RDS and Secrets Manager
- LoadBalancer Services instead of NodePort
- Ingress Controller (nginx or ALB) as the entry point