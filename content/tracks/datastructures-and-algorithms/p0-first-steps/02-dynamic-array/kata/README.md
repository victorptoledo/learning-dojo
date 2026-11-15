# Dynamic Array

Write `DynamicArray<T>`, a growable array backed by a plain `Object[]`. Do **not** use
`ArrayList` or any other collection inside it: the point is to write the growth yourself.

```java
var list = new DynamicArray<String>();   // capacity 4, size 0
list.add("a"); list.add("b");            // [a, b]
list.add(0, "z");                        // [z, a, b]
list.get(1);                             // "a"
list.removeLast();                       // "b", the list is [z, a]
```

## The rules

- A new array has **capacity 4** and size 0. `capacity()` is the length of the backing array.
- **Growth**: when an add finds the array full, the capacity **doubles** before the element goes
  in.
- **Shrinking**: after a removal, if `size <= capacity / 4` and the capacity is above 4, the
  capacity **halves**. Halving at a quarter, not at a half, means that adding and removing at a
  boundary cannot resize on every call.
- `get`, `set` and `remove` take an index from `0` to `size - 1`; `add(index, value)` accepts
  `0` to `size`. Any other index throws `IndexOutOfBoundsException`.
- `removeLast()` on an empty array throws `NoSuchElementException`.
- `null` elements are allowed.
- The array is `Iterable<T>`: a for-each loop visits the elements from index 0.

## Methods

| Method | Does | Cost |
| --- | --- | --- |
| `add(value)` | appends | amortised O(1) |
| `add(index, value)` | inserts, shifting the rest right | O(n) |
| `get(index)`, `set(index, value)` | reads, replaces (returns the old value) | O(1) |
| `removeLast()` | removes and returns the last element | amortised O(1) |
| `remove(index)` | removes and returns, shifting the rest left | O(n) |
| `size()`, `isEmpty()`, `capacity()` | | O(1) |

## What the hidden tests check

- Every method against a reference list, including bad indexes.
- After a million appends the capacity changed at most 20 times, and it is always at least the
  size.
- Shrinking happens exactly at a quarter and never goes below 4.
- Adding and removing again and again at a capacity boundary resizes at most twice.
- Three million appends followed by three million `removeLast` calls finish within a time limit
  that a "grow by 10 slots" array cannot meet.

> Tip: `System.arraycopy(src, from, dest, to, count)` shifts elements and handles overlapping
> ranges correctly. Set the freed slot to `null` after a removal so the array does not keep the
> object alive.
