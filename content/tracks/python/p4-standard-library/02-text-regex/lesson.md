# Text, Regular Expressions and Unicode

Text arrives from users, files and other systems, and it is never as clean as the examples.
This lesson covers the three tools that handle it: `str` methods for the simple cases, the
`re` module for patterns, and `unicodedata` for the cases where two strings look the same but
are not.

## Start with str methods

Most "parsing" needs no regular expression:

```python
line.strip()                     # whitespace at both ends
name.removeprefix("Dr. ")        # 3.9+; strip("Dr. ") would remove characters, not a prefix
key, _, value = "host = db1".partition("=")
"a,b,,c".split(",")              # ['a', 'b', '', 'c']
"  many   spaces ".split()       # ['many', 'spaces']: no argument splits on runs of whitespace
" ".join(words)
code.isdigit(), word.isalpha(), title.startswith(("Re:", "Fwd:"))
```

They are faster, and much easier to read, than the pattern that does the same.

## Regular expressions with re

Write patterns as **raw strings** (`r"\d+"`), so the backslashes reach `re` untouched.

| Function | Finds |
| --- | --- |
| `re.fullmatch(p, s)` | The **whole** string must match: use it to validate |
| `re.match(p, s)` | A match at the **start** only |
| `re.search(p, s)` | The first match anywhere |
| `re.finditer(p, s)` | Every match, lazily, as match objects |
| `re.findall(p, s)` | Every match as strings (or tuples of groups) |
| `re.sub(p, repl, s)` | Replaces every match; `repl` can be a function |
| `re.split(p, s)` | Splits on the pattern |

`re.match(r"\d+", "12abc")` succeeds, which surprises people who meant "is it a number?".
Validation is `fullmatch`.

**Groups.** Parentheses capture; name them so the code says what it reads:

```python
LINE = re.compile(r"(?P<level>[A-Z]+) (?P<service>[\w-]+): (?P<message>.*)")

if m := LINE.fullmatch("ERROR billing-api: card declined"):
    m["level"], m["service"]     # ('ERROR', 'billing-api')
    m.groupdict()                # every named group as a dict
```

`(?:...)` groups without capturing. `re.compile` once at module level names the pattern and
skips the (cached) parse on every call.

**Greedy and lazy.** `.*` takes as much as it can, then gives back. In `<b>one</b><b>two</b>`,
`<b>(.*)</b>` captures `one</b><b>two`; the lazy `<b>(.*?)</b>` captures `one`. Better still,
say what may not appear: `<b>([^<]*)</b>`.

**Verbose patterns.** `re.VERBOSE` ignores whitespace and allows comments, which makes a long
pattern reviewable:

```python
DURATION = re.compile(r"""
    \s* (?:(?P<hours>\d+)\s*h)?      # 2h
    \s* (?:(?P<minutes>\d+)\s*m)?    # 30m
    \s* (?:(?P<seconds>\d+)\s*s)?    # 15s
    \s*
""", re.VERBOSE)
```

**Replacing with a function.** `re.sub` calls the function with each match, which is the clean
way to transform matches:

```python
def mask(m: re.Match[str]) -> str:
    return m[0][:2] + "*" * (len(m[0]) - 4) + m[0][-2:]

re.sub(r"\b\d{12,19}\b", mask, "card 4111111111111111 ok")   # 'card 41************11 ok'
```

**User input inside a pattern.** A search term like `c++` or `a.b` contains metacharacters.
`re.escape(term)` makes it literal. Never paste raw input into a pattern.

## When a regex is slow

Nested quantifiers such as `(a+)+$` or `(\w+\s?)*$` can take exponential time on a string that
almost matches, and one request can hang a worker (ReDoS). Defences:

- Prefer patterns where each character can be matched in only one way (`[^,]*` instead of
  `.*?` before a comma).
- Since 3.11, **atomic groups** `(?>...)` and **possessive quantifiers** `a*+`, `a++` never give
  back what they matched, which removes the explosive backtracking.
- Limit the input length before matching, and do not run user-supplied patterns.

## Unicode: what a character is

A `str` is a sequence of **code points**. What a reader sees as one character can be several:
`"é"` can be one code point (`U+00E9`) or two (`e` plus the combining accent `U+0301`). They
print the same and compare **different**:

```python
import unicodedata

a, b = "café", "café"
a == b                                   # False
len(a), len(b)                           # (4, 5)
unicodedata.normalize("NFC", a) == unicodedata.normalize("NFC", b)   # True
```

| Form | Does | Use it to |
| --- | --- | --- |
| **NFC** | Composes (`e` + accent becomes `é`) | Store and compare text; most input is already NFC |
| **NFD** | Decomposes | Strip accents: decompose, then drop the combining marks (category `Mn`) |
| **NFKC** | Composes and folds look-alikes (`ﬁ` to `fi`, full-width `Ａ` to `A`, `²` to `2`) | Identifiers and search keys |

**Case.** `lower()` is for display; `casefold()` is for comparing. `"Straße".lower()` keeps the
`ß`, while `"Straße".casefold() == "strasse"`. A good comparison key is
`unicodedata.normalize("NFKC", s).casefold()`.

**Stripping accents** for slugs and search:

```python
def strip_accents(text: str) -> str:
    decomposed = unicodedata.normalize("NFD", text)
    return "".join(c for c in decomposed if unicodedata.category(c) != "Mn")

strip_accents("São João")      # 'Sao Joao'
```

**Regex and Unicode.** In Python 3, `\w`, `\d` and `\s` match Unicode: `\w` matches `ã` and
`\d` matches Arabic-Indic digits like `٣`. Pass `re.ASCII` (or write `[0-9]`) when only ASCII
is valid, for example in a product code. `re.IGNORECASE` uses simple case folding, so
normalise and casefold first when the comparison must be exact.

## Summary

- `str` methods first; `partition`, `removeprefix`, `split()` without arguments.
- Raw strings, `fullmatch` to validate, named groups, `re.compile` for reused patterns.
- Lazy quantifiers or negated classes instead of greedy `.*`; `re.VERBOSE` for long patterns.
- `re.escape` for user input; atomic groups or possessive quantifiers against ReDoS.
- Normalise (NFC, or NFKC for keys), then `casefold()` before comparing text.
