---
from: javascript
status: draft
concept: "JSX is sugar for function calls"
from_way: "You build DOM with document.createElement and template literals, or with innerHTML strings that are easy to inject into."
native_way: "JSX compiles to jsx() calls that return element objects; React turns them into DOM. Props are typed in TypeScript and attributes are camelCase."
sensei_tip: "When JSX surprises you, rewrite it in your head as jsx(type, props): braces are just expressions, and class becomes className because it is an object key."
---

| JavaScript | React (TSX) |
| --- | --- |
| `document.createElement("h1")` | `<h1 />` → `jsx("h1", {})` |
| `el.className = "title"` | `className="title"` |
| `` el.innerHTML = `<b>${name}</b>` `` (unsafe) | `<b>{name}</b>` (escaped) |
| `el.addEventListener("click", f)` | `onClick={f}` |
| A function returning a DOM node | A component returning JSX |
| JSDoc types | `type Props = { ... }` |
