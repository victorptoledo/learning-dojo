---
from: go
status: draft
concept: "Budgets and non-determinism"
from_way: "Functions that return (value, error), len() for byte length, and explicit limits on buffers and request sizes."
native_way: "A model returns text and never an error for a wrong answer; length, cost and limits are counted in tokens, and the context window is a fixed-size buffer shared by input and output."
sensei_tip: "Picture the context window as a fixed-size buffer: what you write as input reduces what the model can write as output. Reserve room for the answer the way you size a buffer before a read."
---

| Go | Model concept |
| --- | --- |
| `len(s)` counts bytes | tokens are counted by the tokenizer |
| a fixed-size `[]byte` buffer | the context window |
| `io.LimitReader` | `max_tokens` |
| `(value, error)` | a plausible answer with no error to check |
| `math/rand` | sampling and temperature |
