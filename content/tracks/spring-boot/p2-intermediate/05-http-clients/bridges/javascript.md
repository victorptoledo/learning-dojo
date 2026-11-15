---
from: javascript
status: draft
concept: "HTTP clients in Spring"
from_way: "fetch or axios with interceptors and AbortController timeouts."
native_way: "RestClient and @HttpExchange interfaces with configured timeouts."
sensei_tip: "An axios instance with baseURL and interceptors is a RestClient.Builder bean; a typed API client like those generated from OpenAPI is an @HttpExchange interface."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| axios.create({ baseURL }) | RestClient.builder().baseUrl(...) |
| axios interceptors | ClientHttpRequestInterceptor |
| AbortSignal.timeout(5000) | read timeout settings |
| msw / nock | MockRestServiceServer / WireMock |
