# Big-O, Amortised Analysis and Dynamic Arrays

Data structures and algorithms is the study of **how the cost of a program grows with its
input**. A method that takes 2 ms on your test data can take an hour in production because its
cost grows with the square of the input. This track teaches the structures and techniques that
keep costs low, ties each one to the place where the JDK and real systems use it, and practises
them as Java katas whose hidden tests time large inputs. It is also the core of most coding
interviews.

## Counting steps, not seconds

Seconds depend on the machine. Instead, count the **basic steps** (a comparison, an addition,
reading an array slot) as a function of the input size `n`, and keep only how that function
grows:

```java
int sum(int[] a) {            // n steps                    → O(n)
    int s = 0;
    for (int x : a) s += x;
    return s;
}

boolean hasDuplicate(int[] a) {  // n · (n - 1) / 2 comparisons → O(n²)
    for (int i = 0; i < a.length; i++)
        for (int j = i + 1; j < a.length; j++)
            if (a[i] == a[j]) return true;
    return false;
}
```

**Big-O** describes an upper bound on growth, ignoring constant factors and smaller terms:
`3n² + 100n + 7` is `O(n²)`. Two relatives are worth knowing: **Ω** (a lower bound) and **Θ**
(both at once, a tight bound). In everyday talk and interviews, "O(n)" usually means the tight
bound.

## The growth rates you will meet

| Big-O | Name | Typical code | n = 1,000,000 |
| --- | --- | --- | --- |
| O(1) | constant | array index, hash lookup | 1 |
| O(log n) | logarithmic | binary search, balanced tree lookup | ≈ 20 |
| O(n) | linear | one pass over the data | 10⁶ |
| O(n log n) | linearithmic | good sorting | ≈ 2 · 10⁷ |
| O(n²) | quadratic | all pairs, nested loops | 10¹² |
| O(2ⁿ) | exponential | all subsets | impossible beyond n ≈ 40 |
| O(n!) | factorial | all orderings | impossible beyond n ≈ 12 |

A modern CPU does roughly 10⁸ to 10⁹ simple steps per second. So at a million items, O(n log n)
is a fraction of a second and O(n²) is many minutes. That one table explains most
"it worked in tests" performance incidents.

## Rules for reading code

- **Sequence**: add the costs, keep the largest. `O(n) + O(n log n)` is `O(n log n)`.
- **Nesting**: multiply. A loop of n around a loop of m is `O(n · m)`.
- **Halving**: a loop that halves its range is `O(log n)`.
- **Hidden loops**: library calls have costs too. `list.contains(x)` on an `ArrayList` is O(n),
  `String.substring` copies, `s1 + s2` builds a new string. A loop over them is a nested loop.
- **Recursion**: count the calls and the work per call. Naive Fibonacci makes about φⁿ calls;
  a binary search makes log n.
- **Space** follows the same rules: extra arrays, maps and the depth of the call stack.

## Worst, average and amortised

These are three different questions:

- **Worst case**: the most any single call can cost. Quicksort is O(n²) in the worst case.
- **Average (expected) case**: the cost over random inputs or random choices. Quicksort with a
  random pivot is O(n log n) expected; a hash lookup is O(1) expected.
- **Amortised**: the cost of a **sequence** of operations divided by their number, guaranteed
  even in the worst case. One call may be expensive, but expensive calls are rare enough that the
  total stays small.

## The dynamic array: amortised O(1) append

A Java array has a fixed length. `ArrayList`, Python's `list`, Go's slices, C#'s `List<T>` and
JavaScript arrays all grow anyway: they keep a larger backing array than they need, and when it
is full they allocate a bigger one and copy everything.

```text
capacity 4:  [a b c d]           append e → full!
capacity 8:  [a b c d e _ _ _]   copied 4 elements, then wrote e
```

If the capacity **doubles**, the copies for n appends are at most `1 + 2 + 4 + ... + n < 2n`:
a total of O(n), so **O(1) amortised per append**. If instead the capacity grows by a constant
(say 10 slots), there are n/10 copies of up to n elements: O(n²) in total. The growth factor is
what matters, not its exact value: `ArrayList` grows by 1.5×, CPython by about 1.125×, Go by 2×
for small slices and less for large ones.

Shrinking needs care too. If an array halves as soon as it is half empty, then add, remove, add,
remove at the boundary resizes every time. Halving when it is a **quarter** full keeps the
amortised bound. You implement exactly this in the next kata.

## When Big-O is not the whole story

- **Constants matter at small n**: insertion sort beats merge sort below a few dozen items,
  which is why library sorts switch to it for small runs.
- **Memory locality matters**: an array walk reads consecutive memory and the CPU cache loves
  it; a linked list jumps around the heap. Both are O(n), but the array is often many times
  faster.
- **Know your n**: an O(n²) loop over 20 configuration entries is fine. An O(n log n) sort inside
  a request handler called a million times may not be.
- **Measure** when it matters: JMH for Java microbenchmarks, a profiler for real systems.

## In the JDK and in real systems

- `ArrayList` and `ArrayDeque` are dynamic arrays with geometric growth; `new ArrayList<>(n)`
  and `ensureCapacity(n)` skip the intermediate copies when you know the size.
- `HashMap(int initialCapacity)` does the same for the bucket array.
- `StringBuilder` is a dynamic array of characters; `String +` in a loop is the "grow by one"
  mistake.
- Databases, Kafka and log-structured stores pre-allocate and grow files geometrically for the
  same reason.

## Summary

- Big-O counts how steps grow with n, keeping the largest term and dropping constants.
- Learn the table: log n is about 20 at a million, n² is 10¹².
- Nested loops multiply; hidden library loops count.
- Amortised cost averages a worst-case sequence; doubling makes append O(1) amortised.
- Grow by a factor, shrink at a quarter; growing by a constant is quadratic.

## Further reading

- Sedgewick and Wayne, *Algorithms*, 4th ed., "Analysis of Algorithms": <https://algs4.cs.princeton.edu/14analysis/>
- Morin, *Open Data Structures* (free book), array-based lists and amortised analysis: <https://opendatastructures.org/>
- MIT OCW 6.006 Introduction to Algorithms (Spring 2020): <https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/>
- JDK 25 API, `ArrayList` (growth and amortised cost are documented in the class comment): <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ArrayList.html>
- roadmap.sh, DSA topic "Big O Notation": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/big-o-notation@oylTfop_JDPHJ3jYuA2Nq.md>
- roadmap.sh, DSA topic "How to Calculate Complexity": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/how-to-calculate-complexity@HosRc0hczubBHrWxCzEJj.md>
