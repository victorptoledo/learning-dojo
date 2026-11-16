---
from: go
status: draft
concept: "Decoding with DisallowUnknownFields"
from_way: "json.Unmarshal into a struct, which silently ignores unknown fields and leaves missing ones as zero values."
native_way: "Decode strictly, check required fields and enums explicitly (a zero value is not 'missing'), and send every problem back in a bounded retry."
sensei_tip: "Use a json.Decoder with DisallowUnknownFields, and pointer fields (*string) for anything that may be null, so you can tell 'missing' from 'empty'."
---

| Go | Structured-output concept |
| --- | --- |
| a struct with `json` tags | the output schema |
| `dec.DisallowUnknownFields()` | no unknown fields |
| `*string` fields | an explicit null for missing values |
| a `switch` over allowed values | enum validation |
| `errors.Join(errs...)` | all the problems for the retry message |
