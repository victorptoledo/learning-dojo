---
from: go
status: draft
concept: "Coding interviews"
from_way: "Writing container/heap boilerplate from memory under pressure, or ignoring errors in interview code."
native_way: "Go interview code is plain loops over slices and maps; the standard library's slices, maps and sort packages cover sorting and searching, and container/heap needs its five methods. Say when you skip error handling for brevity."
sensei_tip: "Practise the container/heap boilerplate (Len, Less, Swap, Push, Pop on a slice type) until it takes under two minutes, and remember `sort.Search` for binary search on the answer."
---

| Need | Go | Java |
| --- | --- | --- |
| count | `m[k]++` | `merge` |
| queue | slice with `q[1:]` | `ArrayDeque` |
| heap | `container/heap` | `PriorityQueue` |
| sort | `slices.SortFunc` | `Arrays.sort` |
| binary search on the answer | `sort.Search(n, pred)` | hand-written |
