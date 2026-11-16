---
from: go
status: draft
concept: "Handlers behind a mux"
from_way: "http.HandlerFunc handlers registered on a ServeMux, decoding a JSON body into a struct and returning errors."
native_way: "Tools are handlers registered by name; the model's call is decoded, validated against the schema, authorised and executed, and errors go back as tool results."
sensei_tip: "A map[string]Tool with a Schema and a func(args map[string]any) (string, error) is a complete dispatcher; return the error text as the tool result, never panic on bad arguments."
---

| Go | Tool-use concept |
| --- | --- |
| `mux.HandleFunc("/orders", h)` | registering a tool by name |
| decoding into a struct | parsing the arguments |
| validation returning `error` | the problems in an error result |
| middleware checking the user | permission checks in code |
| `context.WithTimeout` | limits on the tool loop |
