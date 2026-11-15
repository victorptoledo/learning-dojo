---
from: go
status: draft
concept: "HTTP clients in Spring"
from_way: "http.Client with Timeout, a context per call, and hand-written retries."
native_way: "RestClient with configured timeouts and declarative interfaces."
sensei_tip: "Go's default client has no timeout and Spring's defaults are generous too. Set connect and read timeouts explicitly, as you would set http.Client{Timeout: ...}."
---

| Go | Spring Boot 4 |
| --- | --- |
| http.Client{Timeout: 5 * time.Second} | timeouts in spring.http.clients.* |
| resp.StatusCode >= 500 | onStatus(HttpStatusCode::is5xxServerError, ...) |
| retry loop with backoff | Resilience4j |
| httptest.NewServer | WireMock |
