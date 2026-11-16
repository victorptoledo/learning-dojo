---
from: javascript
status: draft
concept: "Accessibility beyond the click handler"
from_way: "Adding onClick to any element in a component library."
native_way: "Only native interactive elements get focus, keyboard activation and a role; ARIA describes state but adds no behavior."
sensei_tip: "If you write onKeyDown to make a div clickable, stop: a <button> already does Enter, Space, focus, disabled and the role."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `div.onclick` | `<button type="button">` |
| `isOpen` state | `aria-expanded={isOpen}` on the trigger |
| A toast in a corner | `role="status"` live region |
| `element.focus()` after an action | Deliberate focus management |
