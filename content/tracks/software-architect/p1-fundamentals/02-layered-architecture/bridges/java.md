---
from: java
status: draft
concept: "Layered architecture"
from_way: "`controller`, `service`, `repository` and `entity` packages with every class public, `@Service` classes that only forward to Spring Data repositories."
native_way: "Feature packages (`orders`, `payments`) with the controller, service and repository inside, package-private classes, one public interface per feature, and ArchUnit's `layeredArchitecture()` where layers remain."
sensei_tip: "Spring finds package-private `@Component` classes as happily as public ones. Nothing in Spring forces your services to be public; only package-by-layer does."
---

| Java / Spring | Layering idea |
| --- | --- |
| `@RestController` | presentation layer |
| `@Service` with `@Transactional` | application layer |
| `@Repository`, Spring Data interfaces | persistence layer |
| a service that only calls the repository | sinkhole |
| ArchUnit `layeredArchitecture().whereLayer(...)` | closed and open layers as a test |
| package-private classes in a feature package | package by feature |
