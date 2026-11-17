Parse a formula into a tree, and print the tree back fully parenthesised. `expr.hpp` declares
`parse` and `to_string`; design `Expr` and write them.

The grammar, from the loosest operator to the tightest:

```text
expression := term (('+' | '-') term)*        left to right: 8 - 3 - 2 is (8 - 3) - 2
term       := unary (('*' | '/') unary)*      left to right
unary      := '-' unary | power               -2 ^ 2 is -(2 ^ 2)
power      := primary ('^' unary)?            right to left: 2 ^ 3 ^ 2 is 2 ^ (3 ^ 2)
primary    := number | name | name '(' arguments ')' | '(' expression ')'
arguments  := nothing, or expression (',' expression)*
```

- A number is one or more digits, optionally followed by `.` and one or more digits (`42`,
  `3.25`; not `.5` or `1.`). A name is a letter or `_` followed by letters, digits or `_`.
- Spaces and tabs may go between any two tokens.

`to_string` prints numbers in their shortest form (`std::format("{}", x)`), a negation as
`(-x)`, a binary operation as `(a + b)` with one space around the operator, and a call as
`max(a, (b + 1))`, arguments separated by `", "`. Parentheses of the text are not kept: they
only shape the tree.

When the text is not a formula, `parse` returns the `Error` of the first problem, with the
position (counting from 0) of the character where it is:

| Kind | When | Example |
| --- | --- | --- |
| `UnexpectedEnd` | the text ends where something was needed (position = length of the text) | `1 + ` → 4 |
| `UnexpectedToken` | a digit, letter, `_` or one of `+ - * / ^ ( ) ,` where it cannot go | `1 2` → 2, `max(1,)` → 6 |
| `UnexpectedCharacter` | any other character | `1 $ 2` → 2, `1.` → 1 |

Each node should remember its position in the text (the start of a number or name, the `-`
of a negation, the operator of a binary operation): the next stages report errors there.
