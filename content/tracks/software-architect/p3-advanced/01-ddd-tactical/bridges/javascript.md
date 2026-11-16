---
from: javascript
status: draft
concept: "Tactical DDD"
from_way: "Plain objects mutated anywhere, ORM models with every field public, and rules in route handlers."
native_way: "Readonly value types (with branded ids in TypeScript), classes with private fields for aggregate roots and methods that check invariants, a repository per root, and events collected by the root."
sensei_tip: "Branded types (`type OrderId = string & { readonly brand: 'OrderId' }`) make TypeScript reject an order id where a customer id is expected, at zero runtime cost."
---

| JavaScript / TypeScript | Tactical DDD idea |
| --- | --- |
| `Readonly<{ cents: number; currency: string }>` | value object |
| branded string types | identity as a value |
| a class with `#private` fields and methods | aggregate root |
| a repository per root over Prisma or Drizzle | repository per aggregate |
| `this.#events.push(...)` and `pullEvents()` | domain events recorded by the root |
| a `version` column in the update's `where` | concurrency on the aggregate boundary |
