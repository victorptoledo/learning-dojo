# Collections: List, Set, Map and Friends

The collection interfaces you know from Java 8 are all still there: `List`, `Set`, `Map`,
`ArrayList`, `HashMap`. What changed is how you **create** them, the **methods** that
replace common loops, and a new family of interfaces for collections with an order.

## Creating collections

| You want | Write |
| --- | --- |
| A fixed list, set or map | `List.of(1, 2, 3)`, `Set.of("a", "b")`, `Map.of("ada", 36, "linus", 29)` |
| A map with more than 10 entries | `Map.ofEntries(Map.entry("a", 1), Map.entry("b", 2), ...)` |
| An unmodifiable copy of another collection | `List.copyOf(list)`, `Set.copyOf(set)`, `Map.copyOf(map)` |
| A list you will change | `new ArrayList<>()`, or `new ArrayList<>(List.of(...))` to start with values |

The `of` and `copyOf` collections are **unmodifiable**: `add`, `remove` and `put` throw
`UnsupportedOperationException`. They also **reject `null`** (elements, keys and values),
and `Set.of` and `Map.of` throw if you pass a duplicate. `Set.of` and `Map.of` have **no
defined iteration order**, and the order can change between runs.

> [!WARNING]
> `Arrays.asList(...)` is not the same as `List.of(...)`. It is a fixed-size **view** of the
> array: `set` works and writes through to the array, `add` throws, and `null` is allowed.
> Prefer `List.of` unless you really want the view.

## Choosing an implementation

| Need | Use |
| --- | --- |
| A growable list, index access | `ArrayList` |
| A queue or stack | `ArrayDeque` (not `Stack`, not `LinkedList`) |
| Unique elements, fast lookup | `HashSet` |
| Unique, in insertion order | `LinkedHashSet` |
| Unique, sorted | `TreeSet` (or any `Comparator`) |
| Key-value, fast lookup | `HashMap` |
| Key-value, insertion order | `LinkedHashMap` |
| Key-value, sorted by key | `TreeMap` |

Declare variables and parameters with the **interface** (`List<String>`, `Map<K, V>`) and
choose the implementation only where you create the object.

## Map methods that replace loops

Counting words the Java 7 way:

```java
Integer current = counts.get(word);
if (current == null) {
    counts.put(word, 1);
} else {
    counts.put(word, current + 1);
}
```

Since Java 8 the `Map` interface has methods for exactly these patterns:

```java
counts.merge(word, 1, Integer::sum);                               // count
byLength.computeIfAbsent(word.length(), k -> new ArrayList<>()).add(word); // group
int seen = counts.getOrDefault("dojo", 0);                         // read with a default
counts.putIfAbsent("dojo", 0);                                     // insert only if missing
counts.computeIfPresent("dojo", (k, v) -> v > 1 ? v - 1 : null);   // update; null removes
counts.replaceAll((k, v) -> v * 2);                                // update every value
```

- `merge(key, value, f)`: puts `value` if the key is absent, otherwise stores
  `f(old, value)`. Returning `null` from `f` removes the entry.
- `computeIfAbsent(key, k -> ...)`: returns the existing value, or creates, stores and
  returns a new one. Perfect for a `Map<K, List<V>>`.
- `forEach((k, v) -> ...)` loops over entries without `entrySet()`.

> [!TIP]
> `computeIfAbsent` returns the list, so `.add(word)` chains on it. That is the whole group-by
> in one line.

## Iterating safely

Removing from a collection while looping over it with for-each throws
`ConcurrentModificationException`. Use `removeIf`:

```java
names.removeIf(String::isBlank);
map.entrySet().removeIf(entry -> entry.getValue() == 0);
```

## Sorting

```java
List<String> sorted = new ArrayList<>(names);
sorted.sort(Comparator.naturalOrder());

people.sort(Comparator.comparing(Person::lastName)
        .thenComparing(Person::firstName)
        .thenComparingInt(Person::age).reversed());
```

`List.sort` sorts **in place** (the list must be modifiable). `Comparator.comparing` builds a
comparator from a key, `thenComparing` breaks ties, `reversed()` flips the whole order.
`Map.Entry.comparingByValue()` and `comparingByKey()` sort entries.

## Sequenced collections (Java 21)

Getting the last element of a list used to be `list.get(list.size() - 1)`, and of a
`LinkedHashSet` you had to iterate to the end. Java 21 added interfaces for collections that
have a defined order: `SequencedCollection` (implemented by `List`, `Deque`,
`LinkedHashSet`, `SortedSet`) and `SequencedMap` (`LinkedHashMap`, `SortedMap`).

```java
List<String> belts = List.of("white", "yellow", "green");
belts.getFirst();      // "white"
belts.getLast();       // "green"
belts.reversed();      // [green, yellow, white]: a reversed view

var scores = new LinkedHashMap<String, Integer>();
scores.put("ada", 90);
scores.put("linus", 80);
scores.firstEntry();   // ada=90
scores.pollLastEntry();// removes and returns linus=80
```

`addFirst`, `addLast`, `removeFirst` and `removeLast` complete the set. On unmodifiable
collections the mutating ones throw, as usual.

## Returning collections from your API

When a method returns a collection that callers must not change, return an unmodifiable one:

```java
public List<String> members() {
    return List.copyOf(members);                 // a snapshot
    // or Collections.unmodifiableList(members) // a live read-only view
}
```

Return an **empty collection**, never `null`: `List.of()`, `Map.of()`.

## Summary

- `List.of`, `Set.of`, `Map.of` and `copyOf` create unmodifiable collections that reject
  `null`; `Set.of` and `Map.of` have no stable order.
- Choose the implementation by need: `ArrayList`, `ArrayDeque`, `HashMap`, `LinkedHashMap`,
  `TreeMap`, ...
- `merge`, `computeIfAbsent`, `getOrDefault` and `removeIf` replace the classic loops.
- `getFirst`, `getLast` and `reversed` work on every sequenced collection.
- Return empty, unmodifiable collections from public methods.
