---
from: python
status: draft
concept: "The model proposes calls, your code executes them"
from_way: "A dict of command names to functions, called with **kwargs from parsed input."
native_way: "A registry of tools with JSON Schemas; the loop sends them, receives calls, validates arguments, calls the function and appends results until the model gives a final answer."
sensei_tip: "The official SDKs have tool runners (Anthropic's @beta_tool, OpenAI's Agents SDK) that build schemas from type hints and run the loop; write the loop yourself once, as in the next kata, to understand the limits you must set."
---

| Python | Tool calling |
| --- | --- |
| `TOOLS = {"get_order": get_order}` | the tool registry |
| `inspect.signature` / Pydantic | schema from the function |
| `func(**arguments)` | executing a call |
| `except Exception as e: return error(str(e))` | an error tool result |
| `for step in range(MAX_STEPS)` | the agent loop with a limit |
