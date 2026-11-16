---
from: python
status: draft
concept: "Choosing status codes"
from_way: "`return {\"error\": \"...\"}` with the default 200, or letting exceptions become 500."
native_way: "`raise HTTPException(status_code=409, ...)` or custom exception handlers; `status_code=201` on the route decorator."
sensei_tip: "`http.HTTPStatus.CONFLICT` and friends exist in the standard library; use the names instead of bare numbers."
---

| Python | Status code |
| --- | --- |
| `@app.post(..., status_code=201)` | 201 |
| `Response(status_code=204)` | 204 |
| `raise HTTPException(404)` | 404 |
| `HTTPStatus.UNPROCESSABLE_CONTENT` | 422 |
| `@app.exception_handler(MyError)` | one place for status decisions |
