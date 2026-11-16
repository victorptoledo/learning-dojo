---
from: javascript
status: reviewed
concept: "From the browser out to the database"
from_way: "A front-end developer starts at the component and treats the API as a black box that returns JSON."
native_way: "Open the box: the same request goes through a proxy, a server, a validation layer and SQL, and each can fail in its own way."
sensei_tip: "When an API call fails, read the status and the response body in the network tab before touching the component: the server usually told you what is wrong."
---

| Front-end habit | End to end |
| --- | --- |
| `fetch` returns data | it returns a status, headers and a body you must check |
| "the API is down" | DNS, TLS, CORS, auth, validation or the database? |
| state in the component | the source of truth is a table with constraints |
| `npm run build` | the bundle is one of several artifacts in a release |
