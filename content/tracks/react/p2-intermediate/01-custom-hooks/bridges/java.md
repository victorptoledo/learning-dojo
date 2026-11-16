---
from: java
status: draft
concept: "Reusable behaviour by composition, not inheritance"
from_way: "You reuse behaviour with base classes, mixins via default methods, or helper objects injected into a class."
native_way: "A custom hook is a function that bundles state and effects; components compose several hooks, each call with its own state."
sensei_tip: "Think of a custom hook as a small stateful helper object created per component instance, with no shared static fields. And remember the hidden rule: it must be created in the same order every time."
---

| Java | React |
| --- | --- |
| Abstract base class with shared logic | A custom hook used by several components |
| A helper object per instance | Each hook call has its own state |
| `static` shared state | Not a hook: use context or a store |
| Unit test of a helper class | `renderHook(() => useThing())` |
| Interface defining the API | The hook's typed parameters and return |
| `close()` on the helper | The cleanup of the hook's effects |
