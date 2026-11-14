# Streams and Collectors

A **stream** describes a computation over a sequence of values: where they come from, what
happens to each, and how they are combined at the end. Instead of telling the computer *how*
to loop, you say *what* you want:

```java
// the five most frequent words longer than three letters
List<String> top = words.stream()
        .filter(w -> w.length() > 3)
        .collect(Collectors.groupingBy(w -> w, Collectors.counting()))
        .entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(5)
        .map(Map.Entry::getKey)
        .toList();
```

## The shape of a pipeline

1. A **source**: `list.stream()`, `Stream.of(a, b)`, `Arrays.stream(array)`,
   `IntStream.range(0, 10)`, `Files.lines(path)`, `text.lines()`.
2. Zero or more **intermediate operations**, each returning a new stream: `filter`, `map`,
   `flatMap`, `distinct`, `sorted`, `limit`, `skip`, `peek`, `takeWhile`, `dropWhile`.
3. One **terminal operation** that produces the result: `toList`, `collect`, `count`,
   `forEach`, `reduce`, `min`/`max`, `findFirst`, `anyMatch`/`allMatch`/`noneMatch`, `sum`.

Streams are **lazy**: nothing runs until the terminal operation, and then each element flows
through the whole pipeline. That is why `limit(5)` on an infinite stream works, and why
`filter(...).findFirst()` stops at the first match. A stream can be consumed **only once**.

## The everyday operations

```java
record Sale(String region, String product, int units, long cents) {}

List<Sale> sales = ...;

long northUnits = sales.stream()
        .filter(s -> s.region().equals("north"))
        .mapToLong(Sale::units)          // a LongStream: sum, average, max without boxing
        .sum();

List<String> products = sales.stream()
        .map(Sale::product)
        .distinct()
        .sorted()
        .toList();                        // Java 16: an unmodifiable list

boolean anyBig = sales.stream().anyMatch(s -> s.cents() > 100_000);

Optional<Sale> biggest = sales.stream().max(Comparator.comparingLong(Sale::cents));
```

`flatMap` turns each element into a stream and joins them:

```java
List<String> words = lines.stream()
        .flatMap(line -> Arrays.stream(line.split("\\s+")))
        .filter(w -> !w.isEmpty())
        .toList();
```

> [!NOTE]
> `toList()` (Java 16) returns an unmodifiable list. `collect(Collectors.toList())` returns a
> list whose mutability is unspecified (an `ArrayList` in practice). Prefer `toList()`.

## Collectors: grouping and summarising

`collect` with a `Collector` builds anything: maps, strings, statistics.

```java
// region -> list of sales
Map<String, List<Sale>> byRegion = sales.stream()
        .collect(Collectors.groupingBy(Sale::region));

// region -> total cents, regions sorted
Map<String, Long> centsByRegion = sales.stream()
        .collect(Collectors.groupingBy(Sale::region, TreeMap::new,
                Collectors.summingLong(Sale::cents)));

// product -> number of sales
Map<String, Long> countByProduct = sales.stream()
        .collect(Collectors.groupingBy(Sale::product, Collectors.counting()));

// split in two by a condition
Map<Boolean, List<Sale>> bigOrSmall = sales.stream()
        .collect(Collectors.partitioningBy(s -> s.cents() > 10_000));

// one key per element (throws on duplicate keys unless you give a merge function)
Map<String, Long> centsByProduct = sales.stream()
        .collect(Collectors.toMap(Sale::product, Sale::cents, Long::sum));

// a String
String csv = products.stream().collect(Collectors.joining(", ", "[", "]"));
```

The second argument of `groupingBy` is a **downstream collector** applied to each group:
`counting()`, `summingLong(...)`, `averagingDouble(...)`, `mapping(f, toList())`,
`maxBy(comparator)`, `toSet()`. The optional middle argument chooses the map
(`TreeMap::new` for sorted keys).

`summarizingLong` gives count, sum, min, average and max in one pass:

```java
LongSummaryStatistics stats = sales.stream().mapToLong(Sale::cents).summaryStatistics();
stats.getAverage();
```

## reduce

`reduce` folds the elements into one value:

```java
long total = sales.stream().map(Sale::cents).reduce(0L, Long::sum);
```

Most reductions have a dedicated method (`sum`, `count`, `max`, `joining`) that is clearer.
Reach for `reduce` when none fits.

## Gatherers (Java 24)

For custom intermediate operations, `Stream.gather` takes a `Gatherer`. The built-in ones in
`java.util.stream.Gatherers` cover common needs:

```java
Stream.of(1, 2, 3, 4, 5).gather(Gatherers.windowFixed(2)).toList();   // [[1, 2], [3, 4], [5]]
Stream.of(1, 2, 3, 4).gather(Gatherers.windowSliding(2)).toList();    // [[1, 2], [2, 3], [3, 4]]
```

## When a loop is better

Streams shine for filter-map-collect. A loop is clearer when you:

- need to **stop early with a complex condition** or throw a checked exception,
- update **several results** at once in one pass,
- need the **index** and the previous element,
- would write a lambda with side effects (`forEach(x -> list.add(...))`): that is a loop in
  disguise, and `collect` or `toList` is the stream way.

> [!WARNING]
> Never modify the stream's source or shared state from inside a lambda. And do not use
> `parallel()` by default: it only helps for large, CPU-heavy work, and it makes any shared
> state a bug.

## Summary

- Source → intermediate operations (lazy) → one terminal operation; a stream is used once.
- `filter`, `map`, `flatMap`, `distinct`, `sorted`, `limit` and `toList` cover most work.
- `Collectors.groupingBy(key, mapFactory, downstream)` groups and summarises; `toMap` needs a
  merge function for duplicate keys; `joining` builds strings.
- Use primitive streams (`mapToLong`, `sum`) to avoid boxing.
- Prefer a loop when the stream would need side effects or early exits.
