# Iterators and Closures

> Most loops in idiomatic Rust are not `for i in 0..len` loops. They are **iterator chains**:
> take a sequence, `filter` it, `map` it, `collect` the result. Closures are the small anonymous
> functions you pass to those steps. Together they read like a description of what you want, and
> the compiler turns them into code as fast as a hand-written loop.

**In this lesson you will learn**

- what a closure is and how it captures variables from its surroundings
- the three closure traits `Fn`, `FnMut` and `FnOnce`, and what `move` does
- the `Iterator` trait and why iterators are lazy
- the adapters you will use every day: `map`, `filter`, `enumerate`, `zip`, `take`, `skip`...
- the consumers that produce a result: `collect`, `sum`, `count`, `fold`, `find`, `any`...
- `iter()`, `iter_mut()` and `into_iter()`, and how to write your own iterator

---

## 1. Closures

A closure is a function without a name, written with `|parameters| body`:

```rust
let double = |x: i32| x * 2;
let add = |a, b| a + b;              // types inferred from the first use
let greet = || println!("hello");    // no parameters

println!("{}", double(21));          // 42
println!("{}", add(2, 3));           // 5
greet();
```

Unlike a normal `fn`, a closure can **use variables from where it was created**:

```rust
let threshold = 10;
let is_big = |n: i32| n > threshold;   // captures threshold by reference
println!("{}", is_big(15));            // true
```

### How closures capture: `Fn`, `FnMut`, `FnOnce`

The compiler picks the lightest capture that works, and that decides which trait the closure
implements:

| The closure... | Captures | Trait | Can be called |
| --- | --- | --- | --- |
| only reads captured values | `&T` | `Fn` | many times |
| changes captured values | `&mut T` | `FnMut` | many times, not at the same time |
| moves a captured value out | `T` | `FnOnce` | once |

```rust
let mut count = 0;
let mut increment = || count += 1;   // FnMut: changes count
increment();
increment();
println!("{count}");                 // 2

let name = String::from("Ferris");
let consume = move || name;          // FnOnce: gives name away
let taken = consume();
// consume();                        // error: closure cannot be called twice
```

### `move`

`move` makes the closure **take ownership** of everything it captures, even if it only reads it.
You need it when the closure outlives the current function, for example in a thread or when you
return it:

```rust
fn make_adder(n: i32) -> impl Fn(i32) -> i32 {
    move |x| x + n       // without move, n would be a reference to a dead local
}

let add_five = make_adder(5);
println!("{}", add_five(1));  // 6
```

### Taking closures as parameters

Use generics with a closure trait bound:

```rust
fn apply_twice<F: Fn(i32) -> i32>(f: F, value: i32) -> i32 {
    f(f(value))
}

apply_twice(|x| x * 3, 2);   // 18
```

Ask for the **most permissive** trait your function can live with: `FnOnce` if you call it once,
`FnMut` if you call it repeatedly and allow mutation, `Fn` only if you need to share it.

## 2. The `Iterator` trait

An iterator is anything that implements one method:

```rust
trait Iterator {
    type Item;
    fn next(&mut self) -> Option<Self::Item>;
    // ...and around 70 provided methods built on top of next
}
```

`next` returns `Some(item)` until the sequence ends, then `None`. A `for` loop is just sugar for
calling `next` until `None`.

### Iterators are lazy

```rust
let numbers = vec![1, 2, 3];
numbers.iter().map(|n| {
    println!("visiting {n}");
    n * 2
});
// prints nothing! warning: unused `Map` that must be used
```

Adapters like `map` only **describe** work. Nothing runs until a **consumer** pulls items
through the chain:

```rust
let doubled: Vec<i32> = numbers.iter().map(|n| n * 2).collect();  // now it runs
```

Laziness is what makes chains efficient: no intermediate vectors are built, and `take(3)` really
stops after three items.

### Three ways to iterate a collection

| Call | Items | The collection afterwards |
| --- | --- | --- |
| `v.iter()` | `&T` | still usable |
| `v.iter_mut()` | `&mut T` | still usable, possibly changed |
| `v.into_iter()` | `T` | moved, gone |

`for x in &v` is `v.iter()`, `for x in &mut v` is `v.iter_mut()`, `for x in v` is
`v.into_iter()`.

## 3. Adapters: transform the sequence

```rust
let words = ["apple", "kiwi", "banana", "fig"];

words.iter().map(|w| w.len());                 // 5, 4, 6, 3
words.iter().filter(|w| w.len() > 3);          // "apple", "kiwi", "banana"
words.iter().enumerate();                      // (0, "apple"), (1, "kiwi"), ...
words.iter().zip(1..);                         // ("apple", 1), ("kiwi", 2), ...
words.iter().skip(1).take(2);                  // "kiwi", "banana"
words.iter().rev();                            // "fig", "banana", ...
words.iter().filter_map(|w| w.strip_prefix('b')); // "anana"
words.iter().flat_map(|w| w.chars());          // 'a', 'p', 'p', ...
words.iter().take_while(|w| w.len() > 3);      // "apple", "kiwi", "banana" (stops at "fig")
```

