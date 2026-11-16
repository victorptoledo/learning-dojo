---
from: javascript
status: draft
concept: "The same FormData, plus state"
from_way: "You read input.value on submit or with new FormData(form), and call preventDefault in a submit listener."
native_way: "Uncontrolled forms work the same way in React; controlled forms keep the value in state; React 19 actions receive FormData directly."
sensei_tip: "Start uncontrolled with FormData, exactly as in vanilla JS; switch a field to controlled only when the UI must react to each keystroke."
---

| JavaScript | React |
| --- | --- |
| `form.addEventListener("submit", ...)` | `<form onSubmit={...}>` |
| `input.value` | `value` + `onChange` (controlled) |
| `new FormData(form)` | `new FormData(event.currentTarget)` or an action |
| `input.value = ""` after submit | `setValue("")`, or actions reset the form |
| `input.setCustomValidity` | an `errors` state and `aria-invalid` |
| `<label for>` | `<label htmlFor>` |
