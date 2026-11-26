---
from: go
status: draft
concept: "Text, regex and Unicode"
from_way: "strings and regexp packages (RE2: linear time, no backreferences), regexp.QuoteMeta, runes, and golang.org/x/text/unicode/norm."
native_way: "str methods, the re module (backtracking, with backreferences and lookarounds), re.escape, and unicodedata in the standard library."
sensei_tip: "Go's RE2 cannot blow up; Python's backtracking engine can. Patterns you copy from Go code are safe, but the reverse is not true."
---

| Go | Python 3.15 |
| --- | --- |
| `regexp.MustCompile(p)` | `re.compile(p)` |
| `re.MatchString(s)` with `^...$` | `pattern.fullmatch(s)` |
| `re.FindAllStringSubmatch(s, -1)` | `pattern.findall(s)` / `finditer(s)` |
| `re.ReplaceAllStringFunc(s, f)` | `pattern.sub(f, s)` |
| `regexp.QuoteMeta(input)` | `re.escape(input)` |
| `norm.NFC.String(s)` | `unicodedata.normalize("NFC", s)` |
| `strings.EqualFold(a, b)` | `a.casefold() == b.casefold()` |
