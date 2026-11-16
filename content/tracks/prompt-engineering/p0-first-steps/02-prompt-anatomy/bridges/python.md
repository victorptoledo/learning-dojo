---
from: python
status: draft
concept: "Templates that fail loudly"
from_way: "f-strings and str.format to build text, which raise KeyError for a missing name but happily expand braces inside values when reused."
native_way: "A prompt template with named sections and placeholders; values are inserted literally and missing names fail with a clear error."
sensei_tip: "string.Template with substitute() is a good model: it raises KeyError for a missing name and never re-reads the inserted values. Avoid calling .format() on text that already contains user data."
---

| Python | Prompt concept |
| --- | --- |
| `f"Summarise: {email}"` inline | a one-off prompt (fine for experiments) |
| `string.Template(...).substitute(values)` | a template that fails on missing names |
| `"""triple-quoted"""` constants | a template kept in one place |
| `text.format(**values)` on user data | a bug: braces in the data are expanded |
| docstrings | role, task and format sections |
