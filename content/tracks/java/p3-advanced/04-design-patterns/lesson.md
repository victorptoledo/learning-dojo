# Design Patterns in Modern Java

The classic design patterns were written for Java 1.2-era languages: no lambdas, no records,
no sealed types. Many patterns were workarounds for missing features. In modern Java some
shrink to a line, some change shape, and a few are still exactly as useful as before. This
lesson goes through the ones you will meet most, old form next to new.

## Strategy: a lambda

**Intent:** choose an algorithm at run time.

The classic form is an interface plus one class per algorithm. When the strategy is a
single method, the interface is a functional interface and each strategy is a lambda:

```java
// one class per strategy, before
interface ShippingCost { long cents(Order order); }
class FlatShipping implements ShippingCost { public long cents(Order o) { return 500; } }
class FreeOverFifty implements ShippingCost { ... }

// now: the strategies are values
Map<String, ToLongFunction<Order>> shipping = Map.of(
        "flat", order -> 500,
        "free-over-50", order -> order.subtotal() >= 5_000 ? 0 : 500,
        "weight", order -> 100 + order.grams() / 10);

long cost = shipping.get(customer.plan()).applyAsLong(order);
```

Keep a named class when a strategy has **state**, several methods or needs its own tests and
documentation. `Comparator` is the strategy pattern you already use every day.

## Command: a Runnable or a record

**Intent:** turn a request into an object you can queue, log or undo.

A command without data is a `Runnable` or `Supplier`. A command **with** data is a record,
and a family of commands is a sealed interface handled by one `switch`:

```java
sealed interface Command permits Deposit, Withdraw, Close {}
record Deposit(String account, long cents) implements Command {}
record Withdraw(String account, long cents) implements Command {}
record Close(String account) implements Command {}

void execute(Command command) {
    switch (command) {
        case Deposit(var account, var cents) -> ledger.add(account, cents);
        case Withdraw(var account, var cents) -> ledger.subtract(account, cents);
        case Close(var account) -> ledger.close(account);
    }
}
```

Records are easy to serialise, log and compare in tests, which is what you want from commands.

## Visitor: a switch over a sealed type

**Intent:** add operations to a fixed hierarchy without changing its classes.

The classic visitor needs an `accept` method in every class and a `visit` method per type in
every visitor, all to get double dispatch and compile-time completeness. A sealed interface
gives the same completeness with a pattern `switch`, and each new operation is just a method:

```java
sealed interface Expr permits Num, Add, Mul {}
record Num(double value) implements Expr {}
record Add(Expr left, Expr right) implements Expr {}
record Mul(Expr left, Expr right) implements Expr {}

static double eval(Expr e) {
    return switch (e) {
        case Num(var v) -> v;
        case Add(var l, var r) -> eval(l) + eval(r);
        case Mul(var l, var r) -> eval(l) * eval(r);
    };
}

static String show(Expr e) {
    return switch (e) {
        case Num(var v) -> Double.toString(v);
        case Add(var l, var r) -> "(" + show(l) + " + " + show(r) + ")";
        case Mul(var l, var r) -> show(l) + " * " + show(r);
    };
}
```

This is data-oriented programming: **data** in records and sealed types, **operations** in
functions over them.

## Creating objects: constructors, factories, builders

| Situation | Use |
| --- | --- |
| A few required values | a record and its constructor |
| Several ways to create, or a name says more | **static factory methods**: `Money.euros(500)`, `Duration.ofMinutes(5)`, `List.of(...)` |
| Many optional parameters | a **builder** |
| Choosing the implementation from input | a static factory that returns the interface (often a switch) |

Static factories have names, can return a cached or a subtype instance, and can validate.
A builder pays off when a constructor would have many parameters, several of them optional:

```java
var request = HttpRequest.newBuilder(URI.create("https://api.example.com/orders"))
        .timeout(Duration.ofSeconds(5))
        .header("Accept", "application/json")
        .GET()
        .build();
```

For your own records, a **wither** is often enough instead of a builder:
`order.withStatus(SHIPPED)` returns a copy with one component changed.

## Decorator: composing functions or wrapping interfaces

**Intent:** add behaviour around an object without changing it.

For a single-method interface, decorating is function composition:

```java
Function<String, String> clean = String::strip;
Function<String, String> pipeline = clean.andThen(String::toLowerCase).andThen(s -> s.replace(' ', '-'));
pipeline.apply("  Hello Dojo ");   // "hello-dojo"
```

For a real interface, the classic form still applies, and it is how logging, caching, retries
and metrics are usually added:

```java
final class CachingPrices implements PriceSource {
    private final PriceSource delegate;
    private final Map<String, Price> cache = new ConcurrentHashMap<>();

    CachingPrices(PriceSource delegate) { this.delegate = delegate; }

    @Override
    public Price price(String sku) {
        return cache.computeIfAbsent(sku, delegate::price);
    }
}
```

## Singleton, observer and template method

- **Singleton:** prefer a single instance created at startup and **passed in** (dependency
  injection, as Spring does). If you really need a global one, an `enum` with one constant is
  the safe form. Hidden globals make testing hard.
- **Observer:** a list of `Consumer<Event>` listeners covers most in-process cases.
- **Template method:** instead of an abstract class with hook methods, pass the varying step
  as a function: `retry(3, () -> client.call())`.

## Immutability is the pattern behind the patterns

Records, `List.copyOf`, withers and pure functions make code easier to share between threads,
cache, test and reason about. When you are unsure how to structure something, start
immutable and add mutation only where the design needs it.

## Summary

- Strategy and command become lambdas, records and sealed interfaces.
- Visitor becomes a pattern `switch` over a sealed hierarchy.
- Create with constructors, named static factories, builders for many optional values, and
  withers for copies.
- Decorate with function composition or a wrapping class; inject dependencies instead of
  singletons.
