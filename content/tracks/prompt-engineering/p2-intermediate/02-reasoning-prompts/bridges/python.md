---
from: python
status: draft
concept: "Scratch work and a return value"
from_way: "A function that prints its working and returns a value, or a one-liner that is hard to check."
native_way: "The model writes its scratch work in <thinking> and its result in <answer>; a parser extracts the answer, and several samples can vote."
sensei_tip: "collections.Counter(answers).most_common(1) is self-consistency in one line, once your parser normalises the answers (\"42\", \"42.0\" and \"42.\" are one vote)."
---

| Python | Reasoning concept |
| --- | --- |
| intermediate variables | chain-of-thought steps |
| `return result` | the `<answer>` tag |
| `re.search(r"<answer>(.*?)</answer>", text, re.S)` | extracting the answer |
| `Counter(answers).most_common(1)` | self-consistency vote |
| `logging.debug` | reasoning kept out of user output |
