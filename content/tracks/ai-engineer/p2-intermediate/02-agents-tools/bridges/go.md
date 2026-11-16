---
from: go
status: draft
concept: "The model proposes calls, your code executes them"
from_way: "A map[string]http.HandlerFunc or a switch over command names."
native_way: "A map from tool name to a function taking json.RawMessage arguments, called by a loop that appends results by call id."
sensei_tip: "Decode each call's arguments into a typed struct with DisallowUnknownFields and return a tool error (not a Go error up the stack) when decoding fails, so the model can retry."
---

| Go | Tool calling |
| --- | --- |
| `map[string]func(ctx, json.RawMessage) (string, error)` | the tool registry |
| `json.Unmarshal` into a struct | argument decoding |
| `errgroup.Group` | parallel tool calls |
| returning an error result | an error tool result |
| `for step := range maxSteps` | the agent loop |
