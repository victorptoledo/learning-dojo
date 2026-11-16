---
from: javascript
status: draft
concept: "fetch in the browser vs Node.js"
from_way: "fetch in Node.js 18+, with the same API, plus server concerns like timeouts and retries."
native_way: "In the page, fetch also obeys CORS, sends the origin's cookies and competes with rendering for the main thread."
sensei_tip: "Your Node fetch code ports directly; add the UI parts: loading and error states, aborting on navigation and never letting a stale response win."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `AbortSignal.timeout(ms)` in Node | The same in the browser |
| `undici` agent options | Not available: the browser manages connections |
| Retry with backoff | The same, plus a visible state for the user |
| Server logs | The Network panel |
