---
from: java
status: draft
concept: "Creational patterns"
from_way: "Telescoping constructors, JavaBeans with setters for everything, `getInstance()` singletons and `clone()`."
native_way: "Records with compact constructors, static factories, builders for many optional parts, one instance per Spring container, and withers for copies."
sensei_tip: "Lombok's `@Builder` and `@With` generate what this lesson writes by hand; for records, a hand-written builder is short, and you keep validation in the compact constructor."
---

| Java | Pattern |
| --- | --- |
| `record` + compact constructor | value object with validation |
| `List.of`, `Duration.ofSeconds`, `Path.of` | static factory |
| `HttpClient.newBuilder()` | builder |
| `@Configuration` with `@Bean` methods | abstract factory, factory method |
| Spring singleton scope, `enum` | singleton |
| `Integer.valueOf`, enum constants | flyweight |
