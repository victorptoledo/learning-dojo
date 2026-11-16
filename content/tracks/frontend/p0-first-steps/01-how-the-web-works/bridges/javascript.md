---
from: javascript
status: draft
concept: "Same language, different host"
from_way: "Node.js: JavaScript with `fs`, `process` and `http`, running on a server you control."
native_way: "The browser: JavaScript with `document`, `window`, `fetch` and `localStorage`, running in a sandbox on the user's device."
sensei_tip: "ECMAScript is the same; the APIs around it are not. There is no `fs` or `process` in a page, and no `document` in Node.js."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `require('fs')` | No file system: the page asks the server with `fetch` |
| `process.env` | Build-time variables baked into the bundle |
| `http.createServer` | The server the page talks to |
| Node's event loop | The browser's event loop, which also renders |
