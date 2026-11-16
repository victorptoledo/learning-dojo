---
from: javascript
status: draft
concept: "Tokens and next-token prediction"
from_way: "str.length counts UTF-16 code units, and fetch returns a whole JSON body."
native_way: "Text is split into model-specific tokens, and the answer arrives token by token, which is why chat UIs stream it."
sensei_tip: "Emoji and non-Latin scripts cost more tokens per character than English, just as they surprise you with .length. Count with the provider's tools before you set limits."
---

| JavaScript idea | LLM idea |
| --- | --- |
| `str.length` | the token count, per tokenizer |
| `Math.random()` | sampling, shaped by temperature and top-p |
| a `ReadableStream` | a streamed response |
| state in a React store | the conversation history your app resends |
| a payload size limit | the context window |
