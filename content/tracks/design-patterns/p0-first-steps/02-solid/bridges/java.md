---
from: java
status: draft
concept: "SOLID principles"
from_way: "A 2,000-line `@Service` that calls `new` on its collaborators, reads `LocalDate.now()` and switches on enum constants in six places."
native_way: "Small classes split by reason to change, constructor-injected collaborators behind interfaces the service owns, `Clock` injected, sealed types or strategies for the axis that varies."
sensei_tip: "If a test needs `@SpringBootTest` to check a pricing rule, dependency inversion is missing: the rule should be testable with `new PricingRule(...)` and a fake."
---

| Java | SOLID idea |
| --- | --- |
| `@Controller` / `@Service` / `@Repository` | single responsibility by layer |
| registering a `Converter` bean | open/closed: extend by adding |
| `List.of(...).add(x)` throws | a Liskov surprise |
| `Supplier<T>`, `Runnable` | interface segregation to the extreme |
| constructor injection + `Clock` | dependency inversion |
