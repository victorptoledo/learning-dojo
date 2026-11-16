# Expression Parser

Write the front end of a tiny language and a tree-walking interpreter for it, in `expr.py`: a
**lexer**, a **recursive-descent parser** that builds an **AST**, a printer for the tree and an
**evaluator**. The token and node dataclasses, the two exceptions and `BUILTINS` are already
written.

## The language

```text
expr   := term   (("+" | "-") term)*
term   := unary  (("*" | "/" | "%") unary)*
unary  := "-" unary | power
power  := atom ("^" unary)?
atom   := NUMBER | IDENT | IDENT "(" [expr ("," expr)*] ")" | "(" expr ")"
```

So `+ - * / %` are left-associative, `^` is right-associative and binds tighter than unary minus
on its left (`-2 ^ 2` is −4) while allowing one on its right (`2 ^ -1` is 0.5).

## The lexer

`tokenize(source) -> list[Token]`. A `Token` has a `kind`, its `text` and the `pos` of its first
character:

| Kind | Matches |
| --- | --- |
| `"number"` | ASCII digits, optionally followed by `.` and more digits: `7`, `2.5` (not `3.` or `.5`) |
| `"ident"` | an ASCII letter or `_`, then letters, digits or `_` |
| `"op"` | one of `+ - * / % ^ ( ) ,` |
| `"eof"` | always the last token, with `pos = len(source)` |

Whitespace separates tokens. Any other character raises `ParseError(message, pos)`.

## The parser

`parse(source) -> Node` returns the tree of the **whole** input, built from `Num` (an `int`, or a
`float` when the literal has a point), `Var`, `Unary("-", operand)`, `Binary(op, left, right)` and
`Call(name, args)` (args a tuple). On bad input it raises `ParseError` whose `pos` is the position
of the **token where the problem was found** (the `eof` token for a premature end), and whose
message ends with `at position N` (the constructor already does that).

`to_sexpr(node) -> str` prints a tree as nested parentheses, numbers with `repr`:

```python
to_sexpr(parse("a - b - c * 2 ^ 3 ^ 2"))   # "(- (- a b) (* c (^ 2 (^ 3 2))))"
to_sexpr(parse("-max(x, 2.5)"))            # "(neg (call max x 2.5))"
to_sexpr(parse("f()"))                     # "(call f)"
```

## The evaluator

`evaluate(node, env=None)` computes the value with Python's arithmetic (`/` is true division, `%`
Python's modulo, `^` is `**`), reading variables from `env` and functions from `BUILTINS` (`abs`,
`min`, `max`, `sqrt`). It raises `EvalError` for an unknown variable or function, a division or
modulo by zero, `0 ^` a negative number, and a builtin that rejects its arguments (`sqrt(-1)`,
`min()`).

## What the hidden tests check

Token positions, number and character rules, about twenty trees covering precedence,
associativity, unary minus, calls and nesting, the position of eleven kinds of syntax error,
evaluation and its errors, and 1,500 random expressions compared with Python's own evaluation.

Run the visible tests with `uv run pytest`.
