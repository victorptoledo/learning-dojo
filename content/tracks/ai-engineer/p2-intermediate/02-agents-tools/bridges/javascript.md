---
from: javascript
status: draft
concept: "The model proposes calls, your code executes them"
from_way: "An object of handlers keyed by action type, like a Redux reducer or an Express router."
native_way: "Tools described by zod or JSON schemas; the model emits calls; a handler map runs them and the loop feeds results back."
sensei_tip: "Promise.allSettled is the natural way to run parallel tool calls and turn rejections into error results without losing the others."
---

| JavaScript | Tool calling |
| --- | --- |
| `const handlers = { getOrder }` | the tool registry |
| a zod schema | the tool parameters |
| `Promise.allSettled(calls.map(run))` | parallel calls with errors kept |
| `try/catch` returning `{ error }` | an error tool result |
| a `while` loop with a counter | the agent loop |
