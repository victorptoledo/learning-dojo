# Creating Objects

Creational patterns decide **who** creates an object, **how**, and **how many**. Java has
gained features that make some of them trivial, but the questions remain.

## Constructors, static factories and records

Start simple. A **record** gives an immutable value type with a canonical constructor;
validate in the **compact constructor**, so an invalid instance can never exist:

```java
public record Money(long cents, String currency) {
    public Money {
        if (cents < 0) throw new IllegalArgumentException("negative amount");
        Objects.requireNonNull(currency);
    }
    public static Money euros(long cents) { return new Money(cents, "EUR"); }
}
```

A **static factory method** (`Money.euros(...)`, `List.of`, `Duration.ofSeconds`) beats a
constructor when you want a name that says what it creates, to return a cached instance or a
hidden subtype, or to offer several "constructors" with the same parameter types.

## Builder

**Problem.** A type has many optional parts. Telescoping constructors
(`new Request(url)`, `new Request(url, method)`, `new Request(url, method, null, null, 30)`)
are unreadable, and positional booleans get swapped silently.

**Solution.** A builder collects named parts and checks them once in `build()`:

```java
var request = QuoteRequest.builder()
        .customer("ana").product("desk").quantity(12)
        .vip(true)
        .build();
```

Put the validation in the record's compact constructor and let `build()` call it; then even
code that bypasses the builder cannot create a bad instance. JDK and Spring builders:
`HttpClient.newBuilder()`, `HttpRequest.newBuilder()`, `Stream.builder()`, `StringBuilder`,
`Locale.Builder`, Spring's `RestClient.builder()`, `UriComponentsBuilder`,
`MockMvcRequestBuilders`.

Do not add a builder to a record with three required fields: the constructor *is* named
parameters in a good IDE. Builders pay off with many optional parts or step-by-step assembly.

## Abstract factory

**Problem.** You create a **family** of related objects that must match (a Postgres
connection, dialect and migrator; a light theme's buttons and dialogs), and the caller should
not pick each one separately.

**Solution.** One interface with a creation method per product; one implementation per family.

```java
interface StorageKit {
    Accounts accounts();
    Transactions transactions();
}
```

In Spring applications the **container** plays this role most of the time: a `@Configuration`
class (or a profile) is a family of `@Bean` factory methods. JDK examples:
`DocumentBuilderFactory`, `javax.xml.transform.TransformerFactory`; `ServiceLoader` finds
implementations of a factory interface on the module path.

## Singleton

**Problem.** Exactly one instance should exist (a configuration, a connection pool, a cache).

**The classic solution** (private constructor, `static getInstance()`) is now mostly an
**anti-pattern**: it is global mutable state, hides dependencies (nothing in a constructor
tells you the class uses it), and makes tests share state.

**The modern answer:** create one instance and **inject** it. In Spring, beans are singletons
by default: one instance per container, passed to whoever needs it, replaceable in tests. When
you truly need a JVM-wide singleton without a container, use an `enum` with one constant
(serialisation-safe and thread-safe) or a holder class for laziness:

```java
enum Registry { INSTANCE; /* state and methods */ }
```

`Runtime.getRuntime()` and `Desktop.getDesktop()` are JDK singletons; note that both
represent something there really is one of.

## Prototype

**Problem.** Create a new object by copying an existing one, perhaps with changes.

**Java's `Cloneable` is broken** (no `clone()` in the interface, shallow copies, checked
exceptions); avoid it. With immutable types, a "prototype" is just a copy with changes:

```java
record Settings(Duration timeout, int retries) {
    Settings withRetries(int retries) { return new Settings(timeout, retries); }
}
var aggressive = defaults.withRetries(5);
```

Copy constructors and `toBuilder()` do the same for classes. A JEP for derived record
creation (`with` expressions on records, JEP 468) has been proposed; until it ships, write
withers by hand.

## Flyweight (and object pools)

**Problem.** Very many objects share most of their state. **Solution:** share immutable
instances for the common part. `Integer.valueOf(-128..127)`, `Boolean.TRUE`, interned
strings and `enum` constants are flyweights. Records make sharing safe because they cannot
change. Do not build object pools for ordinary objects: allocation is cheap on the JVM;
pool only expensive resources (connections, threads).

## Choosing

| Situation | Use |
| --- | --- |
| A value with a few required parts | record (+ compact constructor) |
| Several ways to create, or cached instances | static factory methods |
| Many optional parts | builder (validating in the record) |
| Matching families of objects | abstract factory, or the DI container's configuration |
| One instance shared | one bean injected; `enum` if no container |
| Variants of an existing object | withers, `toBuilder()`, copy constructors |

## Further reading

- Refactoring.Guru, "Builder": <https://refactoring.guru/design-patterns/builder>
- Refactoring.Guru, "Abstract Factory": <https://refactoring.guru/design-patterns/abstract-factory>
- Refactoring.Guru, "Singleton" (and its criticism): <https://refactoring.guru/design-patterns/singleton>
- Refactoring.Guru, "Prototype": <https://refactoring.guru/design-patterns/prototype>
- Refactoring.Guru, "Flyweight": <https://refactoring.guru/design-patterns/flyweight>
- JEP 395, "Records": <https://openjdk.org/jeps/395>
- Spring Framework reference, "Bean Scopes" (singleton and prototype beans): <https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html>
- Java SE 25 API, `ServiceLoader`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ServiceLoader.html>
