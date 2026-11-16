---
from: javascript
status: draft
concept: "From DOM scripting to declarative UI"
from_way: "Vanilla JS or jQuery finds nodes and mutates them (el.textContent = ..., el.classList.add(...)), and you keep the DOM in sync with your data by hand."
native_way: "React components return JSX for the current state; React computes the DOM changes. You write TypeScript in .tsx files and Vite bundles them."
sensei_tip: "If you catch yourself reaching for document.querySelector inside a React app, stop: put the value in state and render it. Refs are the rare exception."
---

| Vanilla JS | React |
| --- | --- |
| `<script src="app.js">` | `index.html` loads `src/main.tsx` through Vite |
| `el.textContent = count` | `<p>{count}</p>` and `setCount(count + 1)` |
| `el.addEventListener("click", f)` | `<button onClick={f}>` |
| `innerHTML` templates | JSX, escaped by default |
| Keep DOM and data in sync by hand | React re-renders from state |
| Webpack or no build | Vite dev server with hot reload |
