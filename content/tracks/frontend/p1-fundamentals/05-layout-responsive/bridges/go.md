---
from: go
status: draft
concept: "Declarative layout"
from_way: "Terminal UIs with fixed widths, or templates that leave layout to someone else."
native_way: "CSS layout is declarative: you state constraints (columns, gaps, minimums) and the browser solves them for every screen size."
sensei_tip: "Like a good interface, a good layout states the minimum it needs (minmax, min-width) and lets the engine do the rest."
---

| Go | In the browser |
| --- | --- |
| Fixed-width tabwriter columns | `grid-template-columns` |
| Padding strings to align | `gap` and `align-items` |
| Different output per terminal size | Media queries |
| Composition of small parts | Components that adapt with container queries |
