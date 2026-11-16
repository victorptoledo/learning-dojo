---
from: go
status: draft
concept: "A task definition is the Pod; a service keeps it running"
from_way: "A small Go binary in a scratch image runs on a VM with systemd or compose."
native_way: "The image lives in ECR and runs as Fargate tasks in an ECS service, or as pods on EKS."
sensei_tip: "Go images can be tiny (`FROM scratch` or distroless); add CA certificates and read the task role through the SDK, which finds the ECS credentials endpoint by itself."
---

| Go world | AWS |
| --- | --- |
| `FROM scratch` image | a tiny Fargate task image |
| systemd restart policy | ECS service desired count |
| a Kubernetes Deployment | an ECS service, or the same Deployment on EKS |
| `os.Getenv("DB_PASSWORD")` | a task definition secret |
