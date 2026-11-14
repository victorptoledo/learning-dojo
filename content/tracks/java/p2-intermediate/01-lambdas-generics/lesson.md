# Lambdas, Generics and Functional Interfaces

Two features let you write code once and reuse it for many cases: **lambdas** pass
*behaviour* as a value, and **generics** let one class or method work for many *types*
while the compiler keeps checking them. Streams, `Optional`, `Comparator` and most modern
APIs are built on both.

## Functional interfaces and lambdas

A **functional interface** has exactly one abstract method. A **lambda** is a short way to
write an implementation of it:

```java
Comparator<String> byLength = (a, b) -> Integer.compare(a.length(), b.length());
Runnable hello = () -> System.out.println("hello");
```

The compiler infers the parameter types from the target interface. A lambda body is either
an expression (`x -> x * 2`) or a block with `return` (`x -> { log(x); return x * 2; }`).

You rarely need to declare your own interface: `java.util.function` has the common shapes.

| Interface | Method | Shape | Example |
| --- | --- | --- | --- |
| `Supplier<T>` | `T get()` | () → T | `() -> new ArrayList<>()` |
| `Consumer<T>` | `void accept(T)` | T → nothing | `s -> System.out.println(s)` |
| `Function<T, R>` | `R apply(T)` | T → R | `String::length` |
| `Predicate<T>` | `boolean test(T)` | T → boolean | `String::isBlank` |
| `BiFunction<T, U, R>` | `R apply(T, U)` | (T, U) → R | `(a, b) -> a + b` |
| `UnaryOperator<T>` | `T apply(T)` | T → T | `String::strip` |
| `BinaryOperator<T>` | `T apply(T, T)` | (T, T) → T | `Integer::sum` |

Primitive versions avoid boxing: `IntPredicate`, `ToIntFunction<T>`, `IntUnaryOperator`...

Annotate your own single-method interfaces with `@FunctionalInterface`: the compiler then
rejects a second abstract method.

### Method references

When a lambda only calls one existing method, a **method reference** says the same thing
shorter:

| Kind | Reference | Equivalent lambda |
| --- | --- | --- |
| Static method | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| Method of the argument | `String::toUpperCase` | `s -> s.toUpperCase()` |
| Method of a given object | `greeting::formatted` | `x -> greeting.formatted(x)` |
| Constructor | `ArrayList::new` | `() -> new ArrayList<>()` |

### Capturing variables

A lambda can read local variables from the enclosing method only if they are **effectively
final** (never reassigned):

```java
int minimum = 3;
words.removeIf(w -> w.length() < minimum);   // fine
// minimum++;  // would make the lambda above a compile error
```

To count inside a lambda, use the return value (a stream's `count()`), an `AtomicInteger`, or
a plain loop.

### Composing functions

```java
Predicate<String> blank = String::isBlank;
Predicate<String> keep = blank.negate().and(s -> s.length() <= 20);

Function<String, String> clean = String::strip;
Function<String, Integer> cleanLength = clean.andThen(String::length);

Comparator<Person> order = Comparator.comparing(Person::city)
        .thenComparing(Person::name, Comparator.reverseOrder());
```

## Generics

Without generics, a `List` held `Object` and every read needed a cast that could fail at run
time. With generics the type is part of the declaration and the compiler checks it:

```java
List<String> names = new ArrayList<>();   // <> (diamond) infers <String>
names.add("Ada");
String first = names.getFirst();          // no cast
// names.add(42);                         // compile error
```

### Generic classes and records

```java
public record Pair<A, B>(A first, B second) {
    public <C> Pair<A, C> withSecond(C value) {
        return new Pair<>(first, value);
    }
}

var p = new Pair<>("ada", 36);   // Pair<String, Integer>
```

Type parameters are conventionally one capital letter: `T` (type), `E` (element), `K`/`V`
(key/value), `R` (result).

### Generic methods

A method can declare its own type parameter before the return type:

```java
static <T> T firstOrDefault(List<T> list, T fallback) {
    return list.isEmpty() ? fallback : list.getFirst();
}

String s = firstOrDefault(List.of("a"), "none");   // T inferred as String
```

### Bounded type parameters

`<T extends Something>` restricts `T` and lets you call `Something`'s methods:

```java
static <T extends Comparable<T>> T max(List<T> items) {
    T best = items.getFirst();
    for (T item : items) {
        if (item.compareTo(best) > 0) best = item;
    }
    return best;
}
```

### Wildcards: extends to read, super to write

`List<Integer>` is **not** a `List<Number>`, even though `Integer` is a `Number`. If it were,
you could add a `Double` to a list of integers. Wildcards express the safe variations:

```java
static double sum(List<? extends Number> numbers) {     // read Numbers from it
    double total = 0;
    for (Number n : numbers) total += n.doubleValue();
    return total;
}

static void fillWithZeros(List<? super Integer> target, int count) {  // write Integers into it
    for (int i = 0; i < count; i++) target.add(0);
}
```

The rule of thumb is **PECS**: *Producer Extends, Consumer Super*. A parameter you only read
from is `? extends T`; one you only write into is `? super T`; one you do both with is plain
`T`. That is why `Collections.copy` is declared
`copy(List<? super T> dest, List<? extends T> src)`.

### Type erasure, briefly

Generic types exist for the compiler; at run time `List<String>` and `List<Integer>` are both
just `List`. Consequences you will meet: no `new T()`, no `instanceof List<String>`, and no
overloading `m(List<String>)` with `m(List<Integer>)`. When you need the type at run time,
pass a `Class<T>` or a `Supplier<T>`.

## Summary

- A lambda implements a functional interface; `java.util.function` has `Supplier`,
  `Consumer`, `Function`, `Predicate` and their friends.
- Method references (`String::strip`, `ArrayList::new`) replace one-call lambdas.
- Captured locals must be effectively final.
- Generic classes, records and methods keep types checked without casts; `<T extends X>`
  bounds them.
- Wildcards: `? extends T` to read (producer), `? super T` to write (consumer).
