---
from: java
status: draft
concept: "Specification"
from_way: "`findActiveAndCountryAndOrdersGreaterThan...` derived query methods and the same conditions copied into services and SQL strings."
native_way: "`Predicate<T>` for in-memory rules, named specifications for domain rules, sealed records with interpreters when a rule must also become a query, or Spring Data's `Specification` when only JPA queries need it."
sensei_tip: "Spring Data derived query names longer than four words are a sign you need specifications (or a `@Query`); the method name has become a query language."
---

| Java | Specification idea |
| --- | --- |
| `Predicate.and/or/negate` | composable rules |
| `sealed interface Spec` + records | specification as data |
| `switch` over specs | interpreters (memory, SQL) |
| Spring Data `JpaSpecificationExecutor` | specs as JPA criteria |
| Querydsl `BooleanExpression` | specs as typed query objects |
