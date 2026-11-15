---
from: csharp
status: draft
concept: "HTTP clients in Spring"
from_way: "IHttpClientFactory, typed clients, Refit interfaces and Polly policies."
native_way: "RestClient, @HttpExchange interfaces and Resilience4j."
sensei_tip: "Refit's interface clients are Spring's HTTP interfaces; Polly is Resilience4j. Configure the base URL and timeouts per client in configuration, as with named HttpClients."
---

| C# | Spring Boot 4 |
| --- | --- |
| IHttpClientFactory + typed client | RestClient.Builder bean |
| Refit [Get("/x")] | @GetExchange("/x") |
| Polly retry | Resilience4j or Spring Retry |
| HttpClient.Timeout | spring.http.clients timeouts |
