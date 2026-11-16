---
from: javascript
status: draft
concept: "Bundles and budgets"
from_way: "Shipping one huge bundle to the browser, versus code splitting, lazy loading and a performance budget."
native_way: "A token budget for the prompt, documents ordered by relevance, truncation with a marker, a stable cached prefix, and retrieval of passages when the corpus is too large."
sensei_tip: "A long prompt is like a JavaScript bundle with a performance budget: include what this request needs, put the stable part where it caches, and lazy-load (retrieve) the rest."
---

| JavaScript | Long-context concept |
| --- | --- |
| a bundle size budget | a token budget |
| code splitting | retrieving only relevant passages |
| long-lived cache for hashed assets | prompt caching of a stable prefix |
| `text.slice(0, n) + "…"` at a word boundary | truncation with a marker |
| critical CSS first | most relevant documents first |
