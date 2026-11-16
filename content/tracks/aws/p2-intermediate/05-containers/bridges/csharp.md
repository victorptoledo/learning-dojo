---
from: csharp
status: draft
concept: "A task definition is the Pod; a service keeps it running"
from_way: "ASP.NET Core runs in containers on Azure App Service or AKS."
native_way: "ECS on Fargate (or EKS) runs the image from ECR behind an ALB; App Runner for the simplest case."
sensei_tip: "Use the chiseled, non-root .NET images and `dotnet publish /t:PublishContainer` to build them; on Graviton use the arm64 base images."
---

| .NET world | AWS |
| --- | --- |
| Azure Container Registry | ECR |
| Azure Container Apps | ECS on Fargate or App Runner |
| AKS | EKS |
| Managed Identity | task role or EKS Pod Identity |
