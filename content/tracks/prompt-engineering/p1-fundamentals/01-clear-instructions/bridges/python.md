---
from: python
status: draft
concept: "Explicit is better than implicit"
from_way: "The Zen of Python, docstrings that describe arguments and returns, and functions that raise on bad input."
native_way: "A prompt that is explicit about audience, purpose, format, limits and the way out, because the model cannot ask what you meant."
sensei_tip: "Write the prompt the way you would write a good docstring for a stranger: what it does, what it returns, and what happens with odd input. Then write the doctest: that is your success criterion."
---

| Python | Prompt concept |
| --- | --- |
| a docstring | task, context and output format |
| `Returns:` section | the exact output shape |
| `raise ValueError(...)` | the explicit exit for inputs that do not fit |
| a doctest | a success criterion with an example |
| `# why:` comments | the reason behind a rule |
