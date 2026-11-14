# Collections: Vec, String and HashMap

> Almost every program keeps a list of things, builds some text and looks values up by key.
> Rust's standard library gives you one obvious type for each job: `Vec<T>`, `String` and
> `HashMap<K, V>`. All three own their data on the heap, and all three follow the ownership and
> borrowing rules you already know.

**In this lesson you will learn**

- how to create, grow, read and loop over a `Vec<T>`
- why indexing can panic and when to use `get` instead
- the difference between `String` and `&str`, and why you cannot index a string
- how to count and group things with a `HashMap` and its `entry` API
- which collection fits which problem

---

## 1. `Vec<T>`: a growable list

A vector stores values of **one type** next to each other in memory.

```rust
fn main() {
    let mut scores: Vec<u32> = Vec::new();
    scores.push(90);
    scores.push(72);

    let primes = vec![2, 3, 5, 7];       // the vec! macro builds one with values
    println!("{} scores, {} primes", scores.len(), primes.len());
}
```

The vector owns its elements. When it goes out of scope, every element is dropped with it.

### Reading elements

```rust
let primes = vec![2, 3, 5, 7];

let third = primes[2];                 // 5; panics if the index is out of range
let tenth = primes.get(9);             // None: get returns Option<&T>

match primes.get(1) {
    Some(value) => println!("second prime: {value}"),
    None => println!("too short"),
}
```

> [!TIP]
> Use `v[i]` when an out-of-range index would be a bug in your program. Use `v.get(i)` when the
> index comes from outside (user input, a file) and may legitimately be wrong.

### Looping

```rust
let mut prices = vec![10, 20, 30];

for price in &prices {          // borrow: read each element
    println!("{price}");
}

for price in &mut prices {      // mutable borrow: change each element
    *price += 1;                // * reaches the value behind the reference
}

for price in prices {           // no &: the loop takes ownership, prices is gone afterwards
    println!("{price}");
}
```

### Borrowing rules still apply

```rust
let mut names = vec![String::from("Ana")];
let first = &names[0];          // immutable borrow of names
names.push(String::from("Bia"));// error[E0502]: cannot borrow `names` as mutable
println!("{first}");
```

Why so strict? `push` may need more room and **move the whole buffer** to a new place in memory.
`first` would then point at freed memory. In C++ this is a classic bug; in Rust it does not
compile.

### Useful methods

| Method | What it does |
| --- | --- |
| `push(x)` / `pop()` | add at the end / remove the last (returns `Option<T>`) |
| `len()` / `is_empty()` | size / no elements |
| `contains(&x)` | linear search |
| `sort()` / `sort_by(...)` | sort in place |
| `dedup()` | remove consecutive duplicates |
| `retain(\|x\| ...)` | keep only the matching elements |
| `iter().sum()` / `iter().max()` | quick aggregates |

## 2. `String` and `&str`

| Type | Owns its text? | Can grow? | Typical use |
| --- | --- | --- | --- |
| `String` | yes, on the heap | yes | text you build or keep |
| `&str` | no, a borrowed view | no | function parameters, literals |

```rust
let mut greeting = String::from("hello");
greeting.push(' ');                 // one char
greeting.push_str("dojo");          // a &str
let shout = greeting.to_uppercase();// a new String

let name = "Ferris";                // a &'static str, stored in the binary
let line = format!("{greeting}, {name}!");  // format! builds a String
```

A function that only reads text should take `&str`. A `&String` turns into a `&str`
automatically, so callers can pass either:

```rust
fn word_count(text: &str) -> usize {
    text.split_whitespace().count()
}

let owned = String::from("one two three");
word_count(&owned);      // &String -> &str
word_count("four five"); // literal
```

### You cannot index a string

```rust
let word = String::from("olá");
let c = word[0]; // error[E0277]: the type `str` cannot be indexed by `{integer}`
```

Rust strings are **UTF-8**: `á` takes two bytes, many emoji take four. "The character at
position 0" is not a cheap, well-defined operation, so Rust makes you say what you mean:

```rust
let word = "olá";
word.len();                     // 4: bytes
word.chars().count();           // 3: characters
word.chars().next();            // Some('o')
word.chars().rev().collect::<String>(); // "álo"
&word[0..2];                    // "ol": a byte range; panics if it cuts a character in half
```

### Splitting and cleaning text

```rust
let text = "  The dojo, the KATA.  ";

text.trim();                                  // "The dojo, the KATA."
text.split_whitespace();                      // iterator over "The", "dojo,", "the", "KATA."
text.to_lowercase();                          // "  the dojo, the kata.  "
"dojo,".trim_matches(|c: char| !c.is_alphanumeric()); // "dojo"
"a,b,c".split(',').collect::<Vec<_>>();       // ["a", "b", "c"]
```

