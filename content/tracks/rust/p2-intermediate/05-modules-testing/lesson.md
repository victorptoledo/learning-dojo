# Modules, Crates and Tests

> A program that fits in one `main.rs` is a script. Real projects split code into **modules**,
> depend on other people's **crates**, and prove they work with **tests**. Cargo does most of the
> work; you only need to know where things go and what `pub`, `use` and `#[test]` mean.

**In this lesson you will learn**

- packages, crates and modules: what each word means
- how `mod`, `pub` and `use` control what code can see
- how a module maps to files and folders
- how to add a dependency from crates.io with `cargo add`
- unit tests, integration tests and doc tests, and how to run them
- `assert!`, `assert_eq!`, `#[should_panic]` and tests that return `Result`

---

## 1. Packages, crates and modules

| Word | What it is | Where you see it |
| --- | --- | --- |
| **package** | a folder with a `Cargo.toml` | what `cargo new` creates |
| **crate** | one compilation unit: a library or a binary | `src/lib.rs`, `src/main.rs` |
| **module** | a named namespace inside a crate | `mod parser { ... }` |

A package can contain **one library crate** (`src/lib.rs`) and **any number of binaries**
(`src/main.rs`, `src/bin/*.rs`). The binary uses the library like any external crate, which keeps
your logic testable and your `main` thin.

```text
shop/
├── Cargo.toml
├── src/
│   ├── lib.rs        ← the library crate root (package name: shop)
│   ├── main.rs       ← a binary that calls shop::...
│   ├── inventory.rs  ← module inventory
│   └── pricing/
│       ├── mod.rs    ← module pricing (or src/pricing.rs)
│       └── tax.rs    ← module pricing::tax
└── tests/
    └── checkout.rs   ← an integration test
```

## 2. `mod`: declaring modules

A module can be written inline:

```rust
mod greetings {
    pub fn hello() -> String {
        String::from("hello")
    }
}

fn main() {
    println!("{}", greetings::hello());
}
```

Or in its own file. In `src/lib.rs`, the line

```rust
mod inventory;
```

tells the compiler: "the module `inventory` lives in `src/inventory.rs` (or
`src/inventory/mod.rs`)". **Files are not picked up automatically**: a `.rs` file nobody declares
with `mod` is simply not compiled.

## 3. `pub`: privacy by default

Everything is **private to its module** unless marked `pub`:

```rust
mod bank {
    pub struct Account {
        pub owner: String,
        balance: u64,          // private: only code inside bank can touch it
    }

    impl Account {
        pub fn open(owner: &str) -> Self {
            Account { owner: owner.to_string(), balance: 0 }
        }

        pub fn deposit(&mut self, amount: u64) {
            self.balance += amount;
        }

        pub fn balance(&self) -> u64 {
            self.balance
        }
    }
}

fn main() {
    let mut account = bank::Account::open("Ana");
    account.deposit(50);
    // account.balance = 1_000_000;  // error[E0616]: field `balance` of struct `Account` is private
    println!("{}", account.balance());
}
```

Private fields are how Rust enforces invariants: outside code must go through your methods.

| Visibility | Who can use it |
| --- | --- |
| (nothing) | the current module and its children |
| `pub(crate)` | anywhere in this crate |
| `pub(super)` | the parent module |
| `pub` | anyone, including other crates |

## 4. Paths and `use`

Items are reached by path, like files in folders:

```rust
crate::pricing::tax::rate();   // absolute, from the crate root
super::helpers::format();      // relative, from the parent module
self::local::thing();          // relative, from this module
```

`use` brings a path into scope so you can write the short name:

```rust
use std::collections::HashMap;
use crate::pricing::tax::{rate, Region};
use std::io::{self, Write};          // io itself and io::Write

let mut map = HashMap::new();
```

> [!TIP]
> Convention: `use` the **parent module** for functions (`use std::fs; fs::read(...)`) and the
> **type itself** for structs, enums and traits (`use std::collections::HashMap;`).

`pub use` re-exports an item, so users of your crate see a simpler API than your file layout:

```rust
// src/lib.rs
mod inventory;
pub use inventory::Inventory;   // users write shop::Inventory, not shop::inventory::Inventory
```

## 5. Dependencies from crates.io

