---
from: javascript
status: reviewed
concept: "The browser's cost model"
from_way: "Adding a library for every need until the bundle is several megabytes, and a WebSocket library for a one-way status feed."
native_way: "Budget JavaScript, split by route, size images, and use EventSource for server-to-client streams before reaching for WebSockets."
sensei_tip: "new EventSource(\"/api/orders/42/events\") reconnects by itself and sends Last-Event-ID; that is a lot of code you do not have to write."
---

| JavaScript | Performance and real-time |
| --- | --- |
| `import Heavy from "./heavy"` | `const Heavy = lazy(() => import("./heavy"))` |
| `<img src>` full size | `srcset`, `sizes`, `loading="lazy"` |
| socket.io for notifications | `EventSource` |
| sequential `await`s | `Promise.all` |
