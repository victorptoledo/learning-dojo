---
from: go
status: draft
concept: "Bounded buffers of text"
from_way: "Reading whole files into memory with os.ReadFile, versus streaming with a bounded buffer."
native_way: "Treat the context window as a bounded buffer: rank the documents, add whole ones while they fit, cut the next at a word boundary with a marker, and leave the rest out on purpose."
sensei_tip: "sort.SliceStable with a score-then-id comparator keeps the document order deterministic, which keeps the prompt prefix identical across calls and cacheable."
---

| Go | Long-context concept |
| --- | --- |
| a bounded `bytes.Buffer` | the token budget |
| `sort.SliceStable` by score, then id | relevance order with a stable tie-break |
| `strings.Fields` and a cut | truncation at a word boundary |
| `html.EscapeString` | escaping document text and metadata |
| a cache keyed on a hash of the prefix | prompt caching |