> [!TIP]
> `filter` gives its closure a **reference to the item**. With `iter()` the items are already
> references, so the closure sees `&&str`. Auto-deref usually hides this, but when you compare
> you may need a pattern: `.filter(|&&w| w != "fig")` or `.filter(|w| **w != "fig")`.

## 4. Consumers: produce a result

```rust
let numbers = vec![3, 1, 4, 1, 5, 9, 2, 6];

let total: i32 = numbers.iter().sum();                 // 31
let evens = numbers.iter().filter(|n| *n % 2 == 0).count(); // 3
let biggest = numbers.iter().max();                    // Some(&9)
let first_big = numbers.iter().find(|n| **n > 4);      // Some(&5)
let position = numbers.iter().position(|n| *n == 4);   // Some(2)
let any_zero = numbers.iter().any(|n| *n == 0);        // false
let all_small = numbers.iter().all(|n| *n < 10);       // true
let product = numbers.iter().fold(1, |acc, n| acc * n);// 6480
```

### `collect` builds any collection

`collect` looks at the type you ask for:

```rust
use std::collections::{HashMap, HashSet};

let squares: Vec<u32> = (1..=4).map(|n| n * n).collect();         // [1, 4, 9, 16]
let unique: HashSet<char> = "hello".chars().collect();            // {'h', 'e', 'l', 'o'}
let lengths: HashMap<&str, usize> = ["ox", "cat"].iter().map(|w| (*w, w.len())).collect();
let sentence: String = vec!["a", "b", "c"].join(" ");             // join is on slices
let text: String = ['r', 'u', 's', 't'].iter().collect();         // "rust"
```

Even errors: collecting an iterator of `Result<T, E>` into `Result<Vec<T>, E>` stops at the
first error.

```rust
let parsed: Result<Vec<i32>, _> = ["1", "2", "x"].iter().map(|s| s.parse::<i32>()).collect();
assert!(parsed.is_err());
```

## 5. From loop to chain

A typical loop:

```rust
let mut names = Vec::new();
for user in &users {
    if user.active {
        names.push(user.name.to_uppercase());
    }
}
```

The same as a chain:

```rust
let names: Vec<String> = users
    .iter()
    .filter(|user| user.active)
    .map(|user| user.name.to_uppercase())
    .collect();
```

Each step has one job, there is no mutable accumulator, and adding a step (`take(10)`, `rev()`)
does not disturb the others.

> [!NOTE]
> Chains are not slower. The compiler inlines the closures and fuses the steps into one loop,
> often without bounds checks. This is the "zero-cost abstraction" Rust is known for.

## 6. Writing your own iterator

Implement `next` and you get every adapter for free:

```rust
struct Countdown {
    remaining: u32,
}

impl Iterator for Countdown {
    type Item = u32;

    fn next(&mut self) -> Option<u32> {
        if self.remaining == 0 {
            return None;
        }
        self.remaining -= 1;
        Some(self.remaining + 1)
    }
}

let launch: Vec<u32> = Countdown { remaining: 3 }.collect();  // [3, 2, 1]
let sum: u32 = Countdown { remaining: 10 }.filter(|n| n % 2 == 0).sum(); // 30
```

> [!WARNING]
> **Common mistake:** calling `collect()` without telling it what to build. `let v =
> iter.collect();` fails with "type annotations needed". Write `let v: Vec<_> = ...` or
> `collect::<Vec<_>>()` (the "turbofish").

> [!NOTE]
> **Coming from Java:** this is the Stream API without `stream()` and `Collectors`: `map`,
> `filter`, `reduce` (here `fold`), `anyMatch` (here `any`). Rust iterators can be reused only
> by calling `iter()` again, just like a stream can only be consumed once.

## Practice

1. With one chain, compute the sum of the squares of the odd numbers from 1 to 100.
2. Turn `vec!["3", "x", "7"]` into a `Vec<i32>` keeping only what parses (`filter_map` and `ok()`).
3. Write `fn counter() -> impl FnMut() -> u32` that returns 1, 2, 3... on each call.
4. Implement an iterator `Fibonacci` and print its first 10 values with `take(10)`.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| an anonymous function | `\|x\| x + 1` |
| a closure that owns its captures | `move \|\| ...` |
| take a closure parameter | `fn f<F: Fn(i32) -> i32>(g: F)` |
| return a closure | `-> impl Fn(i32) -> i32` |
| transform each item | `.map(\|x\| ...)` |
| keep some items | `.filter(\|x\| ...)` |
| index and item | `.enumerate()` |
| pair two sequences | `.zip(other)` |
| build a collection | `.collect::<Vec<_>>()` |
| reduce to one value | `.fold(init, \|acc, x\| ...)` |
| first match | `.find(\|x\| ...)` |

**Ready?** Press **Q** in the dojo for the quiz.
