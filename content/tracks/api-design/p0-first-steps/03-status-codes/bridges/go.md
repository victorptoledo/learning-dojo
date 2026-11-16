---
from: go
status: draft
concept: "Choosing status codes"
from_way: "`w.Write(json)` without `WriteHeader`, so everything is 200."
native_way: "`w.WriteHeader(http.StatusCreated)` after setting `Location`; named constants such as `http.StatusConflict`."
sensei_tip: "Headers must be set **before** `WriteHeader` or the first `Write`; after that, changes are silently ignored."
---

| Go | Status code |
| --- | --- |
| `http.StatusCreated` | 201 |
| `http.StatusNoContent` | 204 |
| `http.StatusConflict` | 409 |
| `http.StatusUnprocessableEntity` | 422 |
| `http.StatusTooManyRequests` | 429 |
