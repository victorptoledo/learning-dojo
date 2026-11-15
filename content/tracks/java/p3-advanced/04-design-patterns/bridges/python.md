---
from: python
status: draft
concept: "Patterns after lambdas, records and sealed types"
from_way: "First-class functions and duck typing make most patterns a few lines or unnecessary."
native_way: "Modern Java gets closer: lambdas for strategies and commands, records for values, sealed types for visitors."
sensei_tip: "Where Python passes a function, Java passes a lambda for a functional interface. A dict of functions keyed by name is a Map<String, Function<...>> in Java too."
---

| Python | Java 25 |
| --- | --- |
| pass a function | a lambda for a strategy interface |
| dict of handlers | Map<String, Handler> |
| @decorator | a wrapping class or a lambda |
| dataclass builder kwargs | a builder or a record with defaults in factories |
