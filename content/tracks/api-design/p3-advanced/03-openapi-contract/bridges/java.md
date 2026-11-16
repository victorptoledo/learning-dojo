---
from: java
status: draft
concept: "OpenAPI as a contract"
from_way: "springdoc-openapi annotations added after the fact and nobody reading `/v3/api-docs`."
native_way: "Either design-first with `openapi-generator` creating Spring interfaces (`interfaceOnly=true`) or code-first with springdoc, and in both cases the YAML is committed, linted and diffed in CI."
sensei_tip: "openapi-generator's `spring` generator with `interfaceOnly` and `useSpringBoot3` gives you controller interfaces; your class implements them and the compiler checks the contract."
---

| Java / Spring | OpenAPI concept |
| --- | --- |
| `openapi-generator` `spring` + `interfaceOnly` | design-first server |
| springdoc `/v3/api-docs` | code-first document |
| `@Schema`, `@Operation` | annotations for the document |
| `openapi-generator` `java` client | SDK |
| Spectral / oasdiff in CI | linting and breaking changes |
