---
from: csharp
status: draft
concept: "Classes, structs and properties"
from_way: "Reference classes and value structs as separate kinds, properties with get and set, constructors, inheritance with virtual and override, and records for values."
native_way: "One kind of struct used as a value or through a pointer, plain fields instead of properties, NewX functions, and embedding without virtual dispatch."
sensei_tip: "A Go struct is a C# struct by default and becomes class-like when you take its address. The receiver type (T or *T) is where you make that choice."
---

| C# | Go |
| --- | --- |
| `class Account` (reference) | `*Account` |
| `struct Point` / `record struct` | `type Point struct{ X, Y int }` (value) |
| `public long Balance { get; private set; }` | unexported `balance` plus a `Balance()` method |
| `new Account(id)` | `NewAccount(id)` |
| `virtual` / `override` | no virtual methods; use interfaces |
| `class Dog : Animal` | `type Dog struct{ Animal }` |
| `override ToString()` | `String() string` |
| `[JsonPropertyName("id")]` | `` `json:"id"` `` |
| `int? age` | `age *int`, built with `new(30)` (1.26) |
