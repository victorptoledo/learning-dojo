---
from: javascript
status: draft
concept: "From selectors to roles"
from_way: "You test DOM code with document.querySelector and dispatchEvent, and accessibility is often an afterthought with divs and spans."
native_way: "Testing Library queries the accessibility tree (roles and names) and user-event simulates real input; semantic elements and ARIA make those queries possible."
sensei_tip: "If getByRole cannot find your control, a screen reader cannot either. Fix the component (a real button, a label), not the test."
---

| JavaScript | React + Testing Library |
| --- | --- |
| `document.querySelector(".save")` | `screen.getByRole("button", { name: "Save" })` |
| `el.dispatchEvent(new Event("click"))` | `await user.click(el)` |
| `setTimeout` then check | `await screen.findByText(...)` |
| `<div class="btn">` | `<button>` |
| `title` attributes as labels | `<label>`, `aria-label` |
| Manual focus with `el.focus()` | Refs and `.focus()` in handlers or effects |
