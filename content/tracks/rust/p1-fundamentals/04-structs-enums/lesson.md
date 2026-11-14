# Structs, Enums and Pattern Matching

> Rust has no classes and no inheritance. You describe data with **structs** (this *and* that)
> and **enums** (this *or* that), add behaviour with `impl`, and take data apart with `match`.
> Together they let you make invalid states impossible to represent.

**In this lesson you will learn**

- structs, methods and associated functions (`impl`, `self`, `Self`)
- enums whose variants carry different data
- `match`: patterns, exhaustiveness, ranges, guards and `_`
- `if let` and `let else` for the one-pattern case
- the control-flow expressions you will use with them: `if`, `loop`, `while`, `for`

---

## 1. Structs: grouping data

```rust
struct Player {
    name: String,
    level: u32,
    active: bool,
}

fn main() {
    let ada = Player {
        name: String::from("Ada"),
        level: 3,
        active: true,
    };
    println!("{} is level {}", ada.name, ada.level);
}
```

There is also the **tuple struct** (fields by position) and the **unit struct** (no fields):

```rust
struct Meters(f64);          // a distinct type, not just an f64
struct Marker;               // carries no data
let distance = Meters(12.5);
println!("{}", distance.0);
```

Wrapping a number in its own type (`Meters`, `UserId`) stops you from mixing up values that
happen to have the same representation.

## 2. `impl`: methods and associated functions

```rust
struct Point {
    x: i32,
    y: i32,
}

impl Point {
    /// Associated function: no self. Called with Point::new(...).
    fn new(x: i32, y: i32) -> Self {
        Self { x, y }              // field init shorthand: x: x, y: y
    }

    /// Method: borrows the point to read it.
    fn distance_from_origin(&self) -> f64 {
        f64::from(self.x * self.x + self.y * self.y).sqrt()
    }

    /// Method: borrows the point mutably to change it.
    fn move_by(&mut self, dx: i32, dy: i32) {
        self.x += dx;
        self.y += dy;
    }
}

fn main() {
    let mut p = Point::new(3, 4);
    println!("{}", p.distance_from_origin());   // 5
    p.move_by(1, 1);
}
```

| First parameter | Meaning | Typical use |
| --- | --- | --- |
| `&self` | borrow to read | getters, calculations |
| `&mut self` | borrow to change | setters, updates |
| `self` | take ownership | conversions that consume the value |
| none | associated function | constructors like `new` |

`Self` is the type being implemented, so renaming the struct doesn't break the `impl`.

## 3. Enums: one of several variants, each with its own data

```rust
enum Shape {
    Circle { radius: f64 },
    Square(f64),
    Dot,
}
```

A `Shape` is exactly one of those, and each variant carries only the data that makes sense for
it. A `Dot` has no radius you could forget to ignore, and a `Circle` can't lack one.

> [!NOTE]
> **Coming from Java:** Java's `enum` is a fixed list of constants. Rust's enum is closer to a
> sealed interface with one record per case (`sealed interface Shape permits Circle, Square, Dot`),
> but declared in three lines and checked by `match`.

## 4. `match`: taking values apart

```rust
fn area(shape: &Shape) -> f64 {
    match shape {
        Shape::Circle { radius } => std::f64::consts::PI * radius * radius,
        Shape::Square(side) => side * side,
        Shape::Dot => 0.0,
    }
}
```

- Each line is an **arm**: `pattern => expression`.
- The pattern can **destructure**: `Shape::Square(side)` binds the inner value to `side`.
- `match` is an **expression**: the whole thing produces the value of the arm that ran.
- The arms are tried top to bottom; the first that fits wins.

### Exhaustiveness

`match` must cover every possible value. Add a `Triangle` variant and forget it:

```
error[E0004]: non-exhaustive patterns: `&Shape::Triangle { .. }` not covered
```

Every `match` over `Shape` in the whole program now points at itself. Adding a case can't
silently break code somewhere else.

### Patterns for numbers

```rust
fn classify(n: i32) -> &'static str {
    match n {
        i32::MIN..=-1 => "negative",
        0 => "zero",
        1..=9 => "small",
        10 | 20 | 30 => "round",
        x if x % 2 == 0 => "large even",   // a guard: extra condition
        _ => "large odd",                  // _ matches anything left
    }
}
```

`..=` is an inclusive range, `|` means "or", `if` adds a **guard**, and `_` is the catch-all.

> [!WARNING]
> **Common mistake:** putting `_ =>` first. Arms are tried in order, so everything matches `_`
> and the compiler warns that the other arms are `unreachable pattern`.

## 5. `if let` and `let else`

When only one pattern matters:

```rust
if let Shape::Circle { radius } = &shape {
    println!("a circle of radius {radius}");
}
```

When you need the value and want to leave early otherwise:

```rust
fn radius_of(shape: &Shape) -> f64 {
    let Shape::Circle { radius } = shape else {
        return 0.0;
    };
    *radius
}
```

## 6. Control flow is made of expressions too

```rust
let label = if score >= 50 { "pass" } else { "fail" };   // if produces a value

let mut tries = 0;
let found = loop {                  // loop runs until break
    tries += 1;
    if tries == 3 {
        break tries * 10;           // break can return a value: found = 30
    }
};

while tries > 0 {                   // while: repeat while true
    tries -= 1;
}

for n in 1..=3 {                    // for: over anything iterable
    println!("{n}");                // 1 2 3
}
for name in ["Ada", "Ferris"] {
    println!("{name}");
}
```

| Use | When |
| --- | --- |
| `for x in items` | going over a collection or a range (most of the time) |
| `while cond` | repeating until a condition changes |
| `loop` | repeating until something inside decides to `break`, possibly with a value |

> [!TIP]
> Prefer `for item in &list` to indexing with `list[i]`: no off-by-one, no bounds check panics,
> and it works on any collection.

## Practice

1. Add a `Triangle { base: f64, height: f64 }` variant to `Shape`, read the compiler errors, and
   fix `area`.
2. Write `impl Shape { fn name(&self) -> &'static str }` returning "circle", "square" or "dot".
3. Write `fn grade(score: u32) -> char` with a single `match` on ranges: 90 and up is `'A'`,
   75–89 `'B'`, 50–74 `'C'`, anything else `'F'`.
4. Use `loop` and `break value` to find the first number above 100 that is divisible by 7.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| group named fields | `struct Name { field: Type }` |
| a constructor | `impl Name { fn new(...) -> Self }` |
| a type that is one of several cases | `enum Name { A, B(T), C { f: T } }` |
| handle every case | `match value { Pattern => expr, ... }` |
| handle one case | `if let Pattern = value { ... }` |
| bind or leave early | `let Pattern = value else { return ...; };` |

**Ready?** Press **Q** in the dojo for the quiz.
