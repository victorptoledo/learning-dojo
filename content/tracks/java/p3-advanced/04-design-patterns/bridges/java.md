---
from: java
status: draft
concept: "Patterns after lambdas, records and sealed types"
from_way: "One class per strategy and command, visitors with accept and visit methods, abstract template classes, and static singletons."
native_way: "Lambdas for one-method strategies, records and sealed interfaces for commands and data, pattern switches instead of visitors, and dependencies passed in."
sensei_tip: "When a pattern needs more boilerplate than behaviour, check whether a lambda, a record or a sealed switch already expresses it."
---

| Classic | Modern Java |
| --- | --- |
| `class FlatShipping implements ShippingCost` | `ToLongFunction<Order> flat = o -> 500;` |
| `Visitor<R>` with `accept`/`visit` | `switch (expr) { case Num(var v) -> ...; }` |
| Command classes with fields and getters | `record Deposit(String account, long cents) implements Command` |
| Telescoping constructors | Static factories, builders or withers |
| `getInstance()` singleton | One instance created at startup and injected |
