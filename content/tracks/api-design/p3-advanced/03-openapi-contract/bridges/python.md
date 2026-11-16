---
from: python
status: draft
concept: "OpenAPI as a contract"
from_way: "FastAPI's generated schema used only for `/docs`."
native_way: "FastAPI's `app.openapi()` exported and committed for review, or design-first with `datamodel-code-generator` producing Pydantic models from the YAML."
sensei_tip: "Export the schema in CI (`json.dumps(app.openapi())`) and diff it against `main`; a renamed Pydantic field then shows up as a breaking change in the pull request."
---

| Python | OpenAPI concept |
| --- | --- |
| `app.openapi()` | code-first document |
| `datamodel-code-generator` | models from the contract |
| `Field(description=..., examples=[...])` | precise schema |
| `openapi-python-client` | SDK |
| schemathesis | tests responses against the schema |