```bash
cargo add serde --features derive
cargo add rand@0.9
```

`cargo add` edits `Cargo.toml`:

```toml
[dependencies]
serde = { version = "1", features = ["derive"] }
rand = "0.9"
```

Versions follow **semantic versioning**: `"1"` means "any 1.x.y". `Cargo.lock` records the exact
versions used, so every build is reproducible. Commit it for applications.

Test-only dependencies go under `[dev-dependencies]` (`cargo add --dev pretty_assertions`).

## 6. Unit tests

Unit tests live **next to the code**, in a module compiled only for `cargo test`:

```rust
pub fn add(a: i32, b: i32) -> i32 {
    a + b
}

fn is_even(n: i32) -> bool {       // private
    n % 2 == 0
}

#[cfg(test)]
mod tests {
    use super::*;                  // everything from the parent, private items included

    #[test]
    fn adds_two_numbers() {
        assert_eq!(add(2, 3), 5);
    }

    #[test]
    fn private_functions_can_be_tested() {
        assert!(is_even(4), "4 should be even");
    }

    #[test]
    #[should_panic(expected = "divide by zero")]
    fn dividing_by_zero_panics() {
        let _ = 1 / std::hint::black_box(0);
    }

    #[test]
    fn parsing_works() -> Result<(), std::num::ParseIntError> {
        let n: i32 = "42".parse()?;   // a test can use ? when it returns Result
        assert_eq!(n, 42);
        Ok(())
    }
}
```

| Macro | Fails when |
| --- | --- |
| `assert!(cond)` | `cond` is false |
| `assert_eq!(left, right)` | they differ; prints both values |
| `assert_ne!(left, right)` | they are equal |
| any of them `, "msg {}", x` | adds your message to the failure |

## 7. Integration tests

Files in `tests/` are compiled as **separate crates** that use your library from the outside,
only through its `pub` API:

```rust
// tests/checkout.rs
use shop::Inventory;

#[test]
fn a_new_inventory_is_empty() {
    assert!(Inventory::new().is_empty());
}
```

That is exactly how the dojo checks your katas: `tests/visible.rs` and the hidden grading tests
are integration tests against your `src/lib.rs`.

## 8. Doc tests

Code blocks in `///` documentation comments are compiled and run by `cargo test`, so examples
never go stale:

```rust
/// Adds two numbers.
///
/// ```
/// assert_eq!(shop::add(2, 2), 4);
/// ```
pub fn add(a: i32, b: i32) -> i32 {
    a + b
}
```

## 9. Running tests

```bash
cargo test                       # everything: unit, integration, doc tests
cargo test parsing               # only tests whose name contains "parsing"
cargo test --test checkout       # only tests/checkout.rs
cargo test -- --nocapture        # show println! output of passing tests
cargo test -- --test-threads=1   # run one at a time
```

Tests run **in parallel** by default, so they must not depend on each other or on shared files.

> [!WARNING]
> **Common mistake:** creating `src/utils.rs` and getting "cannot find function in this scope".
> The file is ignored until some parent declares `mod utils;`.

> [!NOTE]
> **Coming from Java:** a crate is close to a Maven module or a jar, a module to a package,
> `pub` to `public`, and no modifier to package-private, but per module rather than per folder.
> Unit tests sit in the same file instead of a mirrored `src/test/java` tree, which is how
> they can test private functions without reflection.

## Practice

1. Create a library with `cargo new --lib shapes`, put a `Circle` struct in `src/circle.rs`,
   declare it with `mod circle;` and re-export it with `pub use`.
2. Make the radius private and add a constructor that rejects negative values.
3. Write a unit test for the area and an integration test in `tests/` that uses only the public API.
4. Add a doc test to the constructor and run `cargo test` to see all three kinds run.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| a module in its own file | `mod name;` + `src/name.rs` |
| make something public | `pub` (or `pub(crate)`) |
| shorten a path | `use crate::a::b::Thing;` |
| expose a simpler API | `pub use inner::Thing;` |
| add a dependency | `cargo add crate_name` |
| unit tests | `#[cfg(test)] mod tests { use super::*; #[test] fn ... }` |
| expect a panic | `#[should_panic(expected = "...")]` |
| run some tests | `cargo test name_filter` |

**Ready?** Press **Q** in the dojo for the quiz.
