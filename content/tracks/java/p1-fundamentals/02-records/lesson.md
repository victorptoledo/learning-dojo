# Records: Immutable Data in One Line

A large part of Java 8 code is value classes: private final fields, a constructor, getters,
`equals`, `hashCode` and `toString`. Fifty lines to say "a point has an x and a y". Lombok
became popular just to hide that. Since Java 16 the language has its own answer: **records**.

## Declaring a record

```java
public record Point(int x, int y) {}
```

That one line declares a final class with:

| Generated | What it does |
| --- | --- |
| `private final int x, y` | One field per **component** |
| `Point(int x, int y)` | The **canonical constructor** |
| `x()` and `y()` | Accessors. Note: `x()`, not `getX()` |
| `equals` / `hashCode` | Based on every component: two points with the same x and y are equal |
| `toString` | `Point[x=1, y=2]` |

```java
var a = new Point(1, 2);
var b = new Point(1, 2);
a.equals(b);      // true
a == b;           // false: two objects
a.x();            // 1
a;                // Point[x=1, y=2]
```

A record is **transparent**: its state is exactly its components. That is why the compiler
can write `equals`, `hashCode` and `toString` for you, and why records are the natural type
for data that travels: DTOs, events, map keys, results of a calculation.

## What records cannot do

- **No setters, no extra instance fields.** Fields are `final`; a record cannot declare
  other instance fields. (Static fields and methods are fine.)
- **No inheritance from a class.** A record is implicitly `final` and extends
  `java.lang.Record`. It **can implement interfaces**, which is how you will combine it with
  sealed interfaces in the next lesson.

Everything else is allowed: methods, static factories, nested records, generics.

```java
public record Money(long cents, String currency) {
    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(cents + other.cents, currency);   // a new value, never a change
    }

    public static Money euros(long cents) {
        return new Money(cents, "EUR");
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
```

"Changing" a record means building a new one. Methods like `plus` or `withName(...)` return
a copy with one value different.

## Compact constructors: validate once

A record should never exist in an invalid state. Put the checks in a **compact
constructor**, a constructor without the parameter list:

```java
public record Email(String value) {
    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        value = value.strip().toLowerCase();   // normalise before the fields are assigned
    }
}
```

The parameters are implicit, and the fields are assigned **after** the body runs, with the
values the parameters have at the end. So you can validate and normalise in the same place.
You cannot assign `this.value` yourself in a compact constructor.

> [!TIP]
> Validate in the compact constructor and every `Email` in your program is valid by
> construction. No other code needs to check it again.

You can also add other constructors, as long as they call the canonical one with `this(...)`:

```java
public record Range(int from, int to) {
    public Range {
        if (from > to) throw new IllegalArgumentException(from + " > " + to);
    }

    public Range(int single) {
        this(single, single);
    }
}
```

## Immutable means all the way down

A record's fields are final, but a final field can point at a mutable object:

```java
public record Team(String name, List<String> members) {}

var list = new ArrayList<>(List.of("Ada"));
var team = new Team("core", list);
list.add("Linus");
team.members();   // [Ada, Linus]: the record changed behind your back
```

Make a **defensive copy** in the compact constructor. `List.copyOf` returns an unmodifiable
list (and returns the same list when it is already unmodifiable):

```java
public record Team(String name, List<String> members) {
    public Team {
        members = List.copyOf(members);
    }
}
```

Now `team.members().add(...)` throws `UnsupportedOperationException`, and changes to the
original list are invisible. The same goes for `Set.copyOf` and `Map.copyOf`.

## Records as local types and keys

A record can be declared **inside a method**, which is handy for intermediate results:

```java
record Score(String player, int points) {}
var best = scores.stream().max(Comparator.comparingInt(Score::points));
```

Because `equals` and `hashCode` come for free, records are good **map keys** and **set
elements**:

```java
record Cell(int row, int column) {}
Map<Cell, String> board = new HashMap<>();
board.put(new Cell(0, 0), "X");
board.get(new Cell(0, 0));   // "X": a new but equal key
```

## Records versus classes, versus enums

| Use | When |
| --- | --- |
| **record** | The type is its data: values, DTOs, events, keys, results |
| **enum** | A fixed set of named constants: `Status.ACTIVE`, `Planet.EARTH` |
| **class** | The object has identity or changing state behind behaviour: a connection, a cache, a service |

## Summary

- `record Point(int x, int y) {}` generates fields, constructor, accessors `x()`,
  `equals`, `hashCode` and `toString`.
- Records are final, their fields are final, and they can implement interfaces.
- Validate and normalise in the **compact constructor**: `public Point { ... }`.
- Copy mutable components with `List.copyOf` and friends so the record stays immutable.
