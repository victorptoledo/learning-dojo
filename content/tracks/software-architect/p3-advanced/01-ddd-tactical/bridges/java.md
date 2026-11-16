---
from: java
status: draft
concept: "Tactical DDD"
from_way: "JPA entities with public setters and `@OneToMany` collections reaching across the model, and services that hold every rule."
native_way: "Records for value objects and ids, aggregate roots as final classes with intention-revealing methods, other aggregates referenced by id, a repository per root, and domain events collected by the root (Spring Data's `@DomainEvents` or `AbstractAggregateRoot`)."
sensei_tip: "A JPA `@ManyToOne Customer customer` inside `Order` quietly makes the customer part of the order's aggregate. Prefer `@Embedded CustomerId customerId` and load the customer separately when needed."
---

| Java | Tactical DDD idea |
| --- | --- |
| `record Money(long cents, String currency)` | value object |
| `record OrderId(String value)` | identity as a value |
| a final class with no setters | aggregate root |
| `OrderRepository` for the root only | repository per aggregate |
| `AbstractAggregateRoot.registerEvent` | domain events recorded by the root |
| `@Version` optimistic locking | concurrency on the aggregate boundary |
