---
from: javascript
status: draft
concept: "Layout without JavaScript"
from_way: "Measuring the window in a resize handler and setting widths from script."
native_way: "CSS layout responds to size by itself: flexbox, grid, clamp(), media and container queries replace almost all resize code."
sensei_tip: "If you are about to write a ResizeObserver to change a class, check whether a container query does it in CSS."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `window.addEventListener("resize")` | `@media (min-width: ...)` |
| `ResizeObserver` on a component | `@container` |
| Computing widths in JS | `fr`, `minmax()`, `clamp()` |
| `matchMedia("(prefers-color-scheme: dark)")` | The same media feature in CSS |
