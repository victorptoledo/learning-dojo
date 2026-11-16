---
from: javascript
status: draft
concept: "The DOM has types too"
from_way: "Building UIs where every node is a div created by a framework."
native_way: "Each element is a different interface (HTMLButtonElement, HTMLAnchorElement, HTMLTimeElement) with its own behavior and accessibility."
sensei_tip: "A <button> comes with focus, keyboard handling and a role; recreating that on a div takes dozens of lines and is never quite right."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `document.createElement("div")` for everything | The specific element: button, a, nav, li |
| A click handler on a div | `<button type="button">` |
| `router.push()` on a span | `<a href>` |
| `new Date()` in text | `<time datetime>` |
