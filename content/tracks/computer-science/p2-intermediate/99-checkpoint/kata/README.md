# Bytecode Compiler

Write a compiler and a virtual machine for a small imperative language, in Java: `Compiler.compile`
turns source text into bytecode for a stack machine, and `Vm.run` executes it. It combines the
phase: lexing and parsing, compiling an expression tree in post-order, control flow as jumps (the
same branches your toy machine executed), and variables as numbered slots, like the local variable
table of the JVM. `Op`, `Instr`, `CompileError` and `VmError` are already written.

## The language

```text
# comments run to the end of the line
let n = 10          # declare a variable (once)
let total = 0
while n > 0 {
    if n % 2 == 0 { total = total + n } else { print n }
    n = n - 1
}
print total
```

```text
program   := { statement (NEWLINE | ";") }        blank lines and extra separators allowed
statement := "let" IDENT "=" expr
           | IDENT "=" expr                          the variable must be declared
           | "print" expr
           | "while" expr block
           | "if" expr block [ "else" ( block | if-statement ) ]
block     := "{" { statement (NEWLINE | ";") } "}"   a statement may also end right before "}"
expr      := sum [ ("<" | "<=" | ">" | ">=" | "==" | "!=") sum ]   comparisons do not chain
sum       := product { ("+" | "-") product }
product   := unary { ("*" | "/" | "%") unary }
unary     := "-" unary | atom
atom      := NUMBER | IDENT | "(" expr ")"
```

- Values are Java `long`s with Java semantics (overflow wraps, `/` truncates towards zero).
  Comparisons give 1 or 0, and `while` and `if` treat 0 as false and anything else as true.
- Keywords: `let`, `print`, `while`, `if`, `else`; they cannot be variable names. `else` must be
  on the same line as the `}` before it.
- There is one global scope: a `let` inside a block declares a global variable, and is compiled
  once even if the block runs many times.

## The bytecode

Variables get **slots** 0, 1, 2, … in the order of their `let`s. Jumps use **absolute**
instruction indexes; the program ends when the pc reaches `code.size()`. The compiler must emit
exactly this shape, which is what the hidden tests compare:

| Source | Bytecode |
| --- | --- |
| a number / a variable | `PUSH n` / `LOAD slot` |
| `a op b` | code of `a`, code of `b`, then `ADD` `SUB` `MUL` `DIV` `MOD` `LT` `LE` `GT` `GE` `EQ` or `NE` |
| `-a` | code of `a`, `NEG` |
| `let x = e`, `x = e` | code of `e`, `STORE slot` |
| `print e` | code of `e`, `PRINT` |
| `while c { body }` | *start:* code of `c`, `JUMP_IF_ZERO end`, body, `JUMP start` *end:* |
| `if c { a }` | code of `c`, `JUMP_IF_ZERO end`, a *end:* |
| `if c { a } else { b }` | code of `c`, `JUMP_IF_ZERO else`, a, `JUMP end` *else:* b *end:* |

```java
Compiler.compile("print 1 + 2 * 3")
// [PUSH 1, PUSH 2, PUSH 3, MUL, ADD, PRINT]
```

Hint: emit `JUMP_IF_ZERO` with a placeholder target and **patch** it when you know where the
block ends (backpatching).

## Errors

`CompileError(message, line)` with the 1-based line of the problem for: an unknown character, a
number that does not fit in a `long`, a syntax error (reported at the line of the token where it
was found; a missing `}` at the end of input is reported at the last line), a use or assignment of
an undeclared variable (including `let x = x + 1`), a second `let` of the same name, and a chained
comparison.

`Vm.run(code)` returns the printed values in order, and `Vm.run(code, maxSteps)` stops with
`VmError` after `maxSteps` instructions. It also throws `VmError` for division or modulo by zero,
popping an empty stack, and a jump outside `0..code.size()`.

## What the hidden tests check

The exact bytecode of expressions, variables, comparisons, `while` and `if`/`else`; programs
(Fibonacci, Euclid's GCD, primes with nested loops, else-if chains, 64-bit arithmetic); fifteen
compile errors and their lines; VM errors and the step limit; hand-written bytecode; 500 random
expressions against Java; and a loop of a million iterations within a time limit.

Run the visible tests with `mvn test` or from your IDE.
