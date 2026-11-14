# Borrowing: References (`&` and `&mut`)

> Moving a value into every function that only wants to look at it would be exhausting.
> Borrowing lets code use a value without owning it. One simple rule about borrows is what
> makes Rust free of data races and dangling pointers.

**In this lesson you will learn**

- shared references `&T` and mutable references `&mut T`
- the borrowing rule: many readers **or** one writer
- why a borrow "ends" at its last use
- why references can never dangle
- why `&str` is the best type for text parameters

---

## 1. Shared references: `&T`

```rust
fn length(text: &String) -> usize {
    text.len()
}

fn main() {
    let word = String::from("dojo");
    let n = length(&word);          // lend word to the function
    println!("{word} has {n} letters");   // word is still ours
}
```

- `&word` creates a **reference**: a pointer that is guaranteed to be valid.
- `text: &String` says "I only borrow a `String`, I don't own it".
- When `length` returns, the borrow is over and nothing is dropped: the owner is still `word`.

Through a shared reference you can read but not change:

```rust
fn add_bang(text: &String) {
    text.push('!');   // error[E0596]: cannot borrow `*text` as mutable, as it is behind a `&` reference
}
```

## 2. Mutable references: `&mut T`

```rust
fn add_bang(text: &mut String) {
    text.push('!');
}

fn main() {
    let mut word = String::from("hey");   // the owner must be mut
    add_bang(&mut word);                  // and the call must say &mut
    println!("{word}");                   // hey!
}
```

Mutation is visible in three places: the variable (`let mut`), the parameter (`&mut String`)
and the call (`&mut word`). Nobody changes your data behind your back.

## 3. The borrowing rule

At any moment a value can have **either**

- any number of shared references (`&T`), **or**
- exactly one mutable reference (`&mut T`),

**never both at once.** Readers can share; a writer needs to be alone.

```rust
let mut list = vec![1, 2, 3];
let first = &list[0];       // shared borrow of list
list.push(4);               // needs a mutable borrow
println!("{first}");        // the shared borrow is still in use here
```

```
error[E0502]: cannot borrow `list` as mutable because it is also borrowed as immutable
 --> src/main.rs:3:1
  |
2 | let first = &list[0];
  |              ---- immutable borrow occurs here
3 | list.push(4);
  | ^^^^^^^^^^^^ mutable borrow occurs here
4 | println!("{first}");
  |            ------- immutable borrow later used here
```

This is not pedantry. `push` may need more room and **move the whole vector** to a new place in
memory. `first` would then point at freed memory. In C++ that is undefined behaviour that might
crash months later; in Rust it doesn't compile.

Two mutable borrows at once are refused for the same reason:

```rust
let mut score = 0;
let a = &mut score;
let b = &mut score;   // error[E0499]: cannot borrow `score` as mutable more than once at a time
*a += 1;
```

## 4. A borrow ends at its last use

The compiler tracks where each reference is **last used**, not where its block ends. This works:

```rust
let mut list = vec![1, 2, 3];
let first = list[0];        // copies the i32, no borrow kept
let total: i32 = list.iter().sum();   // shared borrow, ends right here
list.push(first + total);   // fine: no shared borrow is alive any more
```

> [!TIP]
> Many borrow errors disappear when you **reorder** code: finish reading before you start
> writing, or copy out the small value you need (`let first = list[0];` instead of
> `let first = &list[0];`).

## 5. References never dangle

A reference can't outlive the value it points to:

```rust
fn longest_name() -> &String {
    let name = String::from("Ferris");
    &name        // name is dropped when the function ends
}
```

The compiler refuses (`error[E0106]: missing lifetime specifier`) and suggests returning the
owned value instead:

```rust
fn longest_name() -> String {
    String::from("Ferris")   // ownership moves to the caller
}
```

Rule of thumb: a function can return a reference only to something it was **given**, never to
something it **created**.

## 6. `&str`: the right type for text parameters

`&str` is a borrowed view into text: it can point into a `String`, or at a literal compiled into
the program. Prefer it for parameters:

```rust
fn greet(name: &str) -> String {
    format!("Hello, {name}!")
}

fn main() {
    let owned = String::from("Ada");
    println!("{}", greet(&owned));   // &String turns into &str automatically
    println!("{}", greet("Ferris")); // a literal already is &str
}
```

With `&String` as the parameter type, `greet("Ferris")` would not compile. The same idea applies
to lists: take `&[i32]` (a slice) instead of `&Vec<i32>`.

> [!NOTE]
> **Coming from Java:** every object variable in Java is a reference, and any number of them can
> modify the same object, from any thread. Rust references are explicit (`&`, `&mut`), checked at
> compile time, and the "one writer" rule is what later makes concurrent code safe.

## Practice

1. Fix the `length(text: String)` function from the last lesson so you can call it twice on the
   same variable.
2. Write `fn shout(text: &mut String)` that turns the text to uppercase in place
   (hint: `*text = text.to_uppercase();`).
3. Reproduce the `push` error from section 3, then fix it by reordering the lines.
4. Write `fn first_word(text: &str) -> &str` that returns the first word (hint:
   `text.split_whitespace().next()` and `unwrap_or("")`).

## Cheat sheet

| I want to... | Parameter type | Call |
| --- | --- | --- |
| read a value | `&T` (`&str`, `&[T]` for text and lists) | `f(&value)` |
| change a value in place | `&mut T` | `f(&mut value)` (value must be `let mut`) |
| keep or consume the value | `T` | `f(value)` (it moves) |

| Allowed at the same time? | |
| --- | --- |
| many `&T` | yes |
| one `&mut T` alone | yes |
| `&mut T` plus anything else | no |

**Ready?** Press **Q** in the dojo for the quiz.
