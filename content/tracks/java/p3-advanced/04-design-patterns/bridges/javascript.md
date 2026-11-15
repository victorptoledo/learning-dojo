---
from: javascript
status: draft
concept: "Patterns after lambdas, records and sealed types"
from_way: "Functions, closures and object literals, with few class-based patterns."
native_way: "Patterns expressed with lambdas, records and sealed interfaces."
sensei_tip: "Middleware chains in Express are the chain-of-responsibility pattern; in Java the same idea is a list of Function or a filter chain like Spring's."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| callback | functional interface + lambda |
| object literal of handlers | Map or enum with lambdas |
| middleware | chain of responsibility / filters |
| factory function | static factory method |
