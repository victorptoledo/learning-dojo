---
from: java
status: draft
concept: "Tokens and next-token prediction"
from_way: "Strings are char sequences, and a method call returns one deterministic value."
native_way: "Text becomes token ids, and generation is a loop that samples one token at a time from a probability distribution."
sensei_tip: "Think of a model call as a pure function of the token list you send, plus some randomness: no session, no memory. Your code owns the conversation state, like a stateless REST service with the client keeping the cart."
---

| Java idea | LLM idea |
| --- | --- |
| `String.length()` | the token count, which depends on the model's tokenizer |
| a stateless REST endpoint | a model call: the whole history goes in every request |
| `Random` with a seed | sampling with temperature and top-p, never fully repeatable |
| `InputStream` read in chunks | a streamed response, token by token |
| request size limit | the context window, input and output together |
