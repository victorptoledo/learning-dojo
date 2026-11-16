---
from: go
status: draft
concept: "The API as a contract"
from_way: "Encoding the database struct (with its `db` tags) directly with `json.NewEncoder(w).Encode(row)`."
native_way: "Separate response structs with explicit `json` tags, mapped from storage types; the OpenAPI document is the contract (often generated into types with oapi-codegen)."
sensei_tip: "In Go every exported field is serialised; one struct for storage and wire means renaming a column renames an API field."
---

| Go | API concept |
| --- | --- |
| storage struct with `db:` tags | implementation |
| response struct with `json:` tags | representation |
| oapi-codegen from `openapi.yaml` | design-first contract |
| exported field added for internal use | an accidental API change |
