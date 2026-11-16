---
from: javascript
status: draft
concept: "From page links to the History API"
from_way: "Each link loads a new HTML page from the server; query strings are read with new URLSearchParams(location.search)."
native_way: "A router intercepts links, calls history.pushState and renders the matching components; useSearchParams wraps URLSearchParams as state."
sensei_tip: "React Router is pushState plus a matching table plus React state. Knowing that, the SPA fallback rule on the server and the scroll restoration questions make sense."
---

| JavaScript | React Router |
| --- | --- |
| `<a href="/products">` (full reload) | `<Link to="/products">` |
| `history.pushState({}, "", url)` | `navigate(url)` |
| `window.onpopstate` | Handled by the router |
| `new URLSearchParams(location.search)` | `useSearchParams()` |
| `location.pathname.split("/")` | `useParams()` |
| One HTML file per page | One `index.html`, rewrite all routes to it |
