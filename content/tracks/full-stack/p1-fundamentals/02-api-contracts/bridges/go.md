---
from: go
status: reviewed
concept: "Struct tags are not a contract"
from_way: "json struct tags define the wire format implicitly, and nothing tells the front end when a tag changes."
native_way: "Write the OpenAPI spec (or generate it), generate Go server interfaces with oapi-codegen and TypeScript types for the UI, and validate input explicitly."
sensei_tip: "Spec-first with oapi-codegen makes the compiler complain on both sides when the contract changes."
---

| Go | Contract |
| --- | --- |
| `json:"priceCents"` | a field in the OpenAPI schema |
| manual `if req.Title == ""` checks | validation from the schema or a validator |
| `http.Error(w, "bad", 400)` | `application/problem+json` |
| `oapi-codegen` | typed server and typed TypeScript client from one spec |
