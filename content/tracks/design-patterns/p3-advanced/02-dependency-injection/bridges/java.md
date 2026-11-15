---
from: java
status: draft
concept: "Dependency injection"
from_way: "`@Autowired` private fields, `ApplicationContext.getBean(...)` in methods, static helpers and `new` of services inside services."
native_way: "Final fields set by the only constructor (Spring needs no annotation), `@Configuration` and `@Bean` as the composition root, `ObjectProvider` for laziness, and plain `new` in unit tests."
sensei_tip: "If a unit test needs `@SpringBootTest` to check a rule, the rule is wired too tightly. Aim for `new Service(fake1, fake2)` in milliseconds and keep Spring tests for wiring and slices."
---

| Java | DI idea |
| --- | --- |
| single constructor with final fields | constructor injection |
| `@Configuration` + `@Bean` | composition root, factories |
| `List<Exporter>`, `Map<String, Exporter>` | many implementations |
| `@Primary`, `@Qualifier` | choosing one |
| `ObjectProvider<T>` | lazy or repeated creation |
| `@ConditionalOnMissingBean` | replaceable defaults |
