---
from: csharp
status: draft
concept: "Service discovery by name"
from_way: "Connection strings and base URLs per environment in appsettings, pointing at load balancer addresses."
native_way: "A Service is the stable address: HttpClient calls http://orders.shop and kube-proxy balances over the ready Pods."
sensei_tip: "Register a typed HttpClient with a base address from configuration and set PooledConnectionLifetime, so long-lived connections are recycled across replicas."
---

| .NET world | Kubernetes |
| --- | --- |
| a base URL in appsettings | a Service DNS name |
| an Azure Load Balancer | `type: LoadBalancer` |
| `IHttpClientFactory` connection pooling | connection-level balancing |
| Kestrel `Urls=http://+:8080` | `containerPort: 8080`, named `http` |
