---
from: java
status: draft
concept: "Paging and caching large inputs"
from_way: "Loading a whole table into memory, versus paging, indexes and a cache in front of expensive calls."
native_way: "Fit labelled documents into a token budget by relevance, truncate with a marker, keep a stable prefix that the provider can cache, and retrieve passages when the corpus is large."
sensei_tip: "Prompt caching behaves like a cache keyed on the exact prefix bytes: one changed character near the start is a cache miss for everything after it. Keep the prefix as stable as a cache key."
---

| Java | Long-context concept |
| --- | --- |
| `Pageable` and a page size | a token budget for documents |
| `ORDER BY relevance DESC` | most relevant documents first |
| `String.substring` at a word boundary plus "..." | truncation with a marker |
| a Caffeine cache keyed on the request | prompt caching of a stable prefix |
| a full-text index | retrieval instead of stuffing |
