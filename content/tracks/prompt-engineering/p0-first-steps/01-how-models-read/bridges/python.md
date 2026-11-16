---
from: python
status: draft
concept: "Tokens, sampling and the context budget"
from_way: "len(text) to measure strings, random.choice for randomness, and functions that return the same value for the same arguments."
native_way: "Text is measured in tokens; each output token is drawn like random.choices(tokens, weights=probabilities), and the whole prompt plus the answer must fit in the context window."
sensei_tip: "random.choices with weights is a good picture of sampling: temperature reshapes the weights before the draw. Seeding the generator helps, but with a remote model you rarely control every source of variation."
---

| Python | Model concept |
| --- | --- |
| `len(text)` | token count (about `len(text) / 4` in English) |
| `random.choices(options, weights=p)` | sampling the next token |
| sharpening `p` before the draw | low temperature |
| `max(options, key=p.get)` | greedy decoding (temperature near 0) |
| a function with no global state | a stateless chat API: send all context every call |
