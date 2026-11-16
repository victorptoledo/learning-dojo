---
from: java
status: draft
concept: "A diff over immutable trees"
from_way: "Swing and JavaFX keep one mutable widget tree that your code updates in place; layout passes recompute positions."
native_way: "React builds a new immutable element tree on each render, diffs it against the previous one and mutates the real DOM only where they differ."
sensei_tip: "Think of render as computing a new immutable value and commit as applying a patch, like a diff between two versions of a data structure. Identity in that diff comes from type, position and key."
---

| Java UI | React |
| --- | --- |
| Mutate widgets in place | Render a new element tree |
| `revalidate()` / `repaint()` | Commit phase |
| Component identity = object reference | Identity = type + position + key |
| Replace a panel: new widgets, state lost | Different type: subtree remounted |
| Event dispatch thread | Scheduler with priorities |
| `SwingWorker` for heavy work | Transitions for heavy renders |
