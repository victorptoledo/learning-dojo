---
from: javascript
status: draft
concept: "Objects and classes"
from_way: "Object literals with any shape, classes with constructor and this, prototypes and extends, and objects always shared by reference."
native_way: "Structs with a fixed, typed shape, methods declared outside the type with a receiver, embedding instead of extends, and values copied unless you use a pointer."
sensei_tip: "this in JavaScript depends on how a function is called; a Go receiver is just the first parameter written before the name. There is nothing to bind."
---

| JavaScript | Go |
| --- | --- |
| `{ id: "a", total: 3 }` | `Order{ID: "a", Total: 3}` (the type is declared first) |
| `class Order { constructor(id) { this.id = id } }` | `func NewOrder(id string) *Order` |
| `order.total += 1` inside a method | `func (o *Order) Add() { o.Total++ }` |
| `class Special extends Order` | `type Special struct{ Order }` |
| `toString()` | `String() string` |
| `JSON.stringify` with property names | struct tags `` `json:"total"` `` |
| `const b = { ...a }` | `b := a` (a full copy of the struct) |
| `#private` fields | lower case fields, private to the package |
