---
from: python
status: draft
concept: "difflib for UI trees"
from_way: "You regenerate HTML on the server per request, or mutate widgets in a desktop toolkit; you rarely think about diffing."
native_way: "React regenerates a description of the UI in memory and diffs it against the previous one, like difflib over trees, then patches the DOM."
sensei_tip: "Matching by key is like comparing two lists of dicts by their id instead of by index: without ids, an insertion at the top looks like every row changed."
---

| Python | React |
| --- | --- |
| Re-render a template per request | Re-render components per state change |
| `difflib` over sequences | Reconciliation over element trees |
| Compare rows by `id` | Compare children by `key` |
| Generators that yield work | Fiber units of work that can pause |
| `asyncio` priorities (none built in) | Urgent updates vs transitions |
| Widget replaced, state lost | Different type: state lost |
