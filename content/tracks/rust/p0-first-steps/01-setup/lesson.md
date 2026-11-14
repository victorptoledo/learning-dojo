# Cargo, rustc and the Toolchain

> Before writing Rust you need to know the handful of tools you will use every day.
> The good news: they are few, they always work the same way, and one of them does almost everything.

**In this lesson you will learn**

- what `rustup`, `rustc` and `cargo` are, and which one you actually type
- how a Rust project is laid out, and what `Cargo.toml` says
- the difference between a binary crate and a library crate (every kata is a library)
- the six Cargo commands you will use all the time
- how this dojo builds and checks your code

---

## 1. Three tools, one job each

| Tool | What it does | How often you call it |
| --- | --- | --- |
| `rustup` | installs and updates Rust, keeps several versions side by side | once in a while |
| `rustc` | the compiler: turns `.rs` files into a program | almost never directly |
| `cargo` | build tool, test runner, package manager, project generator | all the time |

Think of `cargo` as the front door. When you run `cargo build`, Cargo works out what needs
compiling, downloads dependencies, and calls `rustc` for you with the right options.

> [!NOTE]
> **Coming from Java:** `rustup` is like SDKMAN or the JDK installer, `rustc` is `javac`, and
> `cargo` is Maven or Gradle with JUnit and the project archetypes built in, in one binary.
> There is no XML and no plugin to configure for the basics.

Check what you have installed:

```
rustup --version
cargo --version
rustc --version
```

On Windows Rust uses the Microsoft linker, so the "Desktop development with C++" workload of
Visual Studio Build Tools must be installed. `rustup` warns you if it is missing.

## 2. Your first project

```
cargo new hello
cd hello
```

Cargo creates:

```
hello/
├── Cargo.toml      the manifest: name, version, edition, dependencies
├── .gitignore      ignores target/
└── src/
    └── main.rs     the code
```

`src/main.rs` already contains a program:

```rust
fn main() {
    println!("Hello, world!");
}
```

- `fn main()` is where every program starts.
- `println!` prints a line. The `!` means it is a **macro**, not a function: macros generate
  code at compile time, which is how `println!` checks your format string before the program runs.

Run it:

```
cargo run
```

```
   Compiling hello v0.1.0 (C:\dev\hello)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.61s
     Running `target\debug\hello.exe`
Hello, world!
```

Everything Cargo produces goes into `target/`. It can get big; it is safe to delete, and it is
ignored by git.

## 3. Reading `Cargo.toml`

```toml
[package]
name = "hello"
version = "0.1.0"
edition = "2024"

[dependencies]
rand = "0.9"
```

- `edition` picks the version of the language rules. Editions only change syntax details;
  code from different editions works together. New projects use the latest one.
- `[dependencies]` lists libraries (called **crates**) from crates.io. `rand = "0.9"` means
  "any 0.9.x version". Cargo downloads them on the next build and records the exact versions
  it chose in `Cargo.lock`.

> [!TIP]
> You rarely edit `[dependencies]` by hand: `cargo add rand` adds the latest version for you.

## 4. Binary crate or library crate

A **crate** is the unit Rust compiles: one program or one library.

| | Binary crate | Library crate |
| --- | --- | --- |
| Starts at | `src/main.rs` | `src/lib.rs` |
| Has `fn main()` | yes | no |
| Produces | an executable | code other crates (and tests) call |
| Created with | `cargo new name` | `cargo new --lib name` |

**Every kata in this dojo is a library.** You write functions in `src/lib.rs`, marked `pub` so
the tests can call them:

```rust
/// Adds two numbers.
pub fn add(a: i32, b: i32) -> i32 {
    a + b
}
```

## 5. Tests

Tests are ordinary functions marked with `#[test]`. Files in the `tests/` folder see your crate
from the outside, exactly as a user of the library would:

```rust
// tests/visible.rs
use kata::add;

#[test]
fn adds_two_numbers() {
    assert_eq!(add(2, 3), 5);
}
```

`cargo test` compiles everything and runs every test:

```
running 1 test
test adds_two_numbers ... ok

test result: ok. 1 passed; 0 failed
```

When a test fails, Rust shows both values:

```
assertion `left == right` failed
  left: 6
 right: 5
```

`left` is what your code returned, `right` is what the test expected.

## 6. The six commands you will use

| Command | What it does | When |
| --- | --- | --- |
| `cargo check` | checks the code compiles, produces nothing | while you write: it is the fastest |
| `cargo build` | compiles into `target/debug` | when you need the executable |
| `cargo run` | builds and runs a binary | to try a program |
| `cargo test` | builds and runs the tests | to check your work |
| `cargo clippy` | the linter: code that works but could be better | before you call it done |
| `cargo fmt` | formats the code in the standard style | any time, it never changes behaviour |

> [!WARNING]
> **Common mistake:** running `cargo run` inside a kata folder. A kata is a library, so there is
> nothing to run: Cargo answers `a bin target must be available for cargo run`.
> Use `cargo test` instead.

## 7. How this dojo uses all of this

1. When you start a kata, the dojo copies its starter into your workspace:
   `Cargo.toml`, `src/lib.rs` with `todo!()` placeholders, and `tests/visible.rs`.
2. You edit `src/lib.rs` in VS Code. rust-analyzer shows errors as you type and lets you
   **▶ Run Test** or **⚙ Debug** the visible tests.
3. When you save, the dojo copies your code plus the **hidden grading tests** into a Docker
   container that has Rust installed, and runs `cargo test` there. Only those results complete
   the kata.

`todo!()` is a macro that compiles anywhere but stops the program when it is reached
("not yet implemented"). It lets a starter compile before you have written anything.

## Practice

1. Run `cargo new playground`, then `cargo run` inside it. Change the message and run it again.
2. In the same project, change `main.rs` so it prints your name on one line and the result of
   `2 + 3` on another (hint: `println!("{}", 2 + 3);`).
3. Run `cargo clippy` and `cargo fmt`. Then break the code on purpose (delete a `;`) and read
   what `cargo check` says.

## Cheat sheet

| I want to... | Run |
| --- | --- |
| create a program | `cargo new name` |
| create a library | `cargo new --lib name` |
| see if it compiles | `cargo check` |
| run the tests | `cargo test` |
| add a dependency | `cargo add crate_name` |
| tidy the code | `cargo fmt` then `cargo clippy` |

**Ready?** Press **Q** in the dojo for the quiz: 5 questions, 4 right to pass.
