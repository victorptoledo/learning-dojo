---
from: go
status: draft
concept: "Patterns after lambdas, records and sealed types"
from_way: "Small interfaces, functional options and composition over inheritance."
native_way: "Lambdas, records, sealed types and composition."
sensei_tip: "Go's functional options pattern maps to a builder or to a record with static factory methods; both make optional settings explicit."
---

| Go | Java 25 |
| --- | --- |
| func(opts ...Option) | builder or record factories |
| small interface | functional interface |
| embedding | composition with delegation |
| type switch over variants | sealed interface + switch |
