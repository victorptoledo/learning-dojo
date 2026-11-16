# Turing Machines, Grammars and Computability

Finite automata cannot count. This lesson climbs the ladder of more powerful machines until it
reaches the most powerful one we know, the Turing machine, and then shows something surprising
and practical: there are precise, simple questions about programs that **no** program can answer.
That result shapes compilers, linters, security scanners and verification tools you use every day.

## One step up: stacks and context-free languages

Add a **stack** to a finite automaton and you get a **pushdown automaton** (PDA). It can match
nested structure: push on `(`, pop on `)`, accept if the stack is empty at the end. PDAs recognise
exactly the **context-free languages**, the ones described by **context-free grammars**:

```text
S → ( S ) S | ε          balanced parentheses
S → a S b | ε            aⁿbⁿ
```

This is why the grammars of the compilers lesson are context free and why parsers are, in
effect, PDAs: the call stack of a recursive-descent parser is the automaton's stack. Context-free
grammars still cannot express everything: { aⁿbⁿcⁿ } is not context free, and neither is "every
variable is declared before use", which is why compilers check that in a later phase.

## The Chomsky hierarchy

| Level | Grammar | Machine | Example |
| --- | --- | --- | --- |
| Type 3 | regular | finite automaton | identifiers, `a*b*` |
| Type 2 | context-free | pushdown automaton | balanced parentheses, arithmetic, most programming-language syntax |
| Type 1 | context-sensitive | linear-bounded automaton | aⁿbⁿcⁿ |
| Type 0 | unrestricted | Turing machine | anything computable |

Each level strictly contains the one above it.

## Turing machines

Alan Turing (1936) modelled a person computing with pencil and paper: an infinite **tape** of
cells, a **head** that reads and writes one cell and moves left or right, and a finite **control**
(a state machine). Each step: in state q reading symbol s, write s', move L or R, go to state q'.
A machine can **accept**, **reject**, or **run forever**.

That is all, and it is enough:

- **Church-Turing thesis**: everything that can be computed by any mechanical procedure can be
  computed by a Turing machine. Lambda calculus, register machines, cellular automata and every
  real programming language turned out to be equivalent. It is a thesis (about the intuitive idea
  of "computable"), not a theorem, but no counterexample has ever been found.
- **Universal Turing machine**: a Turing machine U that takes the description of any machine M and
  an input x and simulates M on x. That is the stored-program computer: an interpreter. Every
  computer and every interpreter is a universal machine with finite memory.
- A system is **Turing complete** if it can simulate a Turing machine (given unbounded memory):
  Java, Python, JavaScript, C#, Go, SQL with recursive CTEs, C++ templates, Excel with LAMBDA,
  even Conway's Game of Life. Regular expressions (without backreferences) and plain JSON are not.

A Turing machine **decides** a language if it always halts with the right answer. It only
**recognises** it if it accepts the strings in it but may run forever on the others.

## The halting problem

**Is there a program H that, given any program P and input x, always halts and says whether P
halts on x?** No.

Proof by **diagonalisation** (the same trick Cantor used to show the reals are uncountable).
Suppose H exists. Build:

```text
D(P):  if H(P, P) says "halts" then loop forever
       else halt
```

Now run `D(D)`. If H says D(D) halts, D loops forever. If H says D(D) loops, D halts. Either way
H is wrong about D(D), so H cannot exist.

The halting problem is **recognisable** (run P and see if it stops) but not **decidable**. And
since there are uncountably many languages but only countably many programs, almost all languages
are not even recognisable.

## Reductions and Rice's theorem

To show a new problem B is undecidable, **reduce** a known undecidable problem A to it: show that a
solver for B would give a solver for A. For example, "does P ever print `hello`?" is undecidable:
given any P and x, build P' that runs P on x and then prints `hello`; P' prints `hello` exactly
when P halts on x.

**Rice's theorem** generalises this: **every non-trivial property of the behaviour of programs is
undecidable**. "Non-trivial" means some programs have it and some do not; "behaviour" means the
function computed, not the text. So these are all undecidable for a Turing-complete language:

- Does this function ever return null? Does it ever throw?
- Are these two programs equivalent? (So no perfect optimiser exists.)
- Is this code dead? Does this loop terminate? Is this program a virus?

Syntactic properties ("is this function longer than 200 lines?", "does it call `eval`?") are
decidable: you just read the text.

## Living with undecidability

Undecidable does not mean hopeless; it means every tool must give something up:

- **Sound but incomplete** (no false negatives, some false positives): type systems, Java's
  definite assignment rules, Rust's borrow checker, Kotlin's and C#'s null safety. They reject some
  correct programs to guarantee that accepted ones are safe.
- **Complete but unsound** (no false positives, some misses): bug finders and linters that only
  report what they are sure of.
- **Restrict the language**: total languages (Coq, Agda, Lean) where every program terminates;
  SQL without recursion; configuration languages such as Dhall or CUE; eBPF programs, which the
  Linux kernel's verifier accepts only if they provably terminate.
- **Bound the problem**: model checkers (TLA+, Alloy, SPIN) explore all states up to a size; tests
  explore some inputs; production systems use **timeouts** and watchdogs because no one can prove
  the request will finish.

## Summary

- A stack turns a finite automaton into a pushdown automaton, which recognises context-free
  languages: the syntax of programming languages.
- The Chomsky hierarchy orders languages by the machine they need; Turing machines are at the top.
- The Church-Turing thesis says Turing machines capture all computation; every real language is
  Turing complete, and every interpreter is a universal machine.
- The halting problem is undecidable (diagonalisation); reductions and Rice's theorem extend this
  to every non-trivial question about a program's behaviour.
- Real tools cope by being sound but incomplete, incomplete but precise, restricted, or bounded.

## Further reading

- Sipser, *Introduction to the Theory of Computation*, 3rd ed., chapters 2 "Context-Free Languages", 3 "The Church-Turing Thesis", 4 "Decidability" and 5 "Reducibility"
- Hopcroft, Motwani and Ullman, *Introduction to Automata Theory, Languages, and Computation*, 3rd ed., chapters 5–9
- Alan Turing, "On Computable Numbers, with an Application to the Entscheidungsproblem" (1936)
- Charles Petzold, *The Annotated Turing*
- MIT OCW 18.404J Theory of Computation (Fall 2020), lectures 4–10: <https://ocw.mit.edu/courses/18-404j-theory-of-computation-fall-2020/>
- Stanford Encyclopedia of Philosophy, "The Church-Turing Thesis": <https://plato.stanford.edu/entries/church-turing/>
