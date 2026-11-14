---
from: java
status: draft
concept: "Data modelling and polymorphism"
from_way: "Abstract classes, `extends`, simple enums and deep object hierarchies."
native_way: "Structs hold plain data; enum variants can carry different data (`enum WebEvent { PageLoad, KeyPress(char), Click { x: i64, y: i64 } }`)."
sensei_tip: "`match` is exhaustive: add a new enum variant and the compiler points at every place that doesn't handle it yet."
---
