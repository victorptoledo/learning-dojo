# Advanced Traits: Associated Types, Operators and Newtypes

> In the Intermediate phase you wrote traits and used them as bounds. The standard library goes
> further: `Iterator` names its item type inside the trait, `+` is just the `Add` trait, and
> `Vec<T>` behaves like a slice because of `Deref`. This lesson shows those tools, so your own
> types can feel as natural as the built-in ones.

**In this lesson you will learn**

- associated types, and when to prefer them over generic parameters
- implementing `Iterator` and `IntoIterator` for your own types
- operator overloading with `Add`, `Neg`, `Index` and friends
- the newtype pattern and the orphan rule
- `Deref`, and why it is not a way to fake inheritance
- supertraits, blanket implementations and dyn compatibility

---

## 1. Associated types

`Iterator` is declared like this:

```rust
pub trait Iterator {
    type Item;                                   // an associated type
    fn next(&mut self) -> Option<Self::Item>;
}
```

The implementer picks `Item` **once**:

```rust
struct Countdown(u32);

impl Iterator for Countdown {
    type Item = u32;
    fn next(&mut self) -> Option<u32> {
        if self.0 == 0 {
            return None;
        }
        self.0 -= 1;
        Some(self.0 + 1)
    }
}

let launch: Vec<u32> = Countdown(3).collect();   // [3, 2, 1]
```

Implementing `next` gives you every adapter for free: `map`, `filter`, `sum`, `collect`...

**Associated type or generic parameter?**

| | `trait Iterator { type Item; }` | `trait From<T> { ... }` |
| --- | --- | --- |
| How many impls per type? | **one** (a type yields one kind of item) | **many** (`String: From<&str>`, `From<char>`...) |
| Caller writes | `I: Iterator<Item = u32>` | `T: From<&str>` |

Use an associated type when the type decides; a generic parameter when the caller may choose.

## 2. `IntoIterator`: making `for` work

`for x in collection` calls `IntoIterator::into_iter(collection)`. Implement it for references
too, so callers can loop without giving up ownership:

```rust
pub struct Playlist { songs: Vec<String> }

impl<'a> IntoIterator for &'a Playlist {
    type Item = &'a String;
    type IntoIter = std::slice::Iter<'a, String>;
    fn into_iter(self) -> Self::IntoIter {
        self.songs.iter()
    }
}

for song in &playlist { println!("{song}"); }    // playlist is still usable afterwards
```

## 3. Operator overloading

Operators are traits in `std::ops`. Implement the trait and the operator works:

```rust
use std::ops::{Add, Neg};

#[derive(Debug, Clone, Copy, PartialEq)]
struct Vec2 { x: f64, y: f64 }

impl Add for Vec2 {
    type Output = Vec2;                         // another associated type
    fn add(self, other: Vec2) -> Vec2 {
        Vec2 { x: self.x + other.x, y: self.y + other.y }
    }
}

impl Neg for Vec2 {
    type Output = Vec2;
    fn neg(self) -> Vec2 { Vec2 { x: -self.x, y: -self.y } }
}

let v = Vec2 { x: 1.0, y: 2.0 } + -Vec2 { x: 0.5, y: 0.5 };
```

| Operator | Trait | Note |
| --- | --- | --- |
| `a + b`, `a - b`, `a * b` | `Add`, `Sub`, `Mul` | `Add<Rhs = Self>`: the right side may be another type |
| `a += b` | `AddAssign` | takes `&mut self` |
| `-a`, `!a` | `Neg`, `Not` | |
| `a[i]`, `a[i] = x` | `Index`, `IndexMut` | return a **reference** to the element |
| `a == b`, `a < b` | `PartialEq`, `PartialOrd` | usually derived |

```rust
use std::ops::Index;

struct Board { cells: [char; 9] }

impl Index<(usize, usize)> for Board {
    type Output = char;
    fn index(&self, (row, col): (usize, usize)) -> &char {
        &self.cells[row * 3 + col]
    }
}

let centre = board[(1, 1)];
```

> [!WARNING]
> Overload an operator only when its meaning is obvious. `money + money` is clear;
> `user + group` is a puzzle. Rust has no custom operators, which keeps the temptation small.

## 4. Newtypes and the orphan rule

You may implement a trait for a type only if **the trait or the type is yours** (the orphan
rule). So `impl Display for Vec<String>` is refused: both are from `std`.

The answer is a **newtype**, a tuple struct with one field:

```rust
struct Lines(Vec<String>);

impl std::fmt::Display for Lines {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.0.join("\n"))
    }
}
```

Newtypes also stop unit mix-ups at zero run-time cost (the wrapper compiles away):

```rust
struct Meters(f64);
struct Feet(f64);

fn climb(height: Meters) { /* ... */ }
climb(Feet(300.0));          // compile error: expected Meters, found Feet
```

## 5. `Deref`: smart pointers, not inheritance

`Deref` lets `*value` and method calls reach an inner value. It is why `&String` works where
`&str` is expected, and why a `Vec<T>` has every slice method:

```rust
use std::ops::Deref;

struct Sorted(Vec<i32>);

impl Deref for Sorted {
    type Target = [i32];
    fn deref(&self) -> &[i32] { &self.0 }
}

let s = Sorted(vec![1, 3, 5]);
s.len();                      // slice methods through deref coercion
s.binary_search(&3);
```

Notice what is **missing**: `DerefMut`. A `Sorted` that handed out `&mut [i32]` would let callers
break the sort order. Deref is for wrappers that **are** their target (pointers, buffers, `String`
and `str`), not to inherit the methods of a field.

## 6. Supertraits, blanket impls and `dyn`

A **supertrait** says "to implement me, implement that first":

```rust
trait Named: std::fmt::Display {
    fn shout(&self) -> String { self.to_string().to_uppercase() }   // can use Display
}
```

A **blanket implementation** implements a trait for every type that meets a bound. The standard
library does this to give every `Display` type a `to_string`:

```rust
impl<T: std::fmt::Display + ?Sized> ToString for T { /* ... */ }
```

Not every trait can be used as `dyn Trait`. A trait is **dyn compatible** (formerly "object
safe") when the vtable can describe it: no generic methods, no `Self` returned by value, no
`Self: Sized` requirement. `Clone` is not dyn compatible (`clone` returns `Self`); `Display` is.

## Practice

1. Write a `Fibonacci` iterator and use `.take(10).collect::<Vec<_>>()`.
2. Add `Sub` and `Mul<f64>` to `Vec2`. Why does `Mul<f64>` need `Rhs = f64`?
3. Write a newtype `Emails(Vec<String>)` with a `Display` that prints one per line.
4. Try `let v: Vec<Box<dyn Clone>>` and read the error.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| a type that yields items | `impl Iterator for T { type Item = ...; fn next(&mut self) ... }` |
| `for x in &my_type` | `impl<'a> IntoIterator for &'a MyType` |
| `a + b` on my type | `impl Add for T { type Output = T; ... }` |
| `a[i]` on my type | `impl Index<usize> for T { type Output = ...; }` |
| a trait on a foreign type | a newtype: `struct Wrapper(Foreign);` |
| a wrapper that acts like its contents | `impl Deref for W { type Target = ...; }` |
| a trait that needs another | `trait A: B { ... }` |

**Ready?** Press **Q** in the dojo for the quiz.
