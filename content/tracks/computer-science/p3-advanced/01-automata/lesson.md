# Automata, Regular Languages and Regular Expressions

Theory of computation asks what machines can compute. It starts with the weakest useful
machine, the **finite automaton**: a machine with a fixed, finite memory that reads its input
once, left to right. It sounds like a toy, but it is the model behind every lexer, every
regular expression engine, network packet filters, protocol state machines and the hardware state
machines of the logic lesson.

## Languages

- An **alphabet** Σ is a finite set of symbols, for example {0, 1} or all Unicode characters.
- A **string** is a finite sequence of symbols; ε is the empty string.
- A **language** is a set of strings: "all binary numbers divisible by 3", "all valid
  identifiers", "all balanced parenthesis strings".

A machine **recognises** a language if it accepts exactly the strings in it. The question "which
languages can this kind of machine recognise?" classifies both machines and problems.

## Deterministic finite automata (DFA)

A DFA has a finite set of **states**, a **start state**, a set of **accepting states**, and a
**transition function**: for each state and symbol, exactly one next state. It reads the input
one symbol at a time and accepts if it ends in an accepting state.

A DFA for binary numbers divisible by 3, where the state is the remainder so far (reading bit *b*
turns remainder *r* into (2r + b) mod 3):

| State | on 0 | on 1 |
| --- | --- | --- |
| → **r0** (accepting) | r0 | r1 |
| r1 | r2 | r0 |
| r2 | r1 | r2 |

`110` (6): r0 →1 r1 →1 r0 →0 r0: accepted. `111` (7): r0 → r1 → r0 → r1: rejected.

A DFA runs in **O(n)** time with **O(1)** memory, whatever the language: one table lookup per
symbol. That is as fast as reading the input.

## Nondeterministic finite automata (NFA)

An NFA may have **several** transitions for the same symbol, and **ε-transitions** that move
without reading anything. It accepts if **some** path ends in an accepting state. NFAs are often
much smaller and easier to build than DFAs, for example "the third symbol from the end is 1".

Two facts make NFAs practical:

- **Simulation**: track the **set** of states the NFA could be in. After each symbol, compute the
  next set (and follow ε-transitions). This takes O(n × m) for m states, never more.
- **Subset construction**: every NFA has an equivalent DFA whose states are sets of NFA states.
  The DFA can be exponentially larger (up to 2ᵐ states) in the worst case, but is usually small,
  and can be built lazily, only for the sets that actually occur.

So NFAs and DFAs recognise exactly the same languages: the **regular languages**. Every DFA can
also be **minimised** to a unique smallest equivalent DFA (Hopcroft's algorithm).

## Regular expressions

A **regular expression** describes a language with three operations over symbols, ε and ∅:

- **Concatenation** `ab`: a string of `a` followed by one of `b`.
- **Alternation** `a|b`: either.
- **Kleene star** `a*`: zero or more repetitions.

Everything else is shorthand: `a+` = `aa*`, `a?` = `a|ε`, `[a-z]` = `a|b|…|z`, `.` = any symbol.

**Kleene's theorem**: regular expressions describe exactly the regular languages. The
constructive direction is **Thompson's construction**, which builds an NFA from a regex
piece by piece, with ε-transitions gluing the fragments:

```text
a       :  ──a──▶
ab      :  ──a──▶ ──ε──▶ ──b──▶
a|b     :  ──ε──▶ (a) ──ε──▶
           └─ε──▶ (b) ──ε──┘
a*      :  ──ε──▶ (a) ──ε──▶  with a loop back from the end of (a) to its start, and a bypass ε
```

The NFA has at most about 2 states per regex symbol, so it is built in linear time. This is
exactly what the next kata does.

## What regular languages cannot do

A finite automaton has finite memory, so it cannot count without bound. The language
{ aⁿbⁿ | n ≥ 0 } (n a's then n b's), balanced parentheses, and matching HTML tags are **not
regular**.

The **pumping lemma** makes this precise: for every regular language there is a length *p* such
that every string *s* in it with |s| ≥ p can be split as s = xyz with |xy| ≤ p, |y| ≥ 1, and
xyⁱz in the language for every i ≥ 0. For aⁿbⁿ, take s = aᵖbᵖ: y lies inside the a's, so pumping
it gives more a's than b's, which is not in the language. Contradiction, so aⁿbⁿ is not regular.

The practical consequence: **you cannot parse HTML, JSON or nested expressions with a true regular
expression.** You need a parser (a pushdown automaton, the next lesson) for that.

## Regex engines in practice

There are two families of engines:

| | Backtracking (Perl, PCRE, Java, Python `re`, JavaScript, .NET) | Automaton (RE2, Go `regexp`, Rust `regex`, grep, Hyperscan) |
| --- | --- | --- |
| How | try one alternative, back up on failure | simulate the NFA or a lazily built DFA |
| Worst case | **exponential** in the input length | **linear**: O(pattern × input) |
| Extras | backreferences (`(a+)\1`), lookaround, possessive quantifiers | none of those, or only some |

Backreferences make matching NP-hard, so they are beyond automata. The price of backtracking is
**catastrophic backtracking**: a pattern such as `(a+)+$` on `aaaa…a!` has exponentially many ways
to split the a's between the two `+`s, and a backtracking engine tries all of them before failing.
That is **ReDoS** (regular expression denial of service), which took down Stack Overflow in 2016
and Cloudflare in 2019. Defences: avoid nested quantifiers over the same characters, use atomic
groups or possessive quantifiers, set timeouts, or use a linear-time engine for untrusted input.

## Summary

- A DFA reads its input once with finite memory and runs in linear time.
- NFAs add choice and ε-moves; they recognise the same languages, simulated with state sets or
  converted to DFAs by the subset construction.
- Regular expressions describe exactly the regular languages; Thompson's construction turns them
  into NFAs.
- Regular languages cannot count: aⁿbⁿ and balanced parentheses need more (the pumping lemma).
- Backtracking regex engines are flexible but can be exponential; automaton engines are linear.

## Further reading

- Sipser, *Introduction to the Theory of Computation*, 3rd ed., chapter 1 "Regular Languages"
- Hopcroft, Motwani and Ullman, *Introduction to Automata Theory, Languages, and Computation*, 3rd ed., chapters 2–4
- Russ Cox, "Regular Expression Matching Can Be Simple And Fast": <https://swtch.com/~rsc/regexp/regexp1.html>
- Cloudflare, "Details of the Cloudflare outage on July 2, 2019" (catastrophic backtracking): <https://blog.cloudflare.com/details-of-the-cloudflare-outage-on-july-2-2019/>
- OWASP, "Regular expression Denial of Service (ReDoS)": <https://owasp.org/www-community/attacks/Regular_expression_Denial_of_Service_-_ReDoS>
- MIT OCW 18.404J Theory of Computation (Fall 2020): <https://ocw.mit.edu/courses/18-404j-theory-of-computation-fall-2020/>
