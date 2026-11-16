---
from: go
status: draft
concept: "Work shown, result returned"
from_way: "Functions that return a value, with debug logging of the intermediate state."
native_way: "The model writes its steps in <thinking> and the result in <answer>; code extracts the answer, falls back explicitly and can take a majority over samples."
sensei_tip: "A map[string]int counting normalised answers plus a slice remembering the first-seen order gives you a deterministic self-consistency vote, ties included."
---

| Go | Reasoning concept |
| --- | --- |
| intermediate variables | chain-of-thought steps |
| the return value | the `<answer>` tag |
| `regexp.MustCompile("(?s)<answer>(.*?)</answer>")` | extracting the answer |
| `map[string]int` of answers | self-consistency vote |
| `slog.Debug` | reasoning kept out of user output |
