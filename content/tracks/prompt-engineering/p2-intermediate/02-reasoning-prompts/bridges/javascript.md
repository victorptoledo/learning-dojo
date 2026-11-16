---
from: javascript
status: draft
concept: "Debug output versus the response body"
from_way: "console.log of intermediate values while computing a response, and returning only the result to the client."
native_way: "The model writes intermediate steps in a thinking section and the result in an answer section; code sends only the answer to the user."
sensei_tip: "Treat <thinking> like server logs: useful to you, never sent to the browser as is. Extract <answer> with a regex that takes the last match, because models sometimes mention the tag in their reasoning."
---

| JavaScript | Reasoning concept |
| --- | --- |
| `console.log` of intermediate values | chain-of-thought steps |
| `res.json(result)` | the `<answer>` tag |
| `text.matchAll(/<answer>([\s\S]*?)<\/answer>/g)` | extracting the answer |
| counting values in a `Map` | self-consistency vote |
| a request timeout | thinking budgets and latency |
