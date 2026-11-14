Write the function `greet` in src/lib.rs. It receives a name and returns a
welcome message.

What it must return:

  greet("Ferris")     ->  "Welcome to the dojo, Ferris!"
  greet("  Ferris ")  ->  "Welcome to the dojo, Ferris!"     (spaces around the name are ignored)
  greet("")           ->  "Welcome to the dojo, stranger!"   (no name: greet a stranger)
  greet("   ")        ->  "Welcome to the dojo, stranger!"   (only spaces counts as no name)

What you start with:

  pub fn greet(name: &str) -> String {
      todo!("greet {name}")
  }

- `name: &str` is a borrowed piece of text (you read it, you don't own it).
- `-> String` means the function gives back a new, owned text.
- The last expression of a function, without `;`, is its return value.

Delete the `todo!(...)` line and write the body. Tools you will need:

- `name.trim()` gives the text without the spaces at both ends.
- `text.is_empty()` is true when the text has no characters.
- `format!("Welcome to the dojo, {who}!")` builds a String, filling `{who}`
  with the variable `who`.
- `if condition { a } else { b }` is an expression: it produces `a` or `b`,
  so you can write `let who = if ... { ... } else { ... };`.

There are 5 tests: 1 in tests/visible.rs (you can read it) and 4 hidden ones that
check the rules above.
