# Variables, Immutability and Shadowing

> In Rust a variable cannot change unless you say so. That one default removes a whole family of
> bugs, and it makes code easier to read: when nothing says `mut`, nothing changes.

**In this lesson you will learn**

- how `let` declares a variable and why it is immutable by default
- when and how to use `mut`
- the basic types and how Rust infers them
- shadowing: reusing a name for a new value, even of another type
- `const`, and how all of this differs from `final` in Java

---

## 1. `let` creates an immutable variable

```rust
fn main() {
    let level = 1;
    println!("level {level}");
}
```

`let` gives a name to a value. Try to change it:

```rust
let level = 1;
level = 2;
```

The compiler stops you:

```
error[E0384]: cannot assign twice to immutable variable `level`
 --> src/main.rs:3:5
  |
2 |     let level = 1;
  |         ----- first assignment to `level`
3 |     level = 2;
  |     ^^^^^^^^^ cannot assign twice to immutable variable
  |
help: consider making this binding mutable
  |
2 |     let mut level = 1;
  |         +++
```

> [!TIP]
> Read compiler errors from the top: the **code** (`E0384`), the **message**, the **arrows**
> pointing at the exact spot, and the **help** with a suggested fix. `rustc --explain E0384`
> prints a full explanation with examples.

## 2. `mut` when the value must change

```rust
let mut level = 1;
level += 1;
println!("level {level}");   // level 2
```

`mut` is a promise to the reader: "watch this one, it changes". Most variables never need it.
If you add `mut` and never change the value, the compiler warns you (`variable does not need
to be mutable`).

## 3. Types and inference

Rust is statically typed, but you rarely write the types: the compiler infers them.

```rust
let count = 10;            // i32: the default integer
let ratio = 0.75;          // f64: the default float
let done = false;          // bool
let grade = 'A';           // char: one Unicode character, single quotes
let name = "Ferris";       // &str: a borrowed text, double quotes
```

You write the type when inference has nothing to go on, or to choose a different one:

```rust
let small: u8 = 255;               // unsigned 8-bit: 0 to 255
let big: i64 = 9_000_000_000;      // underscores make long numbers readable
let parsed: i32 = "42".parse().unwrap();   // parse() needs to know the target type
```

| Family | Types | Notes |
| --- | --- | --- |
| signed integers | `i8` `i16` `i32` `i64` `i128` `isize` | `i32` is the default |
| unsigned integers | `u8` `u16` `u32` `u64` `u128` `usize` | `usize` is used for lengths and indexes |
| floats | `f32` `f64` | `f64` is the default |
| other | `bool`, `char` | `char` is 4 bytes and holds any Unicode character |

> [!WARNING]
> **Overflow:** a `u8` holds at most 255. If the compiler can see that `255u8 + 1` overflows, it
> refuses to compile. When the value only appears at runtime, a debug build panics ("attempt to
> add with overflow") and a release build wraps around to `0`. When wrapping is what you want,
> say so: `x.wrapping_add(1)`; to detect it, use `x.checked_add(1)`, which returns `None`.

## 4. Shadowing: a new variable with the same name

You can declare a variable again with the same name. The new one **shadows** (hides) the old one:

```rust
let input = "42";                       // &str
let input: i32 = input.parse().unwrap(); // i32: a brand-new variable
println!("{}", input + 1);               // 43
```

This is not mutation: it is a second variable that happens to reuse the name. That is why the
type can change. It is handy when you transform a value step by step and the old form is no
longer useful, so you don't need names like `input_str` and `input_num`.

Shadowing in an inner block ends with the block:

```rust
let x = 1;
{
    let x = x * 10;
    println!("inside: {x}");    // inside: 10
}
println!("outside: {x}");       // outside: 1
```

| | `let mut x` then `x = ...` | `let x = ...` again (shadowing) |
| --- | --- | --- |
| Same variable? | yes | no, a new one |
| Can change type? | no | yes |
| Needs `mut`? | yes | no |

## 5. Constants

```rust
const MAX_LEVEL: u32 = 100;
```

- always immutable, `mut` is not allowed
- the type is mandatory
- the value must be known at compile time
- written in `SCREAMING_SNAKE_CASE`, usually at the top of the file

> [!NOTE]
> **Coming from Java:** in Java everything is mutable unless marked `final`, and even a `final`
> reference can point to an object whose fields change. In Rust immutability is the default and
> it is deep: through an immutable binding you cannot change the value at all, fields included.
> `const` is closer to `static final` in Java.

## 6. Naming

Rust uses `snake_case` for variables and functions, `CamelCase` for types and
`SCREAMING_SNAKE_CASE` for constants. The compiler warns when you don't, and `cargo clippy`
catches more style issues.

## Practice

1. Declare `let total = 0;` and try `total += 5;`. Read the error, then fix it with `mut`.
2. Read a number from a text with shadowing: start from `let age = "30";` and end with an `i32`
   you can add `1` to.
3. Write `let x: u8 = 250;` and print `x.checked_add(10)` and `x.wrapping_add(10)`. What do you
   get, and why?

## Cheat sheet

| I want... | Write |
| --- | --- |
| a value that never changes | `let name = value;` |
| a value that changes | `let mut name = value;` |
| a specific type | `let name: u64 = 10;` |
| to reuse a name for a transformed value | `let name = transform(name);` |
| a compile-time constant | `const NAME: Type = value;` |

**Ready?** Press **Q** in the dojo for the quiz.
