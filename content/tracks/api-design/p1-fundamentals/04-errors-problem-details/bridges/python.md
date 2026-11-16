---
from: python
status: draft
concept: "Problem details (RFC 9457)"
from_way: "`HTTPException(detail=\"...\")` producing `{\"detail\": \"...\"}` in FastAPI, or DRF's `{\"field\": [\"error\"]}`."
native_way: "An exception handler that returns `JSONResponse(content=problem, media_type=\"application/problem+json\")`, with a Pydantic model for the problem shape."
sensei_tip: "FastAPI's 422 for validation uses its own shape; override `RequestValidationError` handling to emit problem details with JSON pointers."
---

| Python | Problem details |
| --- | --- |
| `JSONResponse(..., media_type="application/problem+json")` | a problem |
| Pydantic `Problem` model with `extra="allow"` | extension members |
| `@app.exception_handler(RequestValidationError)` | validation errors |
| `exc.errors()[i]["loc"]` | becomes the JSON pointer |
| logging plus a correlation id | safe 500s |
