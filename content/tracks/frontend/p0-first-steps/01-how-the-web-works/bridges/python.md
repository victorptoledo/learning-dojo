---
from: python
status: draft
concept: "The browser as a client runtime"
from_way: "Django or Flask render templates on the server and return HTML."
native_way: "The browser parses that HTML into a DOM, fetches every linked resource and runs JavaScript on the user's machine."
sensei_tip: "Your Jinja templates were already frontend: the difference now is that code also runs after the page arrives, in an environment you do not control."
---

| Python | In the browser |
| --- | --- |
| `render(request, 'page.html')` | The HTML response, then the browser's parsing |
| `requests.get(url)` | `fetch(url)` from the page |
| `print()` | `console.log()` |
| `python -m http.server` | The kata's `serve` command |
