---
from: csharp
status: draft
concept: "Text, regex and Unicode"
from_way: "System.Text.RegularExpressions with Regex.IsMatch, named groups, Regex.Escape, a match timeout against ReDoS, and string.Normalize."
native_way: "The re module with fullmatch and named groups, re.escape, atomic groups or possessive quantifiers instead of a timeout, and unicodedata.normalize."
sensei_tip: "Python's re has no match timeout: write patterns that cannot backtrack badly, and limit the input length."
---

| C# | Python 3.15 |
| --- | --- |
| `Regex.IsMatch(s, @"^\d+$")` | `re.fullmatch(r"\d+", s)` |
| `m.Groups["level"].Value` | `m["level"]` |
| `Regex.Escape(input)` | `re.escape(input)` |
| `new Regex(p, opts, TimeSpan.FromSeconds(1))` | no timeout: `(?>...)`, `a++`, short input |
| `RegexOptions.IgnorePatternWhitespace` | `re.VERBOSE` |
| `s.Normalize(NormalizationForm.FormC)` | `unicodedata.normalize("NFC", s)` |
| `string.Equals(a, b, OrdinalIgnoreCase)` | `a.casefold() == b.casefold()` |
