---
from: python
status: draft
concept: "HTTP clients in Spring"
from_way: "requests or httpx with timeouts, raise_for_status and retry libraries such as tenacity."
native_way: "RestClient, declarative HTTP interfaces with @HttpExchange, timeouts in properties and status handlers."
sensei_tip: "httpx.Client(base_url=..., timeout=...) is a RestClient built from properties. Spring's declarative interface is like a typed client class that is generated for you."
---

| Python | Spring Boot 4 |
| --- | --- |
| httpx.get(url, timeout=5) | restClient.get().uri(url).retrieve() |
| raise_for_status() | onStatus(...) handlers |
| tenacity @retry | Spring Retry or Resilience4j |
| respx / responses mocks | MockRestServiceServer or WireMock |
