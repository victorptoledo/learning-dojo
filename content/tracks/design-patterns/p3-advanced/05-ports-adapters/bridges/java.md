---
from: java
status: draft
concept: "Ports and adapters"
from_way: "Controllers, services and JPA entities that all import vendor SDKs and each other, in packages by technical layer."
native_way: "A domain package with records, services and port interfaces; adapter packages for web, persistence and clients; one `@Configuration` composition root; ArchUnit or modules to enforce the rule."
sensei_tip: "ArchUnit turns the dependency rule into a JUnit test in three lines; add it on day one, because retrofitting the rule into a codebase that broke it for two years is a project."
---

| Java | Ports and adapters |
| --- | --- |
| `interface Accounts` in `..domain` | driven port |
| `@RestController` | driving adapter |
| Spring Data repository behind `Accounts` | driven adapter |
| `@Configuration` | composition root |
| ArchUnit, Maven modules, JPMS | enforcing the dependency rule |
| a translating client for a legacy API | anti-corruption layer |
