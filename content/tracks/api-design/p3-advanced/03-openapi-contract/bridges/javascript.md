---
from: javascript
status: draft
concept: "OpenAPI as a contract"
from_way: "Hand-written types that drift from the docs."
native_way: "`openapi-typescript` generating types from the YAML, `openapi-fetch` for typed calls, and validation middleware checking requests against the document."
sensei_tip: "`openapi-typescript` turns the contract into TypeScript types at build time: a field removed from the YAML becomes a compile error in every client."
---

| JavaScript / TypeScript | OpenAPI concept |
| --- | --- |
| `openapi-typescript` | types from the contract |
| `openapi-fetch` | typed client |
| `express-openapi-validator` | runtime validation |
| Redocly CLI `lint` | style checks |
| Prism | mock server |
