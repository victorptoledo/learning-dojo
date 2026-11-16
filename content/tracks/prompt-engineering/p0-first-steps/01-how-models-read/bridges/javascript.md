---
from: javascript
status: draft
concept: "Streaming, tokens and stateless calls"
from_way: "fetch() a JSON API that answers the same way each time, string.length for size, and session state kept on the server."
native_way: "The model streams the answer token by token, costs are counted in tokens, and every request carries the whole conversation because the API keeps no state."
sensei_tip: "If you have read a streamed fetch body with a reader, you have seen what a model does: each chunk is a few tokens, and the next one is only computed after the previous one exists."
---

| JavaScript | Model concept |
| --- | --- |
| `text.length` | tokens, measured by the provider's tokenizer |
| a `ReadableStream` of chunks | streamed output, one token after another |
| `Math.random()` in a pick | sampling with temperature |
| cookies and server sessions | resending the message list on every call |
| `AbortController` timeout | `max_tokens` and stop sequences end generation |
