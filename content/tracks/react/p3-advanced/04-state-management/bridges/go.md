---
from: go
status: draft
concept: "Ownership decides placement"
from_way: "A Go service is explicit about who owns data: the database owns records, a cache mirrors them with a TTL, and handlers keep request-local values."
native_way: "React apps apply the same ownership rule: server-owned data in a query cache, view state in the URL, client-owned shared state in a store, the rest local."
sensei_tip: "Apply Go's habit of explicit ownership: every piece of state gets one owner. Two copies of the same data in two stores is the bug you are trying to avoid."
---

| Go | React |
| --- | --- |
| Database as source of truth | Server state cached by a query library |
| In-memory cache with TTL | `staleTime` in TanStack Query |
| `r.URL.Query()` | Search params |
| A struct guarded by a mutex, with subscribers | An external store |
| Request-local variables | Local component state |
| Channels notifying listeners | `subscribe` + `useSyncExternalStore` |
