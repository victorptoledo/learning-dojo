---
from: java
status: draft
concept: "Choosing patterns"
from_way: "Applying the GoF catalogue as written for Java 1.4, or adding an interface, a factory and an abstract class to every feature."
native_way: "Choosing from the forces, letting Java 25 (lambdas, records, sealed types) and Spring (DI, proxies, events, templates) carry the patterns they already implement, and removing patterns that stopped paying."
sensei_tip: "Before writing a pattern in a Spring app, search the reference docs for it: there is very often an extension point (a bean type, an interceptor, a customizer) that is the pattern already."
---

| Problem | Java / Spring choice |
| --- | --- |
| varying algorithm | functional interface + lambdas, `Map<String, Bean>` |
| closed variants | sealed records + switch |
| cross-cutting behaviour | decorator, Spring AOP proxy |
| reacting to facts | `ApplicationEventPublisher` |
| foreign model | adapter / ACL behind a port |
| combinable rules | specifications (own or Spring Data) |
