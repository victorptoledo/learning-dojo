# Traits: Interfaces with Superpowers

> A trait describes behaviour that different types can share, like an interface. Traits are also
> how generics work in Rust: "any type that can do X". And because the compiler specialises
> generic code for each type, the abstraction costs nothing at runtime.

**In this lesson you will learn**

- how to define a trait, implement it, and add default methods
- the standard traits you use every day, and `#[derive]`
- generic functions with trait bounds, `impl Trait`, and `where`
- trait objects (`dyn Trait`) for collections of different types
- static vs dynamic dispatch, and how to choose

---

## 1. Defining and implementing a trait

```rust
trait Describe {
    fn describe(&self) -> String;                 // required

    fn shout(&self) -> String {                   // default: optional to override
        self.describe().to_uppercase()
    }
}

struct Ninja {
    name: String,
}

struct Robot {
    model: u32,
}

impl Describe for Ninja {
    fn describe(&self) -> String {
        format!("ninja {}", self.name)
    }
}

impl Describe for Robot {
    fn describe(&self) -> String {
        format!("robot #{}", self.model)
    }
    fn shout(&self) -> String {                   // overrides the default
        String::from("BEEP BOOP")
    }
}
```

`Ninja` gets the default `shout`; `Robot` provides its own.

## 2. Implementing traits for types you didn't write

You can implement **your** trait for **any** type, even `i32` or `String`:

```rust
impl Describe for i32 {
    fn describe(&self) -> String {
        format!("the number {self}")
    }
}
println!("{}", 7_i32.describe());   // the number 7 (the suffix says which integer type)
```

And you can implement **standard** traits for **your** types. The one limit is the **orphan rule**:
the trait or the type must be defined in your crate. You can't implement `Display` (from std)
for `Vec` (from std), because two crates could do it differently.

## 3. Standard traits and `derive`

| Trait | Gives you | Derivable |
| --- | --- | --- |
| `Debug` | `{:?}` formatting for developers | yes |
| `Display` | `{}` formatting for people | no, write it |
| `Clone` | `.clone()` | yes |
| `Copy` | implicit copies (only small stack types) | yes |
| `PartialEq` / `Eq` | `==` and `!=` | yes |
| `PartialOrd` / `Ord` | `<`, `>`, sorting | yes |
| `Hash` | use as a `HashMap` key | yes |
| `Default` | `Type::default()` | yes |

```rust
#[derive(Debug, Clone, PartialEq)]
struct Point {
    x: i32,
    y: i32,
}

let a = Point { x: 1, y: 2 };
let b = a.clone();
println!("{a:?} equals {b:?}: {}", a == b);   // Point { x: 1, y: 2 } equals Point { x: 1, y: 2 }: true
```

## 4. Generics with trait bounds

A generic function works for many types. The **bound** says what those types must be able to do:

```rust
fn announce<T: Describe>(item: &T) {
    println!("Here comes {}", item.describe());
}

announce(&Ninja { name: String::from("Ada") });
announce(&Robot { model: 42 });
```

Two other ways to write the same thing:

```rust
fn announce(item: &impl Describe) { /* ... */ }          // short form for one parameter

fn compare<T, U>(a: &T, b: &U) -> bool
where
    T: Describe,
    U: Describe + PartialEq,                              // several bounds with +
{
    a.describe() == b.describe()
}
```

Without the bound, the compiler doesn't know `item` has `describe`:

```
error[E0599]: no method named `describe` found for reference `&T` in the current scope
help: restrict type parameter `T` with trait `Describe`
```

### How generics run: monomorphisation

The compiler writes a **separate copy** of `announce` for each type you call it with:
`announce::<Ninja>` and `announce::<Robot>`. Each call goes straight to the right method, can be
inlined, and costs exactly what a hand-written function would. This is **static dispatch**.

## 5. Trait objects: `dyn Trait`

Generics need one concrete type per call. To keep **different** types in one collection, use a
trait object:

```rust
let crew: Vec<Box<dyn Describe>> = vec![
    Box::new(Ninja { name: String::from("Ada") }),
    Box::new(Robot { model: 7 }),
];

for member in &crew {
    println!("{}", member.shout());
}
```

- `dyn Describe` means "some type implementing `Describe`, decided at runtime".
- Its size isn't known at compile time, so it lives behind a pointer: `Box<dyn ...>` or `&dyn ...`.
- Each call looks up the right method in a table (a **vtable**): **dynamic dispatch**,
  like every virtual call in Java.

| | Generics (`T: Trait`) | Trait objects (`dyn Trait`) |
| --- | --- | --- |
| Type decided | at compile time | at runtime |
| Mixed types in one collection | no | yes |
| Call cost | direct, inlinable | one indirection |
| Binary size | one copy per type | one copy |
| Use when | most of the time | heterogeneous collections, plugins |

> [!NOTE]
> **Coming from Java:** a trait is close to an interface with default methods. The differences:
> you can implement a trait for a type **after** it was written (even for `i32`), generic bounds
> are checked and specialised at compile time, and there is no class inheritance at all: behaviour
> is shared through traits and composition.

> [!TIP]
> Design small traits with one clear job (`Describe`, `Area`, `Validate`), the way the standard
> library does (`Display`, `Clone`, `Iterator`). A type can implement as many as it needs.

## Practice

1. Add a trait `Area { fn area(&self) -> f64; }` and implement it for two shapes from the
   previous lesson. Write `fn total_area(shapes: &[Box<dyn Area>]) -> f64`.
2. Write a generic `fn largest<T: PartialOrd + Copy>(items: &[T]) -> T` and use it with `i32`
   and `f64`.
3. Implement `Display` for `Point` so `println!("{p}")` prints `(1, 2)`.
4. Try to implement `Display` for `Vec<i32>` and read the orphan-rule error.

## Cheat sheet

| I want... | Write |
| --- | --- |
| shared behaviour | `trait Name { fn method(&self); }` |
| a default method | a body inside the trait |
| standard behaviour for free | `#[derive(Debug, Clone, PartialEq)]` |
| a function for any type that can X | `fn f<T: X>(v: &T)` or `fn f(v: &impl X)` |
| a list of different types | `Vec<Box<dyn X>>` |

**Ready?** Press **Q** in the dojo for the quiz.
