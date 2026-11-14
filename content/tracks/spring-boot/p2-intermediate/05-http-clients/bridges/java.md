---
from: java
status: draft
concept: "HTTP clients in Spring"
from_way: "RestTemplate built with new in each class, string-concatenated URLs, no timeouts, and catch (Exception e) around every call."
native_way: "RestClient from the auto-configured builder or @HttpExchange interfaces, URI templates, connect and read timeouts, onStatus error mapping, and careful retries."
sensei_tip: "Write the timeout and the error mapping before the happy path: remote calls fail in production far more often than in development."
---

| Before | Spring Boot 4 |
| --- | --- |
| `new RestTemplate().getForObject(url + id, Quote.class)` | `client.get().uri("/quotes/{id}", id).retrieve().body(Quote.class)` |
| Feign client from Spring Cloud | `@HttpExchange` interface + `@ImportHttpServices` |
| No timeouts | `spring.http.clients.read-timeout=5s` |
| Hand-written retry loops | `@Retryable` with backoff (Spring Framework 7) |
