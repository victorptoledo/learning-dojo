---
from: python
status: draft
concept: "Layout managers"
from_way: "Tkinter's pack and grid, or Qt's QHBoxLayout, QVBoxLayout and QGridLayout."
native_way: "Flexbox and grid are the browser's layout managers, declared in CSS instead of code, and they re-run automatically on every resize."
sensei_tip: "pack(side=LEFT, expand=True) is flex: 1; grid(row=, column=, columnspan=) is grid-row and grid-column."
---

| Python | In the browser |
| --- | --- |
| `pack(side=LEFT)` | `flex-direction: row` |
| `pack(expand=True, fill=BOTH)` | `flex: 1` |
| `grid(row=0, column=1, columnspan=2)` | `grid-column: 2 / span 2` |
| `<Configure>` event handler | A media or container query |
