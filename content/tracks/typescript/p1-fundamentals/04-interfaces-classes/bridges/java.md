---
from: java
status: draft
concept: "Interfaces and classes without nominal types"
from_way: "Interfaces are contracts a class must declare; private is enforced by the JVM; overloads are separate methods with separate bodies."
native_way: "Interfaces are shapes anything can match; TypeScript's private is compile-time only (use #private for runtime); overloads are several signatures over one body."
sensei_tip: "Write fewer classes than you would in Java: plain objects plus interfaces cover most data. Keep classes for stateful services, and use #fields when privacy matters."
---

| Java | TypeScript |
| --- | --- |
| `interface Greeter { String greet(); }` | `interface Greeter { greet(): string }` |
| `class A implements Greeter` | `class A implements Greeter` (optional, checks only) |
| `private String token;` | `#token: string` (runtime) or `private token` (compile time) |
| `@Override` | `override` keyword |
| Overloads: two methods, two bodies | Two signatures, one body |
| Constructor with `this.x = x` boilerplate | Declare fields and assign them (no parameter properties with type stripping) |
