---
from: javascript
status: draft
concept: "The DOM without a framework"
from_way: "React's onClick props and a virtual DOM that patches the page for you."
native_way: "The real DOM API: you create and update nodes yourself, and listeners attach to real elements; React itself delegates events to the root."
sensei_tip: "React already uses delegation under the hood; writing it by hand once makes framework behavior (and its bugs) much easier to reason about."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `<li onClick={...}>` | One listener on the list + `closest()` |
| JSX text `{title}` | `textContent = title` |
| `dangerouslySetInnerHTML` | `innerHTML`: just as dangerous |
| `useRef().current.focus()` | `element.focus()` |