## 3. `HashMap<K, V>`: look values up by key

```rust
use std::collections::HashMap;   // HashMap is not in the prelude

fn main() {
    let mut stock: HashMap<String, u32> = HashMap::new();
    stock.insert(String::from("apple"), 3);
    stock.insert(String::from("pear"), 5);

    if let Some(count) = stock.get("apple") {     // get takes a &str for String keys
        println!("apples: {count}");
    }

    stock.insert(String::from("apple"), 10);       // insert replaces the old value
    stock.remove("pear");

    for (fruit, count) in &stock {                 // order is NOT guaranteed
        println!("{fruit}: {count}");
    }
}
```

The map **takes ownership** of keys and values you insert. After `stock.insert(name, 3)`, the
`String` called `name` has moved into the map.

### The `entry` API: count, group, default

Counting is the most common job of a map. Without `entry` you look the key up twice:

```rust
// clumsy
if let Some(count) = counts.get_mut(word) {
    *count += 1;
} else {
    counts.insert(word.to_string(), 1);
}
```

With `entry`, one line:

```rust
let mut counts: HashMap<String, usize> = HashMap::new();
for word in "a b a c a".split_whitespace() {
    *counts.entry(word.to_string()).or_insert(0) += 1;
}
// {"a": 3, "b": 1, "c": 1}
```

`entry(key)` finds the slot for the key; `or_insert(0)` puts `0` there if it is empty and returns
a `&mut usize` to the value either way; `*... += 1` increments it.

Grouping works the same way, with a `Vec` as the value:

```rust
let mut by_length: HashMap<usize, Vec<&str>> = HashMap::new();
for word in ["ox", "cat", "dog", "be"] {
    by_length.entry(word.len()).or_default().push(word);
}
// {2: ["ox", "be"], 3: ["cat", "dog"]}
```

`or_default()` inserts `Default::default()` (an empty `Vec`, `0`, an empty `String`...).

### Getting a sorted result

A `HashMap` has no order. To print or return results in a predictable order, move them into a
`Vec` and sort:

```rust
let mut pairs: Vec<(String, usize)> = counts.into_iter().collect();
// highest count first; on a tie, alphabetical
pairs.sort_by(|a, b| b.1.cmp(&a.1).then_with(|| a.0.cmp(&b.0)));
```

> [!NOTE]
> Need a map that is always sorted by key? Use `BTreeMap` from the same module: same API,
> ordered iteration, slightly slower lookups.

## 4. Choosing a collection

| Problem | Collection |
| --- | --- |
| an ordered list, appended at the end | `Vec<T>` |
| text you build or modify | `String` |
| look up a value by a key | `HashMap<K, V>` |
| keys in sorted order | `BTreeMap<K, V>` |
| "have I seen this already?" | `HashSet<T>` |
| a queue (push back, pop front) | `VecDeque<T>` |

> [!WARNING]
> **Common mistake:** `vec.contains(&x)` inside a loop over another big list is O(n²). If you
> test membership often, build a `HashSet` once and ask it instead.

> [!NOTE]
> **Coming from Java:** `Vec<T>` is `ArrayList<T>`, `HashMap` is `HashMap`, `String` is a
> mutable `StringBuilder`-like type, and `&str` is closest to a read-only `CharSequence` view.
> The big difference: no `null` entries (`get` returns an `Option`), and the borrow checker
> stops `ConcurrentModificationException` bugs at compile time.

## Practice

1. Build a `Vec<i32>` with `vec!`, double every element with `for x in &mut v`, and print the sum.
2. Write `fn initials(name: &str) -> String` that turns `"ada lovelace"` into `"AL"`.
3. Count the letters of `"mississippi"` with a `HashMap<char, usize>` and the `entry` API.
4. Try to `push` into a vector while holding a reference to one of its elements. Read the error.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| a vector with values | `vec![1, 2, 3]` |
| a safe element read | `v.get(i)` → `Option<&T>` |
| loop without consuming | `for x in &v` |
| text from parts | `format!("{a}-{b}")` |
| characters of a string | `s.chars()` |
| words of a string | `s.split_whitespace()` |
| count with a map | `*map.entry(k).or_insert(0) += 1` |
| group with a map | `map.entry(k).or_default().push(v)` |
| sorted output from a map | collect into a `Vec`, then `sort_by` |

**Ready?** Press **Q** in the dojo for the quiz.
