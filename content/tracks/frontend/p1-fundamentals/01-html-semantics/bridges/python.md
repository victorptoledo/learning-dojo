---
from: python
status: draft
concept: "Meaning before presentation"
from_way: "Readable code with descriptive names; Jinja templates that output whatever HTML you write."
native_way: "The element you pick is the meaning: browsers, screen readers and search engines read it, so <h2> is not just bigger text."
sensei_tip: "Template engines will happily output a div for everything. Treat element choice like naming: the most precise one wins."
---

| Python | In the browser |
| --- | --- |
| A `dataclass` with typed fields | Elements with built-in meaning |
| `datetime.date` | `<time datetime>` |
| `Decimal` | `<data value>` |
| Docstrings | `alt` text, captions and labels |
