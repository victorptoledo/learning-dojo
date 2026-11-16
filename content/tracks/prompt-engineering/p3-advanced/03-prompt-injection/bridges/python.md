---
from: python
status: draft
concept: "Untrusted input in a new place"
from_way: "Escaping HTML with html.escape, parameterised SQL, and never passing user input to eval or subprocess with shell=True."
native_way: "Neutralise untrusted text (strip hidden characters, escape tags), wrap it in labelled tags, flag suspicious patterns, and check the model's output before rendering or acting."
sensei_tip: "unicodedata.category(ch) == \"Cc\" finds control characters, and a small set of zero-width code points (U+200B to U+200D, U+2060, U+FEFF) covers the common invisible-text tricks."
---

| Python | Prompt-injection concept |
| --- | --- |
| `html.escape(text, quote=False)` | text that cannot close its tag |
| `unicodedata.category(ch)` | finding hidden control characters |
| `re.search(pattern, text, re.I)` | heuristic flags for suspicious text |
| `urllib.parse.urlsplit(url).hostname` | checking output links against an allow-list |
| never `eval(model_output)` | never execute model output as code |
