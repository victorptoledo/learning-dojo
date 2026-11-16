---
from: csharp
status: draft
concept: "Functions triggered by events, connected by managed services"
from_way: "An ASP.NET Core app with hosted services (`BackgroundService`) and an Azure Service Bus or RabbitMQ consumer."
native_way: "Lambda functions behind API Gateway and SQS, events on EventBridge, workflows in Step Functions."
sensei_tip: "Use the Lambda Annotations framework or ASP.NET Core minimal APIs hosted on Lambda; turn on SnapStart or Native AOT to cut .NET cold starts."
---

| .NET world | AWS |
| --- | --- |
| a controller action | a function behind an HTTP API |
| `BackgroundService` reading a queue | an SQS event source mapping |
| Durable Functions | Step Functions |
| Azure Functions timer trigger | EventBridge Scheduler |
