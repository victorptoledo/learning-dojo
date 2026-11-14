# Sealed Types and Pattern Matching

Records model one kind of value. Real domains have **alternatives**: a payment is a card, a
bank transfer *or* a voucher; a shape is a circle, a rectangle *or* a triangle. Java 8 had
two tools for that, both weak: an abstract class with `instanceof` chains (the compiler
cannot tell you a case is missing) or the visitor pattern (correct, but heavy). Java 21 has
a better pair: **sealed types** to close the set of alternatives, and **pattern matching** in
`switch` to handle each one.

## Sealed interfaces

```java
public sealed interface Shape permits Circle, Rectangle, Triangle {}

public record Circle(double radius) implements Shape {}
public record Rectangle(double width, double height) implements Shape {}
public record Triangle(double a, double b, double c) implements Shape {}
```

`sealed ... permits` lists **every** type allowed to implement `Shape`. Any other class that
tries gets a compile error. If all the subtypes are in the same file, you can leave
`permits` out and the compiler infers it.

Each permitted subtype must say how the hierarchy continues:

| Modifier | Meaning |
| --- | --- |
| `final` | No subtypes. Records are implicitly final. |
| `sealed` | Its own closed list of subtypes. |
| `non-sealed` | Open again: anyone may extend it. |

Sealed classes work too (`sealed abstract class Shape permits ...`), but a sealed
**interface** with **record** subtypes is the common, simplest form. Together they are what
other languages call an *algebraic data type* or *sum type*.

## switch on types

Since Java 21 a `switch` can match on the **type** of a value:

```java
static double area(Shape shape) {
    return switch (shape) {
        case Circle c -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
        case Triangle t -> heron(t.a(), t.b(), t.c());
    };
}
```

There is **no `default`**, and that is the point. Because `Shape` is sealed, the compiler
knows these three cases cover everything. Add a `Square` to `permits` tomorrow and every
`switch` that forgot it **stops compiling**, which shows you exactly what to update. A
`default` branch would silently swallow the new case.

> [!IMPORTANT]
> With sealed types, avoid `default` in switches over the hierarchy. Exhaustiveness is the
> compiler checking your logic for free.

## Record patterns: deconstruct in the case

A **record pattern** matches the type and takes the components apart at the same time:

```java
static double area(Shape shape) {
    return switch (shape) {
        case Circle(double r) -> Math.PI * r * r;
        case Rectangle(double w, double h) -> w * h;
        case Triangle(double a, double b, double c) -> heron(a, b, c);
    };
}
```

You can use `var` for the component types and nest patterns:

```java
record Line(Point from, Point to) {}

case Line(Point(var x1, var y1), Point(var x2, var y2)) -> Math.hypot(x2 - x1, y2 - y1);
```

Components you do not need can be ignored with `_` (Java 22): `case Rectangle(var w, _) -> w;`.

## Guards with when

A **guard** adds a condition to a case:

```java
static String describe(Shape shape) {
    return switch (shape) {
        case Circle(double r) when r == 0 -> "a point";
        case Circle c -> "a circle";
        case Rectangle(double w, double h) when w == h -> "a square";
        case Rectangle r -> "a rectangle";
        case Triangle t -> "a triangle";
    };
}
```

Cases are tried **top to bottom**, so the guarded case must come before the general one.
The compiler rejects a case that can never match because an earlier one already covers it
(*dominance*): `case Circle c` followed by `case Circle(double r) when r == 0` is an error.

A guarded case does not count for exhaustiveness: `case Circle c when c.radius() > 0` alone
does not cover `Circle`, so you still need an unguarded `case Circle c`.

## null and other types

- Without a `case null`, a switch on a `null` value throws `NullPointerException`, as it
  always did. You can handle it explicitly: `case null -> "nothing";`.
- Pattern switches work on any reference type, not only sealed ones: `switch (obj)` with
  `case String s`, `case Integer i`, ... For a non-sealed type you then need a `default`.

## Sealed types beyond shapes

The same idea models results and commands cleanly:

```java
sealed interface Payment permits Card, Transfer, Voucher {}
record Card(String number, YearMonth expiry) implements Payment {}
record Transfer(String iban) implements Payment {}
record Voucher(String code, long cents) implements Payment {}

long fee(Payment payment, long amount) {
    return switch (payment) {
        case Card c -> amount * 2 / 100;
        case Transfer t -> 50;
        case Voucher(var code, var cents) when cents >= amount -> 0;
        case Voucher v -> 25;
    };
}
```

Compare with the classic object-oriented alternative, a `fee()` method in every subtype.
Both are valid. Put behaviour **in the types** when each subtype owns its rule and new
subtypes are frequent. Put it **in a switch** when you have many operations over a stable
set of types, or the logic needs context the types should not know (here, `amount`).

## Summary

- `sealed interface X permits A, B, C` closes a hierarchy; subtypes are `final`, `sealed`
  or `non-sealed` (records are final).
- `switch` matches types (`case Circle c`) and deconstructs records
  (`case Circle(double r)`), with `var`, nesting and `_`.
- `when` adds guards; order matters and dominated cases are compile errors.
- Over a sealed type, an exhaustive switch needs no `default`, and should not have one.
