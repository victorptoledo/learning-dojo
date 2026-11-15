---
from: go
status: draft
concept: "Documents inside a relational table"
from_way: "encoding/json with structs, map[string]any or json.RawMessage."
native_way: "jsonb columns queried in SQL; Go receives only the fields it needs."
sensei_tip: "Scan jsonb into json.RawMessage to pass it through untouched, or into a struct; select single fields with ->> when you need only one."
---

| Go | SQL |
| --- | --- |
| json.Unmarshal(b, &v) | a jsonb value scanned into a struct |
| json.RawMessage | pass the document through unparsed |
| v["order"].(map[string]any)["id"] | payload -> 'order' ->> 'id' |
| json.Marshal(struct) | jsonb_build_object(...) |
