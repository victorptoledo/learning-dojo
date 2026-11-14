# Modern Syntax: var, Text Blocks and switch

Java 8 code is still valid Java 25, but it is no longer how Java is written. This lesson
covers four small features that change almost every method you write: `var`, text blocks,
`switch` expressions and pattern matching for `instanceof`.

## var: let the compiler write the type

```java
var names = new ArrayList<String>();          // ArrayList<String>
var counts = new HashMap<String, Integer>();  // HashMap<String, Integer>
for (var entry : counts.entrySet()) { ... }   // Map.Entry<String, Integer>
```

`var` is **local type inference** (Java 10). The variable still has a static type, fixed at
compile time; you just do not repeat it. It is not JavaScript's `var`: you cannot assign a
`String` to it later if it was inferred as `int`.

Where `var` works and where it does not:

| Allowed | Not allowed |
| --- | --- |
| Local variables with an initializer | Fields, parameters, return types |
| `for` and enhanced `for` variables | `var x;` without an initializer |
| `try (var in = ...)` resources | `var x = null;` (no type to infer) |
| Lambda parameters: `(var a, var b) -> ...` | `var list = {1, 2};` (array initializer) |

> [!TIP]
> Use `var` when the type is obvious from the right-hand side (`new`, a factory, a cast).
> Keep the explicit type when the right side does not say what you get:
> `var result = service.process(order);` hides whether `result` is a `Receipt` or an
> `Optional<Receipt>`.

One trap: `var` infers the **concrete** type. `var list = new ArrayList<String>();` is an
`ArrayList<String>`, not a `List<String>`. Inside a method that rarely matters.

## Text blocks: multi-line strings

Before Java 15, a JSON snippet in a test looked like this:

```java
String json = "{\n" +
              "  \"name\": \"Ada\",\n" +
              "  \"age\": 36\n" +
              "}\n";
```

With a **text block** it looks like the JSON itself:

```java
String json = """
        {
          "name": "Ada",
          "age": 36
        }
        """;
```

Rules worth knowing:

- It starts with `"""` followed by a **line break**; the content begins on the next line.
- The **common indentation is removed**. The closing `"""` counts too: put it at the left
  margin of the content to keep no indentation, or further left to keep some.
- Quotes need no escaping. A line ending in `\` joins the next line (no newline);
  `\s` keeps a trailing space.
- A text block is an ordinary `String`, so `.formatted(...)` works on it:

```java
String greeting = """
        Hello, %s!
        You have %d new katas.
        """.formatted(name, count);
```

## switch expressions

The old `switch` is a statement with fall-through: forget a `break` and the next case runs.
Since Java 14 `switch` can be an **expression** that produces a value, with arrow cases that
never fall through:

```java
int days = switch (month) {
    case FEBRUARY -> leapYear ? 29 : 28;
    case APRIL, JUNE, SEPTEMBER, NOVEMBER -> 30;
    default -> 31;
};
```

- `case A, B ->` lists several labels; no fall-through, no `break`.
- A switch **expression** must be **exhaustive**: every possible value needs a case. For an
  enum that lists every constant, `default` is not needed, and the compiler warns you when a
  new constant is added.
- When a case needs several statements, use a block and `yield` the value:

```java
String label = switch (status) {
    case ACTIVE -> "active";
    case SUSPENDED -> {
        log.warn("suspended account {}", id);
        yield "suspended";
    }
    case CLOSED -> "closed";
};
```

`switch` also works on `String` and on `int`. Since Java 21 it works on any type with
patterns (next lessons), and `case null` is allowed.

## Pattern matching for instanceof

The Java 8 way tests and then casts:

```java
if (shape instanceof Circle) {
    Circle circle = (Circle) shape;
    return Math.PI * circle.radius() * circle.radius();
}
```

Since Java 16 the test **binds a variable** when it matches:

```java
if (shape instanceof Circle circle) {
    return Math.PI * circle.radius() * circle.radius();
}
```

The binding is in scope only where the match is certain, so it also works with `&&`:

```java
if (obj instanceof String text && !text.isBlank()) {
    return text.strip();
}
```

and with an early return:

```java
if (!(obj instanceof Integer number)) {
    return 0;
}
return number * 2;   // number is in scope here
```

## Small things you will see everywhere

| Feature | Example |
| --- | --- |
| Unnamed variable `_` (Java 22) | `catch (NumberFormatException _) { ... }`, `for (var _ : list) count++;` |
| Collection factories (Java 9) | `List.of(1, 2)`, `Set.of("a")`, `Map.of("k", 1)` |
| String helpers (Java 11+) | `strip`, `isBlank`, `lines()`, `repeat(3)`, `formatted(...)` |
| Numeric literals | `1_000_000`, `0b1010`, `0xFF` |

## Summary

- `var` infers a local variable's static type; use it when the type is obvious.
- Text blocks (`"""`) keep multi-line text readable and strip the common indentation.
- `switch` expressions use `->`, never fall through, must be exhaustive and `yield` from
  blocks.
- `obj instanceof Type name` tests and casts in one step.
