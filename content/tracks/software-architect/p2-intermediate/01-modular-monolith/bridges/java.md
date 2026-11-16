---
from: java
status: draft
concept: "Modular monolith"
from_way: "One Spring Boot application with `controller`, `service` and `repository` packages and JPA entities that join across every table."
native_way: "Top-level packages per module with an `api` subpackage, package-private internals, Spring Modulith's `ApplicationModules.of(App.class).verify()` or ArchUnit rules, `@ApplicationModuleListener` for in-process events, and one schema per module."
sensei_tip: "Spring Modulith treats each direct subpackage of the application's main package as a module and its root package as the api by default. Its `verify()` test is a ready-made fitness function."
---

| Java / Spring | Modular monolith idea |
| --- | --- |
| `com.shop.orders`, `com.shop.catalog` | modules by capability |
| package-private internals, `@NamedInterface` | published interface |
| `ApplicationModules.verify()` | boundaries as a test |
| `ApplicationEventPublisher`, `@ApplicationModuleListener` | in-process events |
| Event Publication Registry | events stored and retried (outbox) |
| Maven or Gradle module per business module | compiler-enforced boundaries |
