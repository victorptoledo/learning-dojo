---
from: java
status: draft
concept: "Text, regex and Unicode"
from_way: "String methods, Pattern.compile and Matcher with find(), matches() and named groups, Pattern.quote, and java.text.Normalizer."
native_way: "str methods, re.compile with fullmatch, search and finditer, re.escape, and unicodedata.normalize."
sensei_tip: "Java's matches() is Python's fullmatch(); Java's find() is search(). Python strings are code points, so there are no surrogate pairs to worry about."
---

| Java 25 | Python 3.15 |
| --- | --- |
| `Pattern.compile(p).matcher(s).matches()` | `re.fullmatch(p, s)` |
| `matcher.find()` in a loop | `for m in re.finditer(p, s):` |
| `matcher.group("level")` | `m["level"]` |
| `Pattern.quote(input)` | `re.escape(input)` |
| `Normalizer.normalize(s, Form.NFC)` | `unicodedata.normalize("NFC", s)` |
| `s.toLowerCase(Locale.ROOT)` | `s.casefold()` for comparisons |
| `replaceAll(p, m -> ...)` | `re.sub(p, function, s)` |
