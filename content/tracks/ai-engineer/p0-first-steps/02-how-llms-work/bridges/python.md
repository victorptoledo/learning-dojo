---
from: python
status: draft
concept: "Tokens and next-token prediction"
from_way: "len(text) counts characters, and a function returns the same value for the same arguments."
native_way: "A tokenizer turns text into a list of integer ids, and the model samples the next id from a distribution, again and again."
sensei_tip: "Try it: `tiktoken` (OpenAI's open tokenizer) or a provider's count-tokens endpoint shows how your text splits. Seeing a long URL or a JSON blob cost hundreds of tokens changes how you build context."
---

| Python idea | LLM idea |
| --- | --- |
| `len(text)` | the token count, per tokenizer |
| `random.choices(tokens, weights)` | sampling the next token |
| a generator yielding values | a streamed response |
| a list you keep appending to | the conversation history you resend |
| a memory limit | the context window |
