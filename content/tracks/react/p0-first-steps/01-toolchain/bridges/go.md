---
from: go
status: draft
concept: "A UI as a pure function of data"
from_way: "html/template renders a page from a struct on the server; to change the page, you render it again."
native_way: "React components render JSX from props and state in the browser, and React re-renders and patches the DOM whenever state changes."
sensei_tip: "Go's html/template already taught you to render from data; React does the same on every state change, and diffs the result so only what changed is touched."
---

| Go | React |
| --- | --- |
| `go mod init`, `go run .` | `npm create vite@latest`, `npm run dev` |
| `tmpl.Execute(w, data)` | `createRoot(el).render(<App />)` |
| `{{.Name}}` | `{name}` |
| Re-render the page per request | Re-render the component per state change |
| Standard library first | React for UI; routing and data from libraries |
| `go vet` warnings | StrictMode warnings in development |
