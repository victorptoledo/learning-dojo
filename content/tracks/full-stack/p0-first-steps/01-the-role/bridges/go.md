---
from: go
status: reviewed
concept: "From a net/http handler to the user's screen"
from_way: "A Go developer writes a small fast handler and considers the job done when it returns 200."
native_way: "The handler is one hop of the journey; the page, the proxy, the database and the way back are part of the same feature."
sensei_tip: "Log one structured line per request with slog and the trace id, and you can join the user's report with your server in seconds."
---

| Go habit | End to end |
| --- | --- |
| `http.HandleFunc` | the browser, DNS, TLS and a proxy come first |
| `log.Printf` | `slog` JSON with a trace id |
| a single static binary | an image, a front-end bundle and migrations |
| `database/sql` | constraints and indexes are part of the feature |
