# Regex Engine

Write a regular expression engine in `src/lib.rs` that can never be made to take exponential
time: parse the pattern, build an NFA with **Thompson's construction**, and simulate it by
tracking the **set** of states it can be in. `RegexError` is declared; `Regex` is yours to fill.

## Syntax

| Pattern | Matches |
| --- | --- |
| `a` | the character `a` (any character that is not special) |
| `.` | any one character |
| `xy` | `x` followed by `y` |
| `x\|y` | `x` or `y` (lowest precedence; an empty side matches the empty string: `a\|`) |
| `x*` `x+` `x?` | zero or more, one or more, zero or one `x` (binds tightest) |
| `(x)` | a group; `()` matches the empty string |
| `\c` | the character `c` literally, for any `c`: `\.`, `\*`, `\(`, `\\` |
| `[abc]` `[a-z0-9]` | one character from the set or ranges |
| `[^...]` | one character **not** in the set |

Inside a class, `\c` is a literal `c`, and `-` is literal when it is the first or last character
(`[-a]`, `[a-]`). Patterns and texts are Unicode: positions and `.` work on `char`s.

## Errors

`Regex::new` returns `Err` with the **character position** of the problem:

| Error | When |
| --- | --- |
| `NothingToRepeat(p)` | a `*`, `+` or `?` at the start, after `(` or `\|`, or right after another quantifier (`a**`, `a+?`) |
| `UnbalancedParen(p)` | a `)` without a `(` (at the `)`), or a `(` never closed (at the `(`) |
| `TrailingBackslash(p)` | a `\` at the very end (at the `\`) |
| `UnclosedClass(p)` | a `[` never closed (at the `[`) |
| `EmptyClass(p)` | `[]` or `[^]` (at the `[`) |
| `BadRange(p)` | a range like `z-a` (at its first character) |

## Matching

| Method | Returns |
| --- | --- |
| `is_match(text)` | whether the **whole** text matches |
| `find(text)` | `Some((start, end))` in **byte** offsets for the leftmost position where a match starts, taking the longest match there, or `None` |
| `state_count()` | the number of states of your NFA |

The NFA must be built in linear size (at most 4 states per pattern character, plus 4), and
`is_match` must run in O(states × text length): never backtrack.

## What the hidden tests check

Every construct above, escapes, classes (including Unicode ranges), sixteen error cases with their
positions, `find` on ASCII and multi-byte text, the NFA size, and inputs that make backtracking
engines hang: `(a*)*b` on 20,000 a's, `(x+x+)+y`, and the classic a?ⁿaⁿ. Each must finish within
seconds.

Run the visible tests with `cargo test`.
