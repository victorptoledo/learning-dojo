# No Null, No Exceptions: Option and Result

> Rust has neither `null` nor exceptions. "There might be no value" and "this might fail" are
> ordinary values, written in the function's type, so the compiler makes sure you deal with them.
> You lose surprise `NullPointerException`s and invisible error paths; you gain a few `?`.

**In this lesson you will learn**

- `Option<T>` for a value that may be absent
- `Result<T, E>` for an operation that may fail
- the `?` operator to pass errors up in one character
- the helper methods that save you from writing `match` every time
- when `unwrap` and `expect` are acceptable, and when they are a bug

---

## 1. `Option<T>`: a value or nothing

`Option` is just an enum from the standard library:

```rust
enum Option<T> {
    Some(T),
    None,
}
```

A function that may have no answer says so in its type:

```rust
fn first_word(text: &str) -> Option<&str> {
    text.split_whitespace().next()
}

fn main() {
    match first_word("hello dojo") {
        Some(word) => println!("first: {word}"),
        None => println!("the text is empty"),
    }
}
```

You **cannot** use an `Option<&str>` where a `&str` is expected:

```rust
let word: &str = first_word(""); // error[E0308]: mismatched types: expected `&str`, found `Option<&str>`
```

The compiler forces the "what if there is nothing?" question at the place where it matters.

## 2. `Result<T, E>`: a value or an error

```rust
enum Result<T, E> {
    Ok(T),
    Err(E),
}
```

`str::parse` returns a `Result`, because the text may not be a number:

```rust
fn main() {
    match "42".parse::<i32>() {
        Ok(n) => println!("got {n}"),
        Err(error) => println!("not a number: {error}"),
    }
}
```

The error is a real value, with a type (`ParseIntError` here) and a message
(`invalid digit found in string`). Nothing is thrown, nothing unwinds across five layers.

## 3. The `?` operator

Writing `match` for every step gets long. Inside a function that returns `Result`, `?` does it
for you: on `Ok` it gives you the value, on `Err` it **returns the error to the caller** right away.

```rust
use std::num::ParseIntError;

fn add_strings(a: &str, b: &str) -> Result<i32, ParseIntError> {
    let x: i32 = a.parse()?;   // on error, add_strings returns Err(...) here
    let y: i32 = b.parse()?;
    Ok(x + y)
}

fn main() {
    println!("{:?}", add_strings("2", "3"));    // Ok(5)
    println!("{:?}", add_strings("2", "x"));    // Err(ParseIntError { kind: InvalidDigit })
}
```

`?` also works on `Option` inside a function that returns `Option`: `None` is returned early.

> [!WARNING]
> **Common mistake:** using `?` in a function that returns nothing (`fn main()` or `-> ()`).
> The compiler says "the `?` operator can only be used in a function that returns `Result` or
> `Option`". Change the return type, or handle the error with `match`.

## 4. Your own error types

When a function can fail in several ways, describe them with an enum:

```rust
use std::fmt;

#[derive(Debug, PartialEq)]
enum AgeError {
    NotANumber,
    TooOld(u32),
}

impl fmt::Display for AgeError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            AgeError::NotANumber => write!(f, "the age must be a number"),
            AgeError::TooOld(age) => write!(f, "{age} is not a realistic age"),
        }
    }
}

impl std::error::Error for AgeError {}

fn parse_age(text: &str) -> Result<u32, AgeError> {
    let age: u32 = text.trim().parse().map_err(|_| AgeError::NotANumber)?;
    if age > 150 {
        return Err(AgeError::TooOld(age));
    }
    Ok(age)
}
```

- `Display` gives people a readable message; `std::error::Error` lets other code treat it as an error.
- `map_err` converts one error type into yours before `?` returns it.
- Callers can `match` on the variants and react differently to each failure.

This is exactly what the **config-parser** kata asks you to build.

## 5. Helper methods

| Method | On | What it does |
| --- | --- | --- |
| `unwrap_or(default)` | Option, Result | the value, or `default` |
| `unwrap_or_default()` | Option, Result | the value, or `0`, `""`, empty... |
| `map(f)` | Option, Result | transforms the inner value, leaves `None`/`Err` alone |
| `and_then(f)` | Option, Result | chains a step that can also fail |
| `ok_or(error)` | Option | turns `None` into `Err(error)` |
| `ok()` | Result | turns `Err` into `None`, dropping the error |
| `is_some()` / `is_none()` | Option | checks without taking the value |
| `is_ok()` / `is_err()` | Result | the same for Result |

```rust
let port: u16 = std::env::var("PORT")
    .ok()                                  // Result -> Option
    .and_then(|text| text.parse().ok())    // parse, or None
    .unwrap_or(8080);                      // default when anything was missing
```

## 6. `unwrap` and `expect`

`unwrap()` returns the value or **panics**: the program stops with an error message.
`expect("message")` does the same with your message.

They are fine in:

- tests and quick experiments
- cases you have just proven impossible, with an `expect` explaining why:
  `"42".parse::<i32>().expect("a constant literal is a valid number")`

They are a bug in code that handles input from users, files or the network: there, failure is
normal and should be returned, not crash the program.

> [!NOTE]
> **Coming from Java:** `Option` is like `Optional` that the compiler enforces everywhere, and
> `Result` replaces checked exceptions. `?` plays the role of `throws`, but the error is a value
> you can see in the signature, and `match` replaces `try/catch`.

## Practice

1. Write `fn divide(a: i32, b: i32) -> Option<i32>` returning `None` when `b` is `0`.
2. Write `fn sum_all(texts: &[&str]) -> Result<i32, std::num::ParseIntError>` that parses each
   text with `?` and adds them up. Try it with one invalid number.
3. Rewrite `parse_age` to also reject an empty text with a third variant `Empty`, and update
   `Display`.
4. Find an `unwrap()` in your own code and replace it with `?` or `unwrap_or`.

## Cheat sheet

| Situation | Type | Handle with |
| --- | --- | --- |
| a value may be missing | `Option<T>` | `match`, `if let`, `unwrap_or`, `?` |
| an operation may fail | `Result<T, E>` | `match`, `?`, `map_err` |
| several ways to fail | `Result<T, MyError>` | an error enum with `Display` + `Error` |
| a failure would be a bug | either | `expect("why this can't happen")` |

**Ready?** Press **Q** in the dojo for the quiz.
