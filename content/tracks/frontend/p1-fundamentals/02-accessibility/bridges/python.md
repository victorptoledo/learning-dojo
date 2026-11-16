---
from: python
status: draft
concept: "Accessibility as part of correctness"
from_way: "Tkinter or Qt widgets expose roles and names to the OS accessibility API."
native_way: "HTML elements map to accessibility roles automatically; your job is names, keyboard support and correct states."
sensei_tip: "Treat axe-core like a linter (ruff for accessibility): it catches the mechanical half; the keyboard and a screen reader catch the rest."
---

| Python | In the browser |
| --- | --- |
| A linter for code style | axe-core for accessibility rules |
| `QLabel.setBuddy(field)` | `<label for>` |
| `setAccessibleName` | `aria-label` |
| Manual QA | Tabbing through the page and using a screen reader |
