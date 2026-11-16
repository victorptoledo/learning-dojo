---
from: java
status: draft
concept: "Pagination, filtering and sorting"
from_way: "`Pageable` with `page`/`size` everywhere, returning Spring Data's `Page` (with `totalElements`) straight to clients."
native_way: "Offset pages via `Pageable` only where they fit; keyset queries (`findTop21ByCreatedAtBeforeOrderByCreatedAtDesc` or Spring Data's `ScrollPosition`/`Window`) for feeds."
sensei_tip: "Spring Data 3.1+ has `Window<T>` and `ScrollPosition.keyset()`: keyset scrolling without writing the `WHERE (a, b) < (?, ?)` yourself."
---

| Java / Spring | Collection concept |
| --- | --- |
| `Pageable` / `Page<T>` | offset pagination |
| `Window<T>` + `KeysetScrollPosition` | keyset cursor |
| `Sort.by(desc("createdAt"), desc("id"))` | sort with a tie-breaker |
| `@RequestParam @Max(100) int limit` | maximum page size |
| `ServletUriComponentsBuilder.replaceQueryParam` | Link URLs |
