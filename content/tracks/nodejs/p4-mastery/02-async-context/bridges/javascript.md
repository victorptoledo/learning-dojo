---
from: javascript
status: draft
concept: "Context that follows await"
from_way: "Front-end code keeps per-user state in module variables or a store, because one tab serves one user; errors go to the console or an error-tracking SDK."
native_way: "A server interleaves many users on one thread, so per-request state lives in AsyncLocalStorage; logs are JSON lines and traces follow requests across services."
sensei_tip: "A module-level variable is per tab in the browser and per process on a server, which means shared by every request. AsyncLocalStorage is the fix, and the TC39 AsyncContext proposal will bring it to all JavaScript."
---

| Front end | Node.js server |
| --- | --- |
| Module variable for the current user | `AsyncLocalStorage` store |
| `console.log` | pino JSON lines |
| Error-tracking SDK | Logs + metrics + traces |
| Browser performance timeline | OpenTelemetry spans |
