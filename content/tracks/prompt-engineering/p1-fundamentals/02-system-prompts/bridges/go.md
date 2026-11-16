---
from: go
status: draft
concept: "Static config and request context"
from_way: "A config struct loaded at start, and a context.Context with request-scoped values passed to every handler."
native_way: "The system prompt is the static configuration; the user turn holds the request-scoped data; permission checks stay in the handler that acts."
sensei_tip: "Build the messages slice in one function and test it like any handler: given this history and input, the slice has one system message first and alternating roles after it."
---

| Go | Prompt concept |
| --- | --- |
| a config struct | the system prompt |
| request body and `context.Context` values | the user turn |
| middleware that checks auth | enforcement in code |
| secrets from the environment, never logged | never put secrets in the prompt |
| `[]Message` | the conversation history |
