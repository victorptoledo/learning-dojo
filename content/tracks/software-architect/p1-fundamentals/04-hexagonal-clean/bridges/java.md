---
from: java
status: draft
concept: "Hexagonal and clean architecture"
from_way: "JPA entities with annotations as the domain model, services that call Spring Data repositories and `RestTemplate` directly."
native_way: "A domain package of records and classes with no framework imports, port interfaces in it, adapter packages for web, persistence and clients, one `@Configuration` composition root, and an ArchUnit test for the dependency rule."
sensei_tip: "If removing `spring-boot-starter-data-jpa` from the pom breaks the domain package's compilation, the dependency rule is already broken. That is the quickest check there is."
---

| Java / Spring | Hexagonal idea |
| --- | --- |
| records and plain classes in `..domain` | the core |
| `interface Orders` in `..domain` | driven port |
| `@RestController`, `@KafkaListener` | driving adapters |
| Spring Data repository behind `Orders` | driven adapter |
| `@Configuration` with `@Bean` methods | composition root |
| ArchUnit `onionArchitecture()` | the dependency rule as a test |
