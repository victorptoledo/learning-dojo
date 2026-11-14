---
from: java
status: draft
concept: "Behavioural polymorphism"
from_way: "A `calculateShipping()` method with 15 branches like `if (type == EXPRESS) ... else if (type == STANDARD)`."
native_way: "One interface or trait implemented by each shipping option; a factory returns the right strategy without touching the calling code."
sensei_tip: "In languages with first-class functions, a strategy is often just a closure passed as a parameter."
---
