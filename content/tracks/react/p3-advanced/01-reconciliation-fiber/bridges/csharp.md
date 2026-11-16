---
from: csharp
status: draft
concept: "Blazor's render tree diff"
from_way: "Blazor builds a render tree per component, diffs it with the previous one and sends DOM edits; @key controls matching; ShouldRender can skip work."
native_way: "React works the same way: element trees diffed by type, position and key, DOM edits in a commit; React adds concurrent, interruptible rendering through Fiber."
sensei_tip: "If you know how Blazor's RenderTreeDiff and @key work, you already know reconciliation. The new part is concurrency: React can stop a render halfway and start again."
---

| Blazor | React |
| --- | --- |
| RenderTreeBuilder output | The element tree |
| RenderTreeDiff | Reconciliation |
| `@key` | `key` |
| `ShouldRender()` | `memo` (next lesson) |
| `StateHasChanged` batching | Automatic batching |
| Synchronous rendering | Concurrent rendering with priorities |
