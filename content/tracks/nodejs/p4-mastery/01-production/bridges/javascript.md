---
from: javascript
status: draft
concept: "A program that must stop politely"
from_way: "A front end is stopped by closing the tab, and deploys replace static files on a CDN."
native_way: "A server is stopped with SIGTERM in the middle of requests, many times a day, and must drain them before exiting."
sensei_tip: "The beforeunload event is a weak cousin of the SIGTERM handler: on the server it is not optional, and it decides whether users see errors during every deploy."
---

| Front end | Node.js service |
| --- | --- |
| Deploy = new files on a CDN | Deploy = replace running processes |
| Close the tab | `SIGTERM`, then `SIGKILL` |
| `beforeunload` | `process.on("SIGTERM")` |
| Console logs | JSON logs on stdout |
| Health = the page loads | Liveness and readiness endpoints |
