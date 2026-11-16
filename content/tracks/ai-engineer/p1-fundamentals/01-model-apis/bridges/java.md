---
from: java
status: draft
concept: "A model call is a remote call with a richer result"
from_way: "RestClient or WebClient calls that return a DTO or throw on 4xx/5xx; retries with Resilience4j."
native_way: "An SDK call whose result carries content blocks, a stop reason and usage, where a 200 can still be a truncated or refused answer."
sensei_tip: "Model the response as a sealed hierarchy of content blocks and switch on the stop reason with a switch expression, so the compiler reminds you of max_tokens and refusal."
---

| Java | Model API |
| --- | --- |
| `ResponseEntity<Dto>` | a response with content, `stop_reason`, `usage` |
| `HttpClientErrorException` 4xx | do not retry (400/401/403/404) |
| `@Retry` on 503 | retry 429, 5xx, overloaded with backoff |
| `Flux<String>` from WebClient | a streamed SSE response |
| `@Value("${api.key}")` | the key from the environment or a vault |
| official SDK | `com.anthropic` Java SDK, `openai-java`, or Spring AI over them |
