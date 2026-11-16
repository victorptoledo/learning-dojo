---
from: python
status: draft
concept: "Messages as a list of dicts"
from_way: "Building one big f-string with instructions, history and the question all mixed together."
native_way: "A list of {\"role\": ..., \"content\": ...} dicts: a stable system prompt, alternating user and assistant turns, and per-call data in the last user turn."
sensei_tip: "Build the messages with a small function, build_messages(system, history, user_input), and test it. Most role bugs are list-building bugs: duplicated system prompts, two user turns in a row, data in the wrong turn."
---

| Python | Prompt concept |
| --- | --- |
| a module-level `SYSTEM_PROMPT` constant | stable operator instructions |
| `messages.append({"role": "user", ...})` | adding a user turn |
| `messages[-20:]` | trimming history to save tokens |
| `functools.cache` on a prefix | provider prompt caching of a stable prefix |
| permission checks in the view function | enforcing rules in code |
