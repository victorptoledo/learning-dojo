---
from: javascript
status: draft
concept: "The same language, a different host"
from_way: "In the browser JavaScript has the DOM, window, localStorage and a sandbox that forbids files and raw sockets."
native_way: "Node.js has no DOM or window, but gives process, Buffer, node:fs, node:net, node:child_process and full access to the machine, plus many web APIs such as fetch and URL."
sensei_tip: "The event loop you know from the browser is the same idea; the difference is that a slow callback now delays every user of the server, not only one tab."
---

| Browser | Node.js |
| --- | --- |
| `window`, `document` | `globalThis`, `process` (no DOM) |
| `localStorage` | Files (`node:fs`), databases |
| `fetch`, `URL`, `AbortController` | The same, built in |
| `<script type="module">` | ES modules (`.mjs` or `"type": "module"`) |
| A bundler for TypeScript | `node file.ts` strips types |
