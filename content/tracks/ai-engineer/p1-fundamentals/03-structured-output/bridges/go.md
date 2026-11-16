---
from: go
status: draft
concept: "Structured output is deserialisation plus validation"
from_way: "json.Unmarshal into a struct with tags, then hand-written checks."
native_way: "A JSON Schema (generated from the struct or written by hand) guides the model; you decode with DisallowUnknownFields, validate and repair."
sensei_tip: "json.Decoder with DisallowUnknownFields is your additionalProperties: false, and pointers (*string) are how you model nullable fields."
---

| Go | Structured output |
| --- | --- |
| a struct with `json` field tags | the schema |
| typed string constants | an enum, checked by hand |
| `*string` | a nullable field |
| `dec.DisallowUnknownFields()` | `additionalProperties: false` |
| a slice of error strings | errors to send back |
