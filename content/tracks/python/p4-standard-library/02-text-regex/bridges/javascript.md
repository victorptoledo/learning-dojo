---
from: javascript
status: draft
concept: "Text, regex and Unicode"
from_way: "Regex literals with flags (g, i, u, v), String.prototype.match, matchAll and replace with a function, and String.prototype.normalize."
native_way: "re module functions and compiled patterns, flags as arguments, fullmatch for validation, and unicodedata.normalize."
sensei_tip: "JavaScript strings are UTF-16, so an emoji has length 2; a Python str counts code points, so it has length 1 (a flag or family emoji is still several)."
---

| JavaScript | Python 3.15 |
| --- | --- |
| `/^\d+$/.test(s)` | `re.fullmatch(r"\d+", s)` |
| `s.matchAll(/(?<lvl>[A-Z]+)/g)` | `re.finditer(r"(?P<lvl>[A-Z]+)", s)` |
| `m.groups.lvl` | `m["lvl"]` |
| `s.replace(/x/g, fn)` | `re.sub(r"x", fn, s)` |
| `/.../i` | `re.IGNORECASE` |
| `s.normalize("NFC")` | `unicodedata.normalize("NFC", s)` |
| `s.toLowerCase()` | `s.casefold()` for comparisons |
