# Declarative Macros with `macro_rules!`

> You have called macros since your first program: `println!`, `vec!`, `format!`, `assert_eq!`.
> The `!` marks code that writes code. A function takes values; a macro takes **tokens** and
> expands into Rust before the type checker runs. That is how `println!` accepts any number of
> arguments and checks the format string at compile time.

**In this lesson you will learn**

- what a macro can do that a function cannot
- writing `macro_rules!` with matchers and fragment specifiers
- repetitions: `$( ... ),*` and `$( ... );*`
- recursion, hygiene and `$crate`
- exporting a macro and reading its expansion
- what procedural macros (`#[derive]`, attributes) are, and when you meet them

---

## 1. Why macros

A function has a fixed number of typed parameters. A macro can:

- take any number of arguments (`vec![1, 2, 3]`);
- accept syntax that is not an expression (`matches!(x, Some(1..=9))`);
- generate items: structs, `impl` blocks, tests;
- see the source text (`stringify!`, `line!`, `file!`).

> [!NOTE]
> Reach for a function first. Macros are harder to read, to debug and to document. Write one
> when a function genuinely cannot express the idea, or to remove real repetition.

## 2. A first `macro_rules!`

```rust
macro_rules! square {
    ($x:expr) => {
        $x * $x
    };
}

let n = square!(2 + 1);   // 9, not 2 + 1 * 2 + 1 = 5
```

A macro is a list of **rules**: `(matcher) => { transcriber };`. The first rule whose matcher
fits the input wins. `$x:expr` captures one expression. Because `$x` is captured as a whole
expression, the expansion keeps its grouping: no C-style parenthesis bugs.

Common **fragment specifiers**:

| Specifier | Matches | Example |
| --- | --- | --- |
| `expr` | an expression | `2 + 1`, `foo()` |
| `ident` | an identifier | `total`, `Meters` |
| `ty` | a type | `Vec<u8>`, `&str` |
| `literal` | a literal | `42`, `"hi"` |
| `pat` | a pattern | `Some(_)`, `1..=9` |
| `tt` | one token tree | anything, one token or a `( ... )` group |
| `block` | a `{ ... }` block | |

## 3. Repetitions

`$( ... ),*` repeats a part zero or more times, separated by commas; `+` means one or more.
The same `$( ... )*` in the transcriber repeats the output once per match.

```rust
macro_rules! set {
    ( $( $item:expr ),* $(,)? ) => {{
        let mut set = std::collections::HashSet::new();
        $( set.insert($item); )*
        set
    }};
}

let primes = set![2, 3, 5, 7,];    // the $(,)? accepts a trailing comma
```

Two details worth copying:

- `{{ ... }}`: the outer braces belong to the rule, the inner ones make the expansion a **block
  expression**, so `let mut set` does not leak into the caller.
- `$(,)?` accepts an optional trailing comma, like the standard macros do.

Several rules give a macro several shapes. `vec!` has one for a list and one for `[value; n]`:

```rust
macro_rules! my_vec {
    () => { Vec::new() };
    ( $value:expr ; $count:expr ) => { vec![$value; $count] };
    ( $( $item:expr ),+ $(,)? ) => { <[_]>::into_vec(Box::new([ $( $item ),+ ])) };
}
```

## 4. Generating items

Macros can write whole definitions. This removes the boilerplate of many similar newtypes:

```rust
macro_rules! unit {
    ( $name:ident, $suffix:literal ) => {
        #[derive(Debug, Clone, Copy, PartialEq, PartialOrd)]
        pub struct $name(pub f64);

        impl std::fmt::Display for $name {
            fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
                write!(f, "{}{}", self.0, $suffix)
            }
        }
    };
}

unit!(Meters, "m");
unit!(Seconds, "s");
```

## 5. Recursion

A rule may call the macro again with less input. This counts its arguments at compile time:

```rust
macro_rules! count {
    () => { 0usize };
    ( $head:tt $( $tail:tt )* ) => { 1usize + count!( $( $tail )* ) };
}

const N: usize = count!(a b c);   // 3
```

The compiler limits recursion depth (128 by default), so keep recursive macros small.

## 6. Hygiene and `$crate`

Variables created inside a macro do not clash with the caller's:

```rust
macro_rules! double_it {
    ($x:expr) => {{ let value = $x; value * 2 }};
}

let value = 10;
let n = double_it!(value + 1);   // 22: the macro's `value` is a different variable
```

Hygiene covers local variables, not paths. A macro used from another crate must name its own
items with `$crate::`, which always points at the crate that defined the macro:

```rust
#[macro_export]                       // makes the macro available as my_crate::shout!
macro_rules! shout {
    ($text:expr) => { $crate::loud($text) };
}
```

## 7. Debugging and procedural macros

- `cargo expand` (a cargo plugin: `cargo install cargo-expand`) prints the code after expansion.
- `trace_macros!` and `log_syntax!` exist on nightly only.
- A matcher that fits nothing gives `no rules expected the token ...`: check separators and the
  fragment specifiers first.

**Procedural macros** are Rust functions that run in the compiler and turn a token stream into
another. They live in their own crate (`proc-macro = true`) and come in three kinds:

| Kind | Looks like | Examples |
| --- | --- | --- |
| derive | `#[derive(Serialize)]` | `serde`, `thiserror` |
| attribute | `#[tokio::main]`, `#[test]` | `tokio`, `tracing::instrument` |
| function-like | `sql!(SELECT ...)` | `sqlx::query!` |

You will **use** them constantly and rarely write one; the `syn` and `quote` crates are the
starting point when you do.

## Practice

1. Write `max!(a, b, c, ...)` that works for one or more expressions, using recursion.
2. Write `hashmap!{ "a" => 1, "b" => 2 }` with a trailing comma allowed.
3. Use `unit!` above to make `Celsius` and print `Celsius(21.5)`.
4. Call `square!(next())` where `next` prints something. How many times does it print? Fix the
   macro so it evaluates its argument once.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| a macro | `macro_rules! name { (matcher) => { expansion }; }` |
| capture an expression / name / type | `$x:expr` / `$n:ident` / `$t:ty` |
| zero or more, comma separated | `$( $x:expr ),*` |
| allow a trailing comma | `$(,)?` |
| expand to a block | `=> {{ ... }}` |
| export it | `#[macro_export]` and `$crate::` inside |
| see the expansion | `cargo expand` |

**Ready?** Press **Q** in the dojo for the quiz.
