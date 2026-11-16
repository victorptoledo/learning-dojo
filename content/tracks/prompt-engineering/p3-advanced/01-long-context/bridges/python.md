---
from: python
status: draft
concept: "Packing text into a budget"
from_way: "Reading whole files with open().read() and concatenating them into one string."
native_way: "Sort documents by relevance, wrap each in tags with escaped metadata, add whole ones while the estimate fits, cut the next at a word boundary with a marker, and record what was left out."
sensei_tip: "The fit check is monotonic in the number of words, so bisect (or a hand-written binary search) finds the longest truncation that fits in a few renders instead of one per word."
---

| Python | Long-context concept |
| --- | --- |
| `sorted(docs, key=lambda d: (-d.score, d.id))` | relevance order with a stable tie-break |
| `html.escape(title, quote=True)` | safe metadata in tag attributes |
| `" ".join(words[:k]) + " [truncated]"` | truncation with a marker |
| `bisect` over `k` | finding the longest cut that fits |
| a `functools.cache` on a prefix | the idea behind prompt caching |
