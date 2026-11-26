# Text Toolkit

Write four functions in `textkit.py`. Only the standard library is needed: `re`,
`unicodedata` and `datetime.timedelta`.

## `search_key(text)`

A key for comparing names and titles. Two inputs that a person reads as the same text must
give the same key:

| Input | Key |
| --- | --- |
| `"José"` (precomposed `é`) and `"José"` (`e` plus a combining accent) | the same |
| `"ＡＤＭＩＮ"` (full-width), `"ﬁle"` (ligature) | `"admin"`, `"file"` |
| `"Straße"` and `"STRASSE"` | `"strasse"` |
| `"  São \t Paulo\n"` | `"são paulo"`: accents stay, whitespace runs become one space, no spaces at the ends |

## `slugify(title, *, max_length=60)`

| Rule | Example |
| --- | --- |
| Accents and compatibility forms go (`ç` to `c`, `ª` to `a`, `ﬁ` to `fi`, full-width digits to digits), `ß` becomes `ss` | `"Ação & Reação — 2ª edição"` to `"acao-reacao-2a-edicao"` |
| Lower case; every run of characters other than `a-z` and `0-9` becomes one `-`; no `-` at either end | `"  Hello,   World!!  "` to `"hello-world"` |
| Longer than `max_length`: cut at the last `-` that fits, so no word is split. A first word longer than `max_length` is cut at `max_length` | `"hello world again"` with 14 to `"hello-world"` |
| Nothing left (`""`, `"!!!"`, emoji only), or `max_length` below 1 | `ValueError` |

## `highlight(text, terms, *, before="[", after="]")`

Wraps every occurrence of any term in `before` and `after`, keeping the text's own case.

- Case is ignored, and terms are **literal** text: `"c++"`, `"c#"` and `"a.b"` match exactly
  those characters (`"a.b"` must not match `"axb"`).
- Whole words only: a match may not have a letter, digit or `_` right before or after it, so
  `"cat"` does not match `"cats"` and `"café"` does not match `"cafés"`. Note that `\b` does not
  work next to `+` or `#`.
- When terms overlap, the longest wins: `["new", "new york"]` on `"New York is new"` gives
  `"[New York] is [new]"`.
- Empty or blank terms are ignored; no terms leaves the text unchanged.
- One pass over the text: 540,000 characters with 301 terms take well under a second.

## `parse_duration(text)`

Reads days, hours, minutes and seconds into a `timedelta`: `"45s"`, `"1h 30m"`, `"2h15m"`,
`"1d 2h 3m 4s"`, `"  90 M  "`.

- Any subset of the units `d`, `h`, `m`, `s`, each at most once and **in that order**, with
  optional spaces anywhere between the parts. Units ignore case.
- Whole numbers with ASCII digits only (`"٣h"`, Arabic-Indic three, is rejected).
- Anything else is a `ValueError`: `""`, `"10"`, `"1h 1h"`, `"30m 1h"`, `"1.5h"`, `"-2m"`,
  `"1 hour"`, `"1h x"`, `"1w"`.
- It must fail fast on hostile input such as 50,000 digits followed by `x`: no nested
  quantifiers.

Tools: `unicodedata.normalize` (NFKC, NFKD), `unicodedata.category`, `str.casefold`,
`re.escape`, lookarounds `(?<!\w)` and `(?!\w)`, `re.VERBOSE`, `re.ASCII`, `re.IGNORECASE`,
`fullmatch` and named groups.

Run the visible tests with `uv run pytest`. There are 33 tests: 1 in `tests/test_visible.py`
and 32 hidden ones, plus ruff on `textkit.py`.
