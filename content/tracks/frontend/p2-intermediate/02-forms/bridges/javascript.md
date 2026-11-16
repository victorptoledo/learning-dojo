---
from: javascript
status: draft
concept: "Forms without a form library"
from_way: "Controlled inputs in React with state per field and a validation library like Zod."
native_way: "Uncontrolled native forms: the DOM holds the values, constraint attributes declare the rules and FormData reads everything at submit."
sensei_tip: "Native constraints and FormData remove most of a form library's job; keep a schema (like Zod) for the server and for rules HTML cannot express."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `useState` per field | The input's own `value` |
| `onSubmit` + `e.preventDefault()` | The same, on `<form>` |
| Zod schema in the client | Constraint attributes plus `setCustomValidity` |
| `Object.fromEntries(...)` of state | `Object.fromEntries(new FormData(form))` |
