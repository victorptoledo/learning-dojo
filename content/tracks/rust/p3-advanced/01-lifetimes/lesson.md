# Lifetimes: How Long a Reference Lives

> Every reference in Rust points at something that must still exist when the reference is used.
> The compiler checks this with **lifetimes**: names for "the region of code where this borrow is
> valid". Most of the time it works them out alone. When it can't, you write a few `'a` to
> describe how inputs and outputs are related. You never change how long anything lives; you only
> tell the compiler what you already know.

**In this lesson you will learn**

- what problem lifetimes solve: dangling references
- how to read and write lifetime annotations like `&'a str`
- the three elision rules that let you skip annotations most of the time
- lifetimes in structs that hold references
- `'static`, and why "add `'static`" is rarely the right fix
- how to choose between borrowing and owning when lifetimes get in the way

---

## 1. The problem: dangling references

```rust
fn main() {
    let r;
    {
        let x = 5;
        r = &x;          // error[E0597]: `x` does not live long enough
    }                    // x is dropped here...
    println!("{r}");     // ...but r would still point at it
}
```

In C this compiles and reads garbage. Rust's **borrow checker** compares how long `x` lives with
how long `r` is used, and refuses.

## 2. When the compiler needs your help

This function returns one of its two inputs:

```rust
fn longest(a: &str, b: &str) -> &str {   // error[E0106]: missing lifetime specifier
    if a.len() >= b.len() { a } else { b }
}
```

Looking only at the signature (the compiler never looks into other functions' bodies), it cannot
tell whether the result borrows from `a`, from `b`, or from neither. So you say it:

```rust
fn longest<'a>(a: &'a str, b: &'a str) -> &'a str {
    if a.len() >= b.len() { a } else { b }
}
```

Read it as: "for some region `'a` in which **both** `a` and `b` are valid, the result is valid in
`'a` too". In practice the result may not outlive the **shorter** of the two inputs:

```rust
let outer = String::from("a long string");
let result;
{
    let inner = String::from("short");
    result = longest(&outer, &inner);
}                          // inner is dropped
println!("{result}");      // error[E0597]: `inner` does not live long enough
```

Even though `outer` is longer at run time, the signature promised only the shorter region.

### Lifetimes describe relationships

If the result only ever comes from the first parameter, say exactly that and the second one is
free:

```rust
fn first_or_default<'a>(text: &'a str, _hint: &str) -> &'a str {
    text.split(',').next().unwrap_or(text)
}
```

## 3. Elision: why you rarely write them

You already wrote many functions returning references without any `'a`. The compiler applies
three **elision rules**:

1. Each reference parameter gets its own lifetime.
2. If there is **exactly one** input lifetime, it is given to every output reference.
3. If one of the parameters is `&self` or `&mut self`, its lifetime is given to the outputs.

```rust
fn first_word(text: &str) -> &str { ... }          // rule 2: output borrows from text
impl Inventory {
    fn name_of(&self, id: u32) -> &str { ... }    // rule 3: output borrows from self
}
fn longest(a: &str, b: &str) -> &str { ... }      // two inputs, no self: you must annotate
```

## 4. Structs that hold references

A struct that stores a reference must say how long that reference lives, because the struct must
not outlive the data:

```rust
struct Excerpt<'a> {
    text: &'a str,
}

impl<'a> Excerpt<'a> {
    fn first_sentence(&self) -> &'a str {
        self.text.split('.').next().unwrap_or(self.text)
    }
}

fn main() {
    let novel = String::from("Call me Ishmael. Some years ago...");
    let excerpt = Excerpt { text: &novel };
    println!("{}", excerpt.first_sentence());
}   // excerpt is dropped before novel: fine
```

This is how parsers and tokenizers avoid copying: the tokens are slices of the input.

```rust
struct Token<'src> {
    kind: Kind,
    text: &'src str,
}

fn tokenize(source: &str) -> Vec<Token<'_>> { ... }   // '_ : "the elided lifetime"
```

## 5. `'static`

`'static` means "valid for the whole program". String literals have it, because they are stored
in the binary:

```rust
let greeting: &'static str = "hello";
```

Trait bounds such as `T: 'static` (required by `thread::spawn`) mean something slightly
different: the type **holds no short-lived borrows**. An owned `String` or `Vec<u8>` satisfies
`'static`; a `&str` pointing into a local variable does not.

> [!WARNING]
> **Common mistake:** the compiler complains about a lifetime and you add `'static` everywhere
> until it compiles. That only works by forcing callers to pass literals or leaked memory. Usually
> the real fix is to **own** the data (`String` instead of `&str`) or to shorten the borrow.

## 6. Reading lifetime errors

| Error | Usually means | Typical fix |
| --- | --- | --- |
| E0597 `does not live long enough` | the data dies while still borrowed | move the data to an outer scope, or own a copy |
| E0106 `missing lifetime specifier` | a returned reference could come from several inputs | annotate which input it borrows from |
| E0515 `cannot return reference to local variable` | you return a borrow of something created inside the function | return the owned value (`String`, `Vec`) |
| E0499 / E0502 | overlapping borrows | end one borrow before starting the other |

```rust
fn make_greeting(name: &str) -> &str {
    let text = format!("hello {name}");
    &text            // error[E0515]: cannot return reference to local variable `text`
}

fn make_greeting(name: &str) -> String {   // fix: return the owned String
    format!("hello {name}")
}
```

## 7. Borrow or own?

| Situation | Prefer |
| --- | --- |
| a function only reads its input | borrow: `&str`, `&[T]`, `&T` |
| the result is a part of the input | borrow, with a lifetime if needed |
| the result is newly built | own: `String`, `Vec<T>` |
| a struct lives long or crosses threads | own its fields |
| a short-lived view over big data (parser, iterator) | a struct with a lifetime |

> [!TIP]
> Start with owned fields in structs. Introduce a lifetime parameter only when copying would
> really cost too much. A struct with a lifetime "infects" every type that contains it.

> [!NOTE]
> **Coming from Java:** the garbage collector keeps an object alive as long as anything points at
> it, so this question never comes up. Rust has no GC, so the compiler proves at build time that
> no reference outlives its data. Lifetimes are the vocabulary of that proof; they produce no code
> and cost nothing at run time.

## Practice

1. Write `fn longest<'a>(a: &'a str, b: &'a str) -> &'a str` and reproduce the E0597 error by
   using the result after the shorter-lived string is dropped. Then fix it by moving the string
   to the outer scope.
2. Write `struct Line<'a> { number: usize, text: &'a str }` and a function that turns a `&str`
   into a `Vec<Line<'_>>` with `lines().enumerate()`.
3. Try to return `&String` from a function that builds the string inside. Read E0515 and fix it.
4. Explain, with the elision rules, why `fn trim_both(a: &str) -> &str` needs no annotation.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| tie the output to the inputs | `fn f<'a>(x: &'a str, y: &'a str) -> &'a str` |
| tie the output to one input only | `fn f<'a>(x: &'a str, y: &str) -> &'a str` |
| a struct holding a reference | `struct S<'a> { field: &'a str }` |
| its impl block | `impl<'a> S<'a> { ... }` |
| let the compiler fill it in | `S<'_>` |
| a literal string type | `&'static str` |
| data that can go to a thread | owned values (`T: 'static`) |

**Ready?** Press **Q** in the dojo for the quiz.
