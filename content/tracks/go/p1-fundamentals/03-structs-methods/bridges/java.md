---
from: java
status: draft
concept: "Classes, objects and inheritance"
from_way: "Classes with private fields, constructors, getters and setters, this, extends for inheritance, and every object handled through a reference."
native_way: "Structs with exported or package-private fields, NewX functions, methods with explicit receivers, embedding for composition, and a choice between values and pointers."
sensei_tip: "Do not write getters and setters by reflex. Export the field when callers may set it, keep it lower case and add a method when there is an invariant to protect."
---

| Java | Go |
| --- | --- |
| `class Account { private long balance; }` | `type Account struct { balance int64 }` |
| `new Account("id")` | `NewAccount("id")` or `&Account{ID: "id"}` |
| `this.balance += x` | `func (a *Account) Deposit(x int64) { a.balance += x }` |
| `record Point(int x, int y)` | `type Point struct{ X, Y int }` (a value, comparable) |
| `class Dog extends Animal` | `type Dog struct{ Animal }` (composition, no subtype) |
| `@Override` | a method on the outer type shadows the promoted one |
| `toString()` | `String() string` |
| `@JsonProperty("id")` | `` `json:"id"` `` struct tag |
| every object is a reference | values are copied; `*T` shares |
