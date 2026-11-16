# Containers: ECR, ECS, Fargate and EKS

Lambda suits short, event-driven code. For long-running services, existing container
images, or work that needs more than 15 minutes, AWS runs containers: **ECS** (AWS's own
orchestrator), **EKS** (managed Kubernetes) and **App Runner** (a simple "give me an
image, give me a URL" service). All of them pull images from **ECR**.

## ECR

**Amazon ECR** is a private registry per account and region. Turn on **image scanning**
(basic, or enhanced with Inspector), **tag immutability** (so `1.8.0` never changes), and
**lifecycle policies** to expire old images. Push with
`aws ecr get-login-password | docker login --username AWS --password-stdin <registry>`.
Pull-through cache rules mirror public registries (Docker Hub, ECR Public, GHCR).

## ECS: task definitions, tasks and services

A **task definition** describes one or more containers that run together (like a
Kubernetes Pod):

```json
{
  "family": "orders-api",
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc",
  "cpu": "512", "memory": "1024",
  "runtimePlatform": { "cpuArchitecture": "ARM64", "operatingSystemFamily": "LINUX" },
  "taskRoleArn": "arn:aws:iam::111122223333:role/orders-api",
  "executionRoleArn": "arn:aws:iam::111122223333:role/orders-api-execution",
  "containerDefinitions": [{
    "name": "api",
    "image": "111122223333.dkr.ecr.eu-west-1.amazonaws.com/orders-api:1.8.0",
    "portMappings": [{ "containerPort": 8080 }],
    "secrets": [{ "name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:eu-west-1:111122223333:secret:orders-db" }],
    "logConfiguration": { "logDriver": "awslogs", "options": {
      "awslogs-group": "/ecs/orders-api", "awslogs-region": "eu-west-1", "awslogs-stream-prefix": "api" } },
    "healthCheck": { "command": ["CMD-SHELL", "curl -f http://localhost:8080/health || exit 1"] }
  }]
}
```

Two roles, often confused:

| Role | Used by | For |
| --- | --- | --- |
| **task execution role** | the ECS agent | pulling the image from ECR, writing logs, reading `secrets` |
| **task role** | **your code**, through the SDK | calling S3, DynamoDB, SQS... |

A **service** keeps N copies of a task running in a **cluster**, registers them in an ALB
target group, replaces unhealthy ones and deploys new task definition revisions:

- **Rolling** updates with `minimumHealthyPercent` and `maximumPercent`, and the
  **deployment circuit breaker** that rolls back a deployment that never becomes healthy.
- **Blue/green** deployments (built into ECS, or with CodeDeploy) shift traffic to the new
  version, with bake time and alarms for automatic rollback.
- **Service Auto Scaling**: target tracking on CPU, memory, ALB requests per target, or a
  custom metric such as SQS backlog per task.
- **Service Connect** (or Cloud Map) gives services DNS names to call each other.

With `awsvpc` networking each task gets its own ENI, private IP and **security group**, so
everything from Phase 1 applies per task. Run tasks in private subnets behind the ALB.
**ECS Exec** opens a shell in a running container through Session Manager.

## Fargate or EC2

| | Fargate | EC2 capacity providers |
| --- | --- | --- |
| Hosts | none to manage | your Auto Scaling group of instances |
| Pricing | per task vCPU and GB per second; **Fargate Spot** up to ~70 % less | per instance; better packing and Savings Plans at scale |
| Fits | most services, small teams | GPUs, daemons, very large steady fleets, special kernels |

**ECS Managed Instances** sits between them: AWS provisions and patches the EC2 instances
for you.

## EKS

**Amazon EKS** runs the Kubernetes control plane (API server, etcd) across three AZs for
you; you run the data plane with **managed node groups**, **Karpenter**, **Fargate** profiles
or **EKS Auto Mode** (AWS manages nodes, load balancing and storage add-ons). Everything
from the Kubernetes track applies; AWS adds:

- **VPC CNI**: pods get VPC IP addresses, so plan subnets with room.
- **AWS Load Balancer Controller**: Ingress and Gateway resources become ALBs and NLBs.
- **EKS Pod Identity** (or the older **IRSA**, IAM roles for service accounts): a
  ServiceAccount maps to an IAM role, so each pod gets its own credentials, never the
  node's role.
- **Access entries** map IAM principals to Kubernetes RBAC.

Choose EKS when you want the Kubernetes API and ecosystem (Helm, operators, portability)
and can run it; choose ECS for less to operate and deep AWS integration. **App Runner**
fits a single web service with minimal configuration.

## Summary

- ECR stores images; scan them, make tags immutable, expire old ones.
- An ECS task definition is the Pod: containers, CPU and memory, roles, logs, secrets.
- The execution role is for ECS; the task role is for your code.
- Services keep tasks running behind an ALB, deploy safely and scale on metrics.
- Fargate removes hosts; EKS gives managed Kubernetes with Pod Identity for AWS permissions.

## Further reading

- Amazon ECR user guide: <https://docs.aws.amazon.com/AmazonECR/latest/userguide/what-is-ecr.html>
- Amazon ECS task definitions: <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definitions.html>
- Task IAM role and task execution role: <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html>
- Amazon ECS deployment types: <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-types.html>
- AWS Fargate: <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html>
- Amazon EKS user guide: <https://docs.aws.amazon.com/eks/latest/userguide/what-is-eks.html>
- EKS Pod Identity: <https://docs.aws.amazon.com/eks/latest/userguide/pod-identities.html>
