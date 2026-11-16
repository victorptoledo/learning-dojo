---
from: go
status: draft
concept: "Resources and REST"
from_way: "A switch on `r.URL.Path` with paths like `/getOrder`."
native_way: "Method-and-path patterns on `http.ServeMux`: `\"GET /orders/{id}\"`, `\"POST /orders/{id}/cancellation\"`."
sensei_tip: "Google's AIP style is common in Go services that also speak gRPC: custom methods look like `POST /orders/{id}:cancel`. Pick one style per API."
---

| Go | REST concept |
| --- | --- |
| `"GET /orders"` | collection |
| `"GET /orders/{id}"` | item |
| `"POST /orders/{id}/cancellation"` | action as a resource |
| `"PATCH /orders/{id}"` | partial update |
| `w.Header().Set("Location", ...)` | link to the created resource |
